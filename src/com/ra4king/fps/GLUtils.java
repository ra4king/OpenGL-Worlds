package com.ra4king.fps;

import org.lwjgl.opengl.APPLEVertexArrayObject;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

/**
 * @author ra4king
 */
public final class GLUtils {
	public final int VERSION;
	
	public final boolean IS_MAC;
	
	public final boolean HAS_VAO;
	public final boolean HAS_ARB_VAO;
	
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
}
