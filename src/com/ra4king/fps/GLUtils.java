package com.ra4king.fps;

import org.lwjgl.opengl.APPLEVertexArrayObject;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.Vector3;
import com.ra4king.opengl.util.math.Vector4;

/**
 * @author ra4king
 */
public final class GLUtils {
	public final int VERSION;
	
	public final boolean IS_MAC;
	
	public final boolean HAS_VAO;
	public final boolean HAS_ARB_VAO;
	
	public int CUBES_UNIFORM;
	
	public final String SHADERS_ROOT_PATH = "/res/shaders/";
	
	private GLUtils() {
		VERSION = GLContext.getCapabilities().OpenGL33 ? 33 : GLContext.getCapabilities().OpenGL30 ? 30 : GLContext.getCapabilities().OpenGL21 ? 21 : 0;
		
		IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
		
		HAS_VAO = VERSION >= 30;
		HAS_ARB_VAO = GLContext.getCapabilities().GL_ARB_vertex_array_object;
	}
	
	private static final GLUtils glUtils = new GLUtils();
	
	public static GLUtils get() {
		return glUtils;
	}
	
	public int getWidth() {
		return Display.getWidth();
	}
	
	public int getHeight() {
		return Display.getHeight();
	}
	
	public void glBindVertexArray(int vao) {
		if(HAS_VAO)
			GL30.glBindVertexArray(vao);
		else if(IS_MAC)
			APPLEVertexArrayObject.glBindVertexArrayAPPLE(vao);
		else if(HAS_ARB_VAO)
			ARBVertexArrayObject.glBindVertexArray(vao);
		else
			throw new UnsupportedOperationException("VAOs not supported on this system.");
	}
	
	public int glGenVertexArrays() {
		if(HAS_VAO)
			return GL30.glGenVertexArrays();
		else if(IS_MAC)
			return APPLEVertexArrayObject.glGenVertexArraysAPPLE();
		else if(HAS_ARB_VAO)
			return ARBVertexArrayObject.glGenVertexArrays();
		else
			throw new UnsupportedOperationException("VAOs not supported on this system.");
	}
	
	public static class FrustumCulling {
		private enum Plane {
			LEFT, RIGHT, BOTTOM, TOP, NEAR, FAR;
			
			private Vector4 plane;
			
			public static final Plane[] values = values();
			
			private final Vector3 temp = new Vector3();
			
			public float distanceFromPoint(Vector3 point) {
				return temp.set(plane).dot(point) + plane.w();
			}
		}
		
		public void setupPlanes(Matrix4 matrix) {
			Plane.LEFT.plane = getPlane(matrix, 1);
			Plane.RIGHT.plane = getPlane(matrix, -1);
			Plane.BOTTOM.plane = getPlane(matrix, 2);
			Plane.TOP.plane = getPlane(matrix, -2);
			Plane.NEAR.plane = getPlane(matrix, 3);
			Plane.FAR.plane = getPlane(matrix, -3);
		}
		
		private Vector4 getPlane(Matrix4 matrix, int row) {
			int scale = row < 0 ? -1 : 1;
			row = Math.abs(row) - 1;
			
			return new Vector4(
					matrix.get(3) + scale * matrix.get(row),
					matrix.get(7) + scale * matrix.get(row + 4),
					matrix.get(11) + scale * matrix.get(row + 8),
					matrix.get(15) + scale * matrix.get(row + 12)).normalize();
		}
		
		private final Vector3 temp = new Vector3();
		
		public boolean isCubeInsideFrustum(Vector3 center, float sideLength) {
			for(Plane p : Plane.values) {
				float d = sideLength / 2;
				
				boolean isIn;
				
				isIn = p.distanceFromPoint(temp.set(center).add(d, d, d)) >= 0;
				isIn |= p.distanceFromPoint(temp.set(center).add(d, d, -d)) >= 0;
				isIn |= p.distanceFromPoint(temp.set(center).add(d, -d, d)) >= 0;
				isIn |= p.distanceFromPoint(temp.set(center).add(d, -d, -d)) >= 0;
				isIn |= p.distanceFromPoint(temp.set(center).add(-d, d, d)) >= 0;
				isIn |= p.distanceFromPoint(temp.set(center).add(-d, d, -d)) >= 0;
				isIn |= p.distanceFromPoint(temp.set(center).add(-d, -d, d)) >= 0;
				isIn |= p.distanceFromPoint(temp.set(center).add(-d, -d, -d)) >= 0;
				
				if(!isIn)
					return false;
			}
			
			return true;
		}
		
		public boolean isPointInsideFrustum(Vector3 point) {
			boolean isIn = true;
			
			for(Plane p : Plane.values)
				isIn &= p.distanceFromPoint(point) >= 0;
			
			return isIn;
		}
	}
}
