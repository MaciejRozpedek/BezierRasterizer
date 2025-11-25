package com.macroz.bezierrasterizer.logic;

import com.macroz.bezierrasterizer.model.Triangle;
import com.macroz.bezierrasterizer.model.Vertex;
import org.joml.Vector3f;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Rasterizer {

	private int width;
	private int height;
	private int[] colorBuffer;
	private float[] zBuffer;
	private BufferedImage image;

	private float kd = 0.5f;
	private float ks = 0.5f;
	private float m = 50.0f;
	private int objectColorRGB = 0xFF8080CC;
	private int lightColorRGB = 0xFFFFFFFF;

	private int[] texturePixels;
	private int textureW, textureH;
	private boolean useTexture = false;

	private int[] normalMapPixels;
	private int normalMapW, normalMapH;
	private boolean useNormalMap = false;

	private final List<Vector3f> lights = new ArrayList<>();

	private final Vector3f L = new Vector3f();
	private final Vector3f V = new Vector3f(0, 0, 1);
	private final Vector3f N = new Vector3f();
	private final Vector3f R = new Vector3f();
	private final Vector3f pixelPos = new Vector3f();

	private final Vector3f interpTu = new Vector3f();
	private final Vector3f interpTv = new Vector3f();
	private final Vector3f mapN = new Vector3f();

	private float currentLightZ = 200.0f;

	public Rasterizer(int width, int height) {
		resize(width, height);
		lights.add(new Vector3f(0, 0, currentLightZ));
	}

	public void setLightingParams(float kd, float ks, float m, float lightZ) {
		this.kd = kd;
		this.ks = ks;
		this.m = m;
		this.currentLightZ = lightZ;

		for (Vector3f l : lights) {
			l.z = lightZ;
		}
	}

	public void setLightPosition(float x, float y) {
		if (lights.size() != 1) {
			lights.clear();
			lights.add(new Vector3f(x, y, currentLightZ));
		} else {
			lights.getFirst().set(x, y, currentLightZ);
		}
	}

	public void setMultiLights(List<Vector3f> newLights) {
		lights.clear();
		for (Vector3f l : newLights) {
			lights.add(new Vector3f(l.x, l.y, currentLightZ));
		}
	}

	public void setObjectColor(Color color) {
		this.objectColorRGB = color.getRGB();
	}

	public void setLightColor(Color color) {
		this.lightColorRGB = color.getRGB();
	}

	public void setTexture(BufferedImage img) {
		if (img != null) {
			this.textureW = img.getWidth();
			this.textureH = img.getHeight();
			this.texturePixels = img.getRGB(0, 0, textureW, textureH, null, 0, textureW);
		} else {
			this.texturePixels = null;
		}
	}

	public void setNormalMap(BufferedImage img) {
		if (img != null) {
			this.normalMapW = img.getWidth();
			this.normalMapH = img.getHeight();
			this.normalMapPixels = img.getRGB(0, 0, normalMapW, normalMapH, null, 0, normalMapW);
		} else {
			this.normalMapPixels = null;
		}
	}

	public void setUseTexture(boolean use) {
		this.useTexture = use;
	}

	public void setUseNormalMap(boolean use) {
		this.useNormalMap = use;
	}

	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
		this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		this.colorBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		this.zBuffer = new float[width * height];
	}

	public void clear() {
		Arrays.fill(colorBuffer, 0xFFC0C0C0);
		Arrays.fill(zBuffer, Float.NEGATIVE_INFINITY);
	}

	public BufferedImage getImage() {
		return image;
	}

	public void render(List<Triangle> triangles) {
		for (Triangle t : triangles) {
			fillTriangle(t);
		}
	}

	private void fillTriangle(Triangle t) {
		float[] x = new float[3];
		float[] y = new float[3];
		int[] idx = {0, 1, 2};

		for (int i = 0; i < 3; i++) {
			Vertex v = t.vertices[i];
			x[i] = v.transformedPosition.x + width / 2.0f;
			y[i] = height / 2.0f - v.transformedPosition.y;
		}

		// Sort vertices by Y (V1 < V2 < V3)
		if (y[idx[0]] > y[idx[1]]) swap(idx, 0, 1);
		if (y[idx[0]] > y[idx[2]]) swap(idx, 0, 2);
		if (y[idx[1]] > y[idx[2]]) swap(idx, 1, 2);

		int i1 = idx[0], i2 = idx[1], i3 = idx[2];

		float invSlopeLong = (x[i3] - x[i1]) / (y[i3] - y[i1]);
		float invSlope1    = (x[i2] - x[i1]) / (y[i2] - y[i1]);
		float invSlope2    = (x[i3] - x[i2]) / (y[i3] - y[i2]);

		int yStart = (int) Math.ceil(y[i1]);
		int yMid   = (int) Math.ceil(y[i2]);
		int yEnd   = (int) Math.ceil(y[i3]);

		if (yMid > yStart) {
			float dy = yStart - y[i1];
			float curXA = x[i1] + invSlopeLong * dy;
			float curXB = x[i1] + invSlope1 * dy;
			processScanlines(yStart, yMid, curXA, curXB, invSlopeLong, invSlope1, t, x, y);
		}

		if (yEnd > yMid) {
			float dyLong = yMid - y[i1];
			float dyShort = yMid - y[i2];
			float curXA = x[i1] + invSlopeLong * dyLong;
			float curXB = x[i2] + invSlope2 * dyShort;
			processScanlines(yMid, yEnd, curXA, curXB, invSlopeLong, invSlope2, t, x, y);
		}
	}

	private void processScanlines(int yStart, int yEnd, float xA, float xB, float dxA, float dxB,
								  Triangle t, float[] xArr, float[] yArr) {
		for (int y = yStart; y < yEnd; y++) {
			if (y >= 0 && y < height) {
				drawScanline(y, xA, xB, t, xArr, yArr);
			}
			xA += dxA;
			xB += dxB;
		}
	}

	private void drawScanline(int y, float xStart, float xEnd, Triangle t, float[] xArr, float[] yArr) {
		if (xStart > xEnd) {
			float tmp = xStart; xStart = xEnd; xEnd = tmp;
		}

		int ixStart = Math.max(0, (int) Math.ceil(xStart));
		int ixEnd = Math.min(width, (int) Math.ceil(xEnd));

		float denom = (yArr[1] - yArr[2]) * (xArr[0] - xArr[2]) + (xArr[2] - xArr[1]) * (yArr[0] - yArr[2]);
		if (Math.abs(denom) < 1e-5) return;
		float invDenom = 1.0f / denom;

		int rowOffset = y * width;
		Vertex v1 = t.vertices[0];
		Vertex v2 = t.vertices[1];
		Vertex v3 = t.vertices[2];

		for (int x = ixStart; x < ixEnd; x++) {
			float w1 = ((yArr[1] - yArr[2]) * (x - xArr[2]) + (xArr[2] - xArr[1]) * (y - yArr[2])) * invDenom;
			float w2 = ((yArr[2] - yArr[0]) * (x - xArr[2]) + (xArr[0] - xArr[2]) * (y - yArr[2])) * invDenom;
			float w3 = 1.0f - w1 - w2;

			float zInterp = w1 * v1.transformedPosition.z + w2 * v2.transformedPosition.z + w3 * v3.transformedPosition.z;

			int idx = rowOffset + x;
			if (zInterp > zBuffer[idx]) {
				zBuffer[idx] = zInterp;

				pixelPos.set(v1.transformedPosition).mul(w1)
					.add(v2.transformedPosition.x * w2, v2.transformedPosition.y * w2, v2.transformedPosition.z * w2)
					.add(v3.transformedPosition.x * w3, v3.transformedPosition.y * w3, v3.transformedPosition.z * w3);

				float u = w1 * v1.u + w2 * v2.u + w3 * v3.u;
				float v = w1 * v1.v + w2 * v2.v + w3 * v3.v;

				int currentColor = objectColorRGB;
				if (useTexture && texturePixels != null) {
					currentColor = sampleTexture(u, v, texturePixels, textureW, textureH);
				}

				N.set(v1.transformedNormal).mul(w1)
					.add(v2.transformedNormal.x * w2, v2.transformedNormal.y * w2, v2.transformedNormal.z * w2)
					.add(v3.transformedNormal.x * w3, v3.transformedNormal.y * w3, v3.transformedNormal.z * w3);

				if (N.lengthSquared() > 0) N.normalize();

				if (useNormalMap && normalMapPixels != null) {
					interpTu.set(v1.transformedTangentU).mul(w1)
						.add(v2.transformedTangentU.x * w2, v2.transformedTangentU.y * w2, v2.transformedTangentU.z * w2)
						.add(v3.transformedTangentU.x * w3, v3.transformedTangentU.y * w3, v3.transformedTangentU.z * w3);

					interpTv.set(v1.transformedTangentV).mul(w1)
						.add(v2.transformedTangentV.x * w2, v2.transformedTangentV.y * w2, v2.transformedTangentV.z * w2)
						.add(v3.transformedTangentV.x * w3, v3.transformedTangentV.y * w3, v3.transformedTangentV.z * w3);

					if(interpTu.lengthSquared() > 1e-8) interpTu.normalize();
					if(interpTv.lengthSquared() > 1e-8) interpTv.normalize();

					int mapColor = sampleTexture(u, v, normalMapPixels, normalMapW, normalMapH);

					float nx = ((mapColor >> 16) & 0xFF) / 127.5f - 1.0f;
					float ny = ((mapColor >> 8) & 0xFF) / 127.5f - 1.0f;
					float nz = (mapColor & 0xFF) / 127.5f - 1.0f;

					mapN.set(nx, ny, nz);
					if (mapN.lengthSquared() > 0) mapN.normalize();

					float finalX = interpTu.x * mapN.x + interpTv.x * mapN.y + N.x * mapN.z;
					float finalY = interpTu.y * mapN.x + interpTv.y * mapN.y + N.y * mapN.z;
					float finalZ = interpTu.z * mapN.x + interpTv.z * mapN.y + N.z * mapN.z;
					N.set(finalX, finalY, finalZ);
					if (N.lengthSquared() > 0) N.normalize();
				}

				if (N.z < 0) N.negate();

				colorBuffer[idx] = calculatePhongColor(N, pixelPos, currentColor);
			}
		}
	}

	private int sampleTexture(float u, float v, int[] pixels, int w, int h) {
		u = Math.max(0, Math.min(1, u));
		v = Math.max(0, Math.min(1, v));

		int x = (int) (u * (w - 1));
		int y = (int) (v * (h - 1));

		y = h - 1 - y;
		return pixels[y * w + x];
	}

	private int calculatePhongColor(Vector3f N, Vector3f pixelPos, int objColor) {
		float objectR = ((objColor >> 16) & 0xFF) / 255.0f;
		float objectG = ((objColor >> 8) & 0xFF) / 255.0f;
		float objectB = (objColor & 0xFF) / 255.0f;

		float lightR = ((lightColorRGB >> 16) & 0xFF) / 255.0f;
		float lightG = ((lightColorRGB >> 8) & 0xFF) / 255.0f;
		float lightB = (lightColorRGB & 0xFF) / 255.0f;

		float totalR = 0.0f;
		float totalG = 0.0f;
		float totalB = 0.0f;

		for (Vector3f lightPos : lights) {
			L.set(lightPos).sub(pixelPos);
			if (L.lengthSquared() > 0) L.normalize();

			float cosNL = Math.max(0.0f, N.dot(L));

			R.set(N).mul(2.0f * cosNL).sub(L);
			if (R.lengthSquared() > 0) R.normalize();

			float cosVR = Math.max(0.0f, V.dot(R));
			float specFactor = (float) Math.pow(cosVR, m);

			float scale = 1.0f;
			if (lights.size() > 1) {
				scale = 0.3f;
			}
			totalR += (kd * lightR * objectR * cosNL + ks * lightR * objectR * specFactor) * scale;
			totalG += (kd * lightG * objectG * cosNL + ks * lightG * objectG * specFactor) * scale;
			totalB += (kd * lightB * objectB * cosNL + ks * lightB * objectB * specFactor) * scale;
		}

		int ir = Math.min(255, (int) (totalR * 255));
		int ig = Math.min(255, (int) (totalG * 255));
		int ib = Math.min(255, (int) (totalB * 255));

		return (255 << 24) | (ir << 16) | (ig << 8) | ib;
	}

	private void swap(int[] arr, int i, int j) {
		int temp = arr[i];
		arr[i] = arr[j];
		arr[j] = temp;
	}
}