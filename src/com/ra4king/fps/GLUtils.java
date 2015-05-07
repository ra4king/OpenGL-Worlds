package com.ra4king.fps;

import org.lwjgl.opengl.APPLEVertexArrayObject;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLContext;

/**
 * @author Roi Atalla
 */
public final class GLUtils {
	public static final int GL_VERSION;
	
	public static final boolean IS_MAC;
	
	private static int queryObject;
	
	public static final String RESOURCES_ROOT_PATH = "/res/";
	
	static {
		GL_VERSION = GLContext.getCapabilities().OpenGL33 ? 33 : GLContext.getCapabilities().OpenGL32 ? 32 : GLContext.getCapabilities().OpenGL30 ? 30 : GLContext.getCapabilities().OpenGL21 ? 21 : 0;
		
		IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
		
		queryObject = GL15.glGenQueries();
	}
	
	/**
	 * Dummy init method to force initialization of GLUtils singleton instance.
	 */
	public static void init() {}
	
	public static int getWidth() {
		return Display.getWidth();
	}
	
	public static int getHeight() {
		return Display.getHeight();
	}
	
	public static void glBindVertexArray(int vao) {
		if(GL_VERSION >= 30) {
			GL30.glBindVertexArray(vao);
		}
		else if(IS_MAC) {
			APPLEVertexArrayObject.glBindVertexArrayAPPLE(vao);
		}
		else if(GLContext.getCapabilities().GL_ARB_vertex_array_object) {
			ARBVertexArrayObject.glBindVertexArray(vao);
		}
		else {
			throw new UnsupportedOperationException("VAOs not supported on this system.");
		}
	}
	
	public static int glGenVertexArrays() {
		if(GL_VERSION >= 30) {
			return GL30.glGenVertexArrays();
		}
		else if(IS_MAC) {
			return APPLEVertexArrayObject.glGenVertexArraysAPPLE();
		}
		else if(GLContext.getCapabilities().GL_ARB_vertex_array_object) {
			return ARBVertexArrayObject.glGenVertexArrays();
		}
		else {
			throw new UnsupportedOperationException("VAOs not supported on this system.");
		}
	}
	
	public static void glDrawArraysInstanced(int mode, int first, int count, int primcount) {
		if(GL_VERSION >= 31) {
			GL31.glDrawArraysInstanced(mode, first, count, primcount);
		}
		else if(GLContext.getCapabilities().GL_ARB_draw_instanced) {
			ARBDrawInstanced.glDrawArraysInstancedARB(mode, first, count, primcount);
		}
		else {
			throw new UnsupportedOperationException("GL_ARB_draw_instanced not supported on this system.");
		}
	}
	
	public static void glDrawElementsInstanced(int mode, int indices_count, int type, long indices_buffer_offset, int primcount) {
		if(GL_VERSION >= 31) {
			GL31.glDrawElementsInstanced(mode, indices_count, type, indices_buffer_offset, primcount);
		}
		else if(GLContext.getCapabilities().GL_ARB_draw_instanced) {
			ARBDrawInstanced.glDrawElementsInstancedARB(mode, indices_count, type, indices_buffer_offset, primcount);
		}
		else {
			throw new UnsupportedOperationException("GL_ARB_draw_instanced not supported on this system.");
		}
	}
	
	public static void glVertexAttribDivisor(int index, int divisor) {
		if(GL_VERSION >= 33) {
			GL33.glVertexAttribDivisor(index, divisor);
		} else if(GLContext.getCapabilities().GL_ARB_instanced_arrays) {
			ARBInstancedArrays.glVertexAttribDivisorARB(index, divisor);
		} else {
			throw new UnsupportedOperationException("GL_ARB_instanced_arrays not supported on this system.");
		}
	}
	
	public static long getTimeStamp() {
		GL33.glQueryCounter(queryObject, GL33.GL_TIMESTAMP);
		return GL33.glGetQueryObjecti64(queryObject, GL15.GL_QUERY_RESULT);
	}
}
