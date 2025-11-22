package com.macroz.bezierrasterizer.logic;

import com.macroz.bezierrasterizer.model.Mesh;
import com.macroz.bezierrasterizer.model.Triangle;
import com.macroz.bezierrasterizer.model.Vertex;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public class SceneTransformer {

	private static final Matrix3f ROTATION_MATRIX = new Matrix3f();

	 // Applies rotation matrices to all vertices and control points in the mesh.
	 // Rotation order: Z axis (alpha) then X axis (beta).
	public static void transform(Mesh mesh, float alphaDeg, float betaDeg) {
		float alphaRad = (float) Math.toRadians(alphaDeg);
		float betaRad = (float) Math.toRadians(betaDeg);

		ROTATION_MATRIX.identity()
				.rotateX(betaRad)
				.rotateZ(alphaRad);

		Vector3f[] originalCP = mesh.getControlPoints();
		Vector3f[] transformedCP = mesh.getTransformedControlPoints();

		for (int i = 0; i < 16; i++) {
			transformedCP[i].set(originalCP[i]).mul(ROTATION_MATRIX);
		}

		for (Triangle t : mesh.getTriangles()) {
			for (Vertex v : t.vertices) {
				v.transformedPosition.set(v.position).mul(ROTATION_MATRIX);

				v.transformedTangentU.set(v.tangentU).mul(ROTATION_MATRIX);
				v.transformedTangentV.set(v.tangentV).mul(ROTATION_MATRIX);
				v.transformedNormal.set(v.normal).mul(ROTATION_MATRIX);
			}
		}
	}
}