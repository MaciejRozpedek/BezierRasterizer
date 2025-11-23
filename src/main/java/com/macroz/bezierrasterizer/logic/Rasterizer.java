package com.macroz.bezierrasterizer.logic;

import com.macroz.bezierrasterizer.model.Triangle;
import com.macroz.bezierrasterizer.model.Vertex;
import org.joml.Vector3f;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
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

	// For optimization purposes
	private final Vector3f lightPos = new Vector3f(0, 0, 200);
	private final Vector3f L = new Vector3f();
	private final Vector3f V = new Vector3f(0, 0, 1);
	private final Vector3f N = new Vector3f();
	private final Vector3f R = new Vector3f();
	private final Vector3f pixelPos = new Vector3f();

	public Rasterizer(int width, int height) {
		resize(width, height);
	}

	public void setLightingParams(float kd, float ks, float m, float lightZ) {
		this.kd = kd;
		this.ks = ks;
		this.m = m;
		this.lightPos.z = lightZ;
	}

	public void setLightPosition(float x, float y) {
		this.lightPos.x = x;
		this.lightPos.y = y;
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

		if (y[idx[0]] > y[idx[1]]) swap(idx, 0, 1);
		if (y[idx[0]] > y[idx[2]]) swap(idx, 0, 2);
		if (y[idx[1]] > y[idx[2]]) swap(idx, 1, 2);

		int i1 = idx[0], i2 = idx[1], i3 = idx[2];

		// Slopes
		float invSlopeLong = (x[i3] - x[i1]) / (y[i3] - y[i1]);
		float invSlope1    = (x[i2] - x[i1]) / (y[i2] - y[i1]);
		float invSlope2    = (x[i3] - x[i2]) / (y[i3] - y[i2]);

		int yStart = (int) Math.ceil(y[i1]);
		int yMid   = (int) Math.ceil(y[i2]);
		int yEnd   = (int) Math.ceil(y[i3]);

		// Upper Triangle
		if (yMid > yStart) {
			float dy = yStart - y[i1];
			float curXA = x[i1] + invSlopeLong * dy;
			float curXB = x[i1] + invSlope1 * dy;
			processScanlines(yStart, yMid, curXA, curXB, invSlopeLong, invSlope1, t, x, y);
		}

		// Lower Triangle
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

		int rowOffset = y * width;
		Vertex v1 = t.vertices[0];
		Vertex v2 = t.vertices[1];
		Vertex v3 = t.vertices[2];

		for (int x = ixStart; x < ixEnd; x++) {
			float w1 = ((yArr[1] - yArr[2]) * (x - xArr[2]) + (xArr[2] - xArr[1]) * (y - yArr[2])) / denom;
			float w2 = ((yArr[2] - yArr[0]) * (x - xArr[2]) + (xArr[0] - xArr[2]) * (y - yArr[2])) / denom;
			float w3 = 1.0f - w1 - w2;

			float zInterp = w1 * v1.transformedPosition.z + w2 * v2.transformedPosition.z + w3 * v3.transformedPosition.z;

			int idx = rowOffset + x;
			if (zInterp > zBuffer[idx]) {
				zBuffer[idx] = zInterp;

				pixelPos.set(v1.transformedPosition).mul(w1)
					.add(v2.transformedPosition.x * w2, v2.transformedPosition.y * w2, v2.transformedPosition.z * w2)
					.add(v3.transformedPosition.x * w3, v3.transformedPosition.y * w3, v3.transformedPosition.z * w3);

				N.set(v1.transformedNormal).mul(w1)
					.add(v2.transformedNormal.x * w2, v2.transformedNormal.y * w2, v2.transformedNormal.z * w2)
					.add(v3.transformedNormal.x * w3, v3.transformedNormal.y * w3, v3.transformedNormal.z * w3);

				if (N.lengthSquared() > 0) N.normalize();

				if (N.z < 0) N.negate();

				colorBuffer[idx] = calculatePhongColor(N, pixelPos);
			}
		}
	}

	private int calculatePhongColor(Vector3f N, Vector3f pixelPos) {
		// L = LightPos - pixelPos
		L.set(lightPos).sub(pixelPos);
		if (L.lengthSquared() > 0) L.normalize();

		float cosNL = Math.max(0.0f, N.dot(L));

		R.set(N).mul(2.0f * cosNL).sub(L);
		if (R.lengthSquared() > 0) R.normalize();

		float cosVR = Math.max(0.0f, V.dot(R));
		float specFactor = (float) Math.pow(cosVR, m);

		// Temporary colors. TODO: Change later
		float objectR = 0.5f, objectG = 0.5f, objectB = 0.8f;
		float lightR = 1.0f, lightG = 1.0f, lightB = 1.0f;

		float r = kd * lightR * objectR * cosNL + ks * lightR * objectR * specFactor;
		float g = kd * lightG * objectG * cosNL + ks * lightG * objectG * specFactor;
		float b = kd * lightB * objectB * cosNL + ks * lightB * objectB * specFactor;

		int ir = Math.min(255, (int)(r * 255));
		int ig = Math.min(255, (int)(g * 255));
		int ib = Math.min(255, (int)(b * 255));

		return (255 << 24) | (ir << 16) | (ig << 8) | ib;
	}

	private void swap(int[] arr, int i, int j) {
		int temp = arr[i];
		arr[i] = arr[j];
		arr[j] = temp;
	}
}