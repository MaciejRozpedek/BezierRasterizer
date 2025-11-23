package com.macroz.bezierrasterizer.logic;

import com.macroz.bezierrasterizer.model.Triangle;
import com.macroz.bezierrasterizer.model.Vertex;

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

	public Rasterizer(int width, int height) {
		resize(width, height);
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
		float[] z = new float[3];
		int[] idx = {0, 1, 2};

		for (int i = 0; i < 3; i++) {
			Vertex v = t.vertices[i];
			x[i] = v.transformedPosition.x + width / 2.0f;
			y[i] = height / 2.0f - v.transformedPosition.y;
			z[i] = v.transformedPosition.z;
		}

		if (y[idx[0]] > y[idx[1]]) swap(idx, 0, 1);
		if (y[idx[0]] > y[idx[2]]) swap(idx, 0, 2);
		if (y[idx[1]] > y[idx[2]]) swap(idx, 1, 2);

		int i1 = idx[0], i2 = idx[1], i3 = idx[2];

		// Temporary color based on depth (z) TODO: Delete this and implement proper lighting
		int val = (int) ((z[i2] + 100) * 1.5f);
		val = Math.max(0, Math.min(255, val));
		int color = (255 << 24) | (val << 8) | (Math.min(val + 50, 255));

		// Slopes
		float invSlopeLong = (x[i3] - x[i1]) / (y[i3] - y[i1]);
		float zSlopeLong   = (z[i3] - z[i1]) / (y[i3] - y[i1]);
		float invSlope1    = (x[i2] - x[i1]) / (y[i2] - y[i1]);
		float zSlope1      = (z[i2] - z[i1]) / (y[i2] - y[i1]);
		float invSlope2    = (x[i3] - x[i2]) / (y[i3] - y[i2]);
		float zSlope2      = (z[i3] - z[i2]) / (y[i3] - y[i2]);

		// Integer Y bounds
		int yStart = (int) Math.ceil(y[i1]);
		int yMid   = (int) Math.ceil(y[i2]);
		int yEnd   = (int) Math.ceil(y[i3]);

		// Upper Triangle
		if (yMid > yStart) {
			float dy = yStart - y[i1];
			float curXA = x[i1] + invSlopeLong * dy;
			float curZA = z[i1] + zSlopeLong * dy;
			float curXB = x[i1] + invSlope1 * dy;
			float curZB = z[i1] + zSlope1 * dy;
			processScanlines(yStart, yMid, curXA, curZA, curXB, curZB, invSlopeLong, zSlopeLong, invSlope1, zSlope1, color);
		}

		// Lower Triangle
		if (yEnd > yMid) {
			float dyLong = yMid - y[i1];
			float dyShort = yMid - y[i2];
			float curXA = x[i1] + invSlopeLong * dyLong;
			float curZA = z[i1] + zSlopeLong * dyLong;
			float curXB = x[i2] + invSlope2 * dyShort;
			float curZB = z[i2] + zSlope2 * dyShort;
			processScanlines(yMid, yEnd, curXA, curZA, curXB, curZB, invSlopeLong, zSlopeLong, invSlope2, zSlope2, color);
		}
	}

	private void processScanlines(int yStart, int yEnd, float xA, float zA, float xB, float zB, float dxA, float dzA, float dxB, float dzB, int color) {
		for (int y = yStart; y < yEnd; y++) {
			if (y >= 0 && y < height) {
				drawScanline(y, xA, xB, zA, zB, color);
			}
			xA += dxA;zA += dzA;
			xB += dxB; zB += dzB;
		}
	}

	private void drawScanline(int y, float x1, float x2, float z1, float z2, int color) {
		if (x1 > x2) {
			float tmp = x1; x1 = x2; x2 = tmp;
			tmp = z1; z1 = z2; z2 = tmp;
		}

		int startX = Math.max(0, (int) Math.ceil(x1));
		int endX = Math.min(width, (int) Math.ceil(x2));

		float dist = x2 - x1;
		float zStep = (dist == 0) ? 0 : (z2 - z1) / dist;

		float currentZ = z1 + (startX - x1) * zStep;

		int rowOffset = y * width;
		for (int x = startX; x < endX; x++) {
			int idx = rowOffset + x;
			if (currentZ > zBuffer[idx]) {
				zBuffer[idx] = currentZ;
				colorBuffer[idx] = color;
			}
			currentZ += zStep;
		}
	}

	private void swap(int[] arr, int i, int j) {
		int temp = arr[i];
		arr[i] = arr[j];
		arr[j] = temp;
	}
}