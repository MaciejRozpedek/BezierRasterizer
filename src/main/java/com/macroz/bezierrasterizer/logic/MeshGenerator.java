package com.macroz.bezierrasterizer.logic;

import com.macroz.bezierrasterizer.math.BezierMath;
import com.macroz.bezierrasterizer.model.Mesh;
import com.macroz.bezierrasterizer.model.Triangle;
import com.macroz.bezierrasterizer.model.Vertex;
import org.joml.Vector3f;

public class MeshGenerator {

	public static void triangulate(Mesh mesh, int precision) {
		// Ensure precision is at least 1
		if (precision < 1) precision = 1;

		mesh.clearMesh();
		Vector3f[] controlPoints = mesh.getControlPoints();

		// Vertex grid [rows][cols]
		Vertex[][] grid = new Vertex[precision + 1][precision + 1];
		float step = 1.0f / precision;

		// 1. Generate vertices
		for (int i = 0; i <= precision; i++) {
			float u = (i == precision) ? 1.0f : i * step; // Avoid rounding errors

			for (int j = 0; j <= precision; j++) {
				float v = (j == precision) ? 1.0f : j * step;

				Vertex vertex = new Vertex(u, v);

				// Compute Bezier surface attributes
				BezierMath.fillVertexAttributes(vertex, controlPoints);

				// Initialize transformed state
				vertex.resetTransformed();

				grid[i][j] = vertex;
			}
		}

		// 2. Create triangles
		for (int i = 0; i < precision; i++) {
			for (int j = 0; j < precision; j++) {
				Vertex v00 = grid[i][j];
				Vertex v10 = grid[i + 1][j];
				Vertex v01 = grid[i][j + 1];
				Vertex v11 = grid[i + 1][j + 1];

				// Two triangles per grid cell
				mesh.addTriangle(new Triangle(v00, v10, v01));
				mesh.addTriangle(new Triangle(v10, v11, v01));
			}
		}
	}
}