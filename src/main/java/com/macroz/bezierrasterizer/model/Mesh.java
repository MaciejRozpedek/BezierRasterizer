package com.macroz.bezierrasterizer.model;

import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

public class Mesh {
	private final Vector3f[] controlPoints;

	private final List<Triangle> triangles;

	public Mesh() {
		this.controlPoints = new Vector3f[16];
		for (int i = 0; i < 16; i++) {
			this.controlPoints[i] = new Vector3f();
		}
		this.triangles = new ArrayList<>();
	}

	public Vector3f[] getControlPoints() {
		return controlPoints;
	}

	public void setControlPoint(int i, int j, float x, float y, float z) {
		if (i < 0 || i > 3 || j < 0 || j > 3) {
			throw new IllegalArgumentException("Control point indices must be in the range 0-3");
		}
		controlPoints[i * 4 + j].set(x, y, z);
	}

	public Vector3f getControlPoint(int i, int j) {
		return controlPoints[i * 4 + j];
	}

	public List<Triangle> getTriangles() {
		return triangles;
	}

	public void clearMesh() {
		triangles.clear();
	}

	public void addTriangle(Triangle t) {
		triangles.add(t);
	}
}