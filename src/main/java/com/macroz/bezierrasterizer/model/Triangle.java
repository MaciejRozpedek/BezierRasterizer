package com.macroz.bezierrasterizer.model;

public class Triangle {
	public final Vertex[] vertices = new Vertex[3];

	public Triangle(Vertex v1, Vertex v2, Vertex v3) {
		this.vertices[0] = v1;
		this.vertices[1] = v2;
		this.vertices[2] = v3;
	}

	public Vertex getV1() { return vertices[0]; }
	public Vertex getV2() { return vertices[1]; }
	public Vertex getV3() { return vertices[2]; }
}