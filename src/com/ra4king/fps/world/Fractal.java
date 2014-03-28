package com.ra4king.fps.world;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.APPLEVertexArrayObject;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.GLContext;

import com.ra4king.opengl.util.loader.PolygonLoader;
import com.ra4king.opengl.util.math.MatrixStack;
import com.ra4king.opengl.util.math.Vector2;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author ra4king
 */
public class Fractal {
	private CubeRenderer renderer;
	
	private int depth, change = 1;
	private long timePassed;
	
	private final float SPLIT_TIME = (float)1e9;
	private final int MAX_DEPTH = 21;
	
	public Fractal() {
		renderer = new CubeRenderer();
	}
	
	public int getDepth() {
		return depth;
	}
	
	public int getTotal() {
		return (int)(Math.pow(2, depth + 1) - 1);
	}
	
	public void update(long deltaTime) {
		timePassed += deltaTime;
		
		if(timePassed >= SPLIT_TIME) {
			timePassed -= SPLIT_TIME;
			
			if(depth <= MAX_DEPTH && depth >= 0)
				depth += change;
			
			if(depth == MAX_DEPTH || depth == 0)
				change *= -1;
		}
	}
	
	public void render(MatrixStack matrixStack) {
		renderer.render(matrixStack);
	}
	
	private class CubeRenderer {
		private FloatBuffer buffer;
		private int cubeVAO, dataVBO;
		
		private final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
		private final boolean HAS_VAO = GLContext.getCapabilities().OpenGL30;
		
		public CubeRenderer() {
			buffer = BufferUtils.createFloatBuffer((int)((Math.pow(2, MAX_DEPTH + 1) - 1) * 4 * 6 * 3));
			
			dataVBO = glGenBuffers();
			glBindBuffer(GL_ARRAY_BUFFER, dataVBO);
			glBufferData(GL_ARRAY_BUFFER, buffer.capacity() * 4, GL_STREAM_DRAW);
			
			cubeVAO = HAS_VAO ? glGenVertexArrays() : (IS_MAC ? APPLEVertexArrayObject.glGenVertexArraysAPPLE() : ARBVertexArrayObject.glGenVertexArrays());
			if(HAS_VAO)
				glBindVertexArray(cubeVAO);
			else if(IS_MAC)
				APPLEVertexArrayObject.glBindVertexArrayAPPLE(cubeVAO);
			else
				ARBVertexArrayObject.glBindVertexArray(cubeVAO);
			
			glEnableVertexAttribArray(0);
			glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * 4, 0);
			glEnableVertexAttribArray(1);
			glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * 4, 3 * 4);
			
			if(HAS_VAO)
				glBindVertexArray(0);
			else if(IS_MAC)
				APPLEVertexArrayObject.glBindVertexArrayAPPLE(0);
			else
				ARBVertexArrayObject.glBindVertexArray(0);
			
			glBindBuffer(GL_ARRAY_BUFFER, 0);
		}
		
		private final double pow = 1.3;
		private final float SIZE_CONSTANT = 3.7f;
		
		private void render(MatrixStack matrixStack, float angle, int curDepth, FloatBuffer buffer) {
			float radius = (float)Math.pow(pow, getDepth() - curDepth + change * timePassed / SPLIT_TIME) / SIZE_CONSTANT;
			
			matrixStack.getTop().translate(0, radius, 0);
			try {
				buffer.put(PolygonLoader.loadPlane(new Vector2(0.1f, 2 * radius), new Vector3(0, 0, 0), true, false, matrixStack.getTop()));
			} catch(Exception exc) {
				System.out.println(getDepth() + " " + getTotal() + " " + (getTotal() * 72) + " " + buffer.capacity());
				throw exc;
			}
			matrixStack.getTop().translate(0, radius, 0);
			
			if(curDepth < getDepth() && curDepth < MAX_DEPTH) {
				matrixStack.pushMatrix();
				matrixStack.getTop().rotate((float)(-Math.PI / 2 + angle), 0, 0, 1);
				render(matrixStack, angle, curDepth + 1, buffer);
				matrixStack.popMatrix();
				
				matrixStack.pushMatrix();
				matrixStack.getTop().rotate(angle, 0, 0, 1);
				render(matrixStack, angle, curDepth + 1, buffer);
				matrixStack.popMatrix();
			}
		}
		
		public void render(MatrixStack matrixStack) {
			matrixStack.getTop().translate(0, -20, Math.min(-40 * getDepth() - 40 * change * timePassed / SPLIT_TIME, -40));
			
			buffer.clear();
			
			float angle = (float)Math.toRadians((getDepth() + change * timePassed / SPLIT_TIME) * 45f / MAX_DEPTH);
			render(matrixStack, angle, 0, buffer);
			
			buffer.flip();
			
			glBindBuffer(GL_ARRAY_BUFFER, dataVBO);
			glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
			glBindBuffer(GL_ARRAY_BUFFER, 0);
			
			if(HAS_VAO)
				glBindVertexArray(cubeVAO);
			else if(IS_MAC)
				APPLEVertexArrayObject.glBindVertexArrayAPPLE(cubeVAO);
			else
				ARBVertexArrayObject.glBindVertexArray(cubeVAO);
			
			glDrawArrays(GL_TRIANGLES, 0, buffer.limit() / 6);
			
			if(HAS_VAO)
				glBindVertexArray(0);
			else if(IS_MAC)
				APPLEVertexArrayObject.glBindVertexArrayAPPLE(0);
			else
				ARBVertexArrayObject.glBindVertexArray(0);
		}
	}
}
