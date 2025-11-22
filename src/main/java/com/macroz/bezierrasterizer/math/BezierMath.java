package com.macroz.bezierrasterizer.math;

import com.macroz.bezierrasterizer.model.Vertex;
import org.joml.Vector3f;

public class BezierMath {

	// Reusable vector to reduce object creation (assumes single-threaded usage in Swing).
	private static final Vector3f TEMP_VEC = new Vector3f();

	// Bernstein polynomial of degree 3 (used for position calculation).
	private static float bernstein3(int i, float t) {
		float oneMinusT = 1.0f - t;
		return switch (i) {
			case 0 -> oneMinusT * oneMinusT * oneMinusT;
			case 1 -> 3 * oneMinusT * oneMinusT * t;
			case 2 -> 3 * oneMinusT * t * t;
			case 3 -> t * t * t;
			default -> 0;
		};
	}

	// Bernstein polynomial of degree 2 (used for derivative calculations).
	private static float bernstein2(int i, float t) {
		float oneMinusT = 1.0f - t;
		return switch (i) {
			case 0 -> oneMinusT * oneMinusT;
			case 1 -> 2 * oneMinusT * t;
			case 2 -> t * t;
			default -> 0;
		};
	}

	// Computes vertex attributes: position, tangents, and normal.
	public static void fillVertexAttributes(Vertex v, Vector3f[] cps) {
		v.position.zero();
		v.tangentU.zero();
		v.tangentV.zero();

		float u = v.u;
		float val_v = v.v;

		// Compute position P(u, v)
		for (int i = 0; i <= 3; i++) {
			float bi = bernstein3(i, u);
			for (int j = 0; j <= 3; j++) {
				float bj = bernstein3(j, val_v);
				v.position.fma(bi * bj, cps[i * 4 + j]);
			}
		}

		// Compute tangent Pu (partial derivative with respect to u)
		for (int i = 0; i <= 2; i++) {
			float bi = bernstein2(i, u);
			for (int j = 0; j <= 3; j++) {
				float bj = bernstein3(j, val_v);

				Vector3f pCurrent = cps[i * 4 + j];
				Vector3f pNext = cps[(i + 1) * 4 + j];

				TEMP_VEC.set(pNext).sub(pCurrent);
				v.tangentU.fma(3.0f * bi * bj, TEMP_VEC);
			}
		}

		// Compute tangent Pv (partial derivative with respect to v)
		for (int i = 0; i <= 3; i++) {
			float bi = bernstein3(i, u);
			for (int j = 0; j <= 2; j++) {
				float bj = bernstein2(j, val_v);

				Vector3f pCurrent = cps[i * 4 + j];
				Vector3f pNext = cps[i * 4 + (j + 1)];

				TEMP_VEC.set(pNext).sub(pCurrent);
				v.tangentV.fma(3.0f * bi * bj, TEMP_VEC);
			}
		}

		// Compute normal N = Pu x Pv
		v.tangentU.cross(v.tangentV, v.normal);

		// Normalize vectors
		if (v.normal.lengthSquared() > 1e-8f) {
			v.normal.normalize();
		} else {
			v.normal.set(0, 0, 1); // Default fallback
		}

		if (v.tangentU.lengthSquared() > 1e-8f) v.tangentU.normalize();
		if (v.tangentV.lengthSquared() > 1e-8f) v.tangentV.normalize();
	}
}