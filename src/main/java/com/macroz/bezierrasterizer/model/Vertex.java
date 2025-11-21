package com.macroz.bezierrasterizer.model;

import org.joml.Vector3f;

public class Vertex {
	// Parametry powierzchni (potrzebne do teksturowania i interpolacji)
	public final float u;
	public final float v;

	public final Vector3f position = new Vector3f();
	public final Vector3f tangentU = new Vector3f();
	public final Vector3f tangentV = new Vector3f();
	public final Vector3f normal = new Vector3f();

	public final Vector3f transformedPosition = new Vector3f();
	public final Vector3f transformedTangentU = new Vector3f();
	public final Vector3f transformedTangentV = new Vector3f();
	public final Vector3f transformedNormal = new Vector3f();

	public Vertex(float u, float v) {
		this.u = u;
		this.v = v;
	}

	public void resetTransformed() {
		transformedPosition.set(position);
		transformedTangentU.set(tangentU);
		transformedTangentV.set(tangentV);
		transformedNormal.set(normal);
	}
}