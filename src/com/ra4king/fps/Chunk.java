package com.ra4king.fps;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

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
	
	private int cubeCount;
	
	private final ChunkRenderer renderer;
	
	public static final int CUBES = GLUtils.get().VERSION >= 33 ? 16 : 8;
	public static final float CUBE_SIZE = 2;
	public static final float SPACING = 7;
	
	public Chunk(Vector3 corner, boolean random) {
		this.corner = corner.copy();
		
		blocks = new BlockType[CUBES * CUBES * CUBES];
		
		if(random)
			initializeRandomly();

		renderer = new ChunkRenderer();
	}
	
	public void initializeRandomly() {
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
	
	public void add(BlockType block, int x, int y, int z) {
		if(x >= CUBES || x < 0 ||
				y >= CUBES || y < 0 ||
				z >= CUBES || z < 0)
			throw new IllegalArgumentException("Invalid block position.");
		
		blocks[x * CUBES * CUBES + y * CUBES + z] = block;
	}
	
	public void update(long deltaTime) {
		
	}
	
	public void render() {
		renderer.render();
	}
	
	private class ChunkRenderer {
		private int dataVBO;
		
		private FloatBuffer buffer;
		
		public ChunkRenderer() {
			buffer = BufferUtils.createFloatBuffer(CUBES * CUBES * CUBES);
			
			if(GLUtils.get().VERSION >= 33) {
				dataVBO = glGenBuffers();
				glBindBuffer(GL_UNIFORM_BUFFER, dataVBO);
				glBufferData(GL_UNIFORM_BUFFER, buffer.capacity() * 4, GL_STATIC_DRAW);
				glBindBuffer(GL_UNIFORM_BUFFER, 0);
			}
			
			updateVBO();
		}
		
		public void updateVBO() {
			buffer.clear();
			
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
			
			glBindBuffer(GL_UNIFORM_BUFFER, dataVBO);
			glBufferSubData(GL_UNIFORM_BUFFER, 0, buffer);
			glBindBuffer(GL_UNIFORM_BUFFER, 0);
		}
		
		public void render() {
			if(GLUtils.get().VERSION >= 31)
				glBindBufferBase(GL_UNIFORM_BUFFER, 3, dataVBO);
			else
				glUniform1(FPS.cubesUniform, buffer);
			
			glDrawArrays(GL_TRIANGLES, 0, getCubeCount() * 36);
		}
	}
}
