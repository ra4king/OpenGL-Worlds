package com.ra4king.fps;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.APPLEVertexArrayObject;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.GLContext;

import com.ra4king.fps.FPS.FrustumCulling;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author ra4king
 */
public class Chunk {
	public enum BlockType {
		DIRT
	}
	
	private final BlockType[] blocks;
	private final Vector3 corner;
	
	private final int cubeCount;
	
	private final ChunkRenderer renderer;
	
	public static final int CUBES = 16;
	public static final float CUBE_SIZE = 2;
	public static final float SPACING = 7;
	
	public Chunk(Vector3 corner) {
		this.corner = corner.copy();
		
		blocks = new BlockType[CUBES * CUBES * CUBES];
		
		cubeCount = (int)(Math.random() * blocks.length / 10);
		
		for(int a = 0; a < cubeCount; a++) {
			int ix;
			do {
				ix = (int)(Math.random() * blocks.length);
				
				if(blocks[ix] != null) {
					ix = -1;
					continue;
				}
				
				blocks[ix] = BlockType.DIRT;
			} while(ix == -1);
		}
		
		renderer = new ChunkRenderer();
	}
	
	public int getCubeCount() {
		return cubeCount;
	}
	
	public Vector3 getCorner() {
		return corner;
	}
	
	public BlockType get(int x, int y, int z) {
		return blocks[x * CUBES * CUBES + y * CUBES + z];
	}
	
	public void update(long deltaTime) {
		
	}
	
	public void render(FrustumCulling culling, boolean viewChanged) {
		renderer.render(culling, viewChanged);
	}
	
	private class ChunkRenderer {
		private int vao;
		private int dataVBO;
		
		private final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
		private final boolean HAS_VAO = GLContext.getCapabilities().OpenGL30;
		
		public ChunkRenderer() {
			FloatBuffer buffer = BufferUtils.createFloatBuffer(getCubeCount() * 4);
			
			for(int x = 0; x < CUBES; x++) {
				for(int y = 0; y < CUBES; y++) {
					for(int z = 0; z < CUBES; z++) {
						if(get(x, y, z) == null)
							continue;
						
						buffer.put(corner.x() + x * SPACING + CUBE_SIZE / 2).put(corner.y() + y * SPACING + CUBE_SIZE / 2).put(corner.z() - z * SPACING - CUBE_SIZE / 2).put(CUBE_SIZE);
					}
				}
			}
			
			buffer.flip();
			
			dataVBO = glGenBuffers();
			glBindBuffer(GL_UNIFORM_BUFFER, dataVBO);
			glBufferData(GL_UNIFORM_BUFFER, buffer, GL_STATIC_DRAW);
			glBindBuffer(GL_UNIFORM_BUFFER, 0);
			
			vao = HAS_VAO ? glGenVertexArrays() : (IS_MAC ? APPLEVertexArrayObject.glGenVertexArraysAPPLE() : ARBVertexArrayObject.glGenVertexArrays());
		}
		
		public void render(FrustumCulling culling, boolean viewChanged) {
			if(HAS_VAO)
				glBindVertexArray(vao);
			else if(IS_MAC)
				APPLEVertexArrayObject.glBindVertexArrayAPPLE(vao);
			else
				ARBVertexArrayObject.glBindVertexArray(vao);
			
			glBindBufferBase(GL_UNIFORM_BUFFER, 3, dataVBO);
			glDrawArrays(GL_TRIANGLES, 0, getCubeCount() * 36);
			
			if(HAS_VAO)
				glBindVertexArray(0);
			else if(IS_MAC)
				APPLEVertexArrayObject.glBindVertexArrayAPPLE(0);
			else
				ARBVertexArrayObject.glBindVertexArray(0);
		}
	}
}
