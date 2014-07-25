package com.ra4king.fps.renderers;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import com.ra4king.fps.GLUtils;
import com.ra4king.fps.world.Chunk;
import com.ra4king.fps.world.Chunk.BlockInfo;
import com.ra4king.opengl.util.Utils;

public class ChunkRenderer {
	private Chunk chunk;
	private int chunkVAO;
	private int dataVBO;
	private int lastCubeRenderCount;
	
	private static final int CUBE_DATA_SIZE = Chunk.CHUNK_CUBE_WIDTH * Chunk.CHUNK_CUBE_HEIGHT * Chunk.CHUNK_CUBE_DEPTH *
			4 * /* 4 floats per cube */
			4 /* 4 bytes per float */;
	
	private static final FloatBuffer tempCubeBuffer;
	
	static {
		tempCubeBuffer = BufferUtils.createFloatBuffer(CUBE_DATA_SIZE / 4);
	}
	
	public ChunkRenderer(Chunk chunk, int cubeVBO, int indicesVBO) {
		this.chunk = chunk;
		
		chunkVAO = GLUtils.glGenVertexArrays();
		GLUtils.glBindVertexArray(chunkVAO);
		
		glBindBuffer(GL_ARRAY_BUFFER, cubeVBO);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesVBO);
		
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 2 * 3 * 4, 0);
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 3, GL_FLOAT, false, 2 * 3 * 4, 3 * 4);
		
		dataVBO = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, dataVBO);
		glBufferData(GL_ARRAY_BUFFER, CUBE_DATA_SIZE, GL_STREAM_DRAW);
		
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(2, 3, GL_FLOAT, false, 4 * 4, 0);
		GLUtils.glVertexAttribDivisor(2, 1);
		
		glEnableVertexAttribArray(3);
		glVertexAttribPointer(3, 1, GL_FLOAT, false, 4 * 4, 3 * 4);
		GLUtils.glVertexAttribDivisor(3, 1);
		
		GLUtils.glBindVertexArray(0);

		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	}
	
	public Chunk getChunk() {
		return chunk;
	}
	
	private void updateVBO() {
		final boolean USE_MAPPED_BUFFERS = true;
		
		int cubesDrawn = 0;

		glBindBuffer(GL_ARRAY_BUFFER, dataVBO);
		
		FloatBuffer cubeBuffer;
		
		if(USE_MAPPED_BUFFERS) {
			ByteBuffer tempMappedBuffer = glMapBufferRange(GL_ARRAY_BUFFER, 0, CUBE_DATA_SIZE, GL_MAP_WRITE_BIT, null);
			if(tempMappedBuffer == null) {
				Utils.checkGLError("mapped buffer");
				System.exit(0);
			}
			cubeBuffer = tempMappedBuffer.asFloatBuffer();
		}
		else {
			cubeBuffer = tempCubeBuffer;
			cubeBuffer.clear();
		}
		
		for(BlockInfo block : chunk.getBlocks()) {
			if(block == null || isSurrounded(block)) {
				continue;
			}
			
			cubeBuffer.put(block.getWorldX()).put(block.getWorldY()).put(-block.getWorldZ()).put(Chunk.CUBE_SIZE);
			
			cubesDrawn++;
		}

		if(USE_MAPPED_BUFFERS) {
			glUnmapBuffer(GL_ARRAY_BUFFER);
		}
		else {
			cubeBuffer.flip();
			
			glBufferSubData(GL_ARRAY_BUFFER, 0, cubeBuffer);
		}
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		lastCubeRenderCount = cubesDrawn;
	}
	
	public int getLastCubeRenderCount() {
		return lastCubeRenderCount;
	}
	
	private boolean isSurrounded(Chunk.BlockInfo block) {
		int x = block.x;
		int y = block.y;
		int z = block.z;
		
		for(int ix = -1; ix < 2; ix++) {
			for(int iy = -1; iy < 2; iy++) {
				for(int iz = -1; iz < 2; iz++) {
					if(ix == 0 && iy == 0 && iz == 0) {
						continue;
					}
					
					boolean blocked;
					
					if(chunk.isValidPos(x + ix, y + iy, z + iz)) {
						blocked = chunk.get(x + ix, y + iy, z + iz) != null;
					}
					else {
						blocked = false;// chunk.getChunkManager().getBlock(chunkInfo.chunkCornerX + x + ix, chunkInfo.chunkCornerY + y + iy, chunkInfo.chunkCornerZ + z + iz) != null;
	}
	
					if(!blocked) {
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	public void render() {
		if(chunk.hasChanged()) {
			updateVBO();
		}
		
		if(lastCubeRenderCount == 0) {
			return;
		}
		
		GLUtils.glBindVertexArray(chunkVAO);
		GLUtils.glDrawElementsInstanced(GL_TRIANGLES, 6 * 6, GL_UNSIGNED_SHORT, 0, lastCubeRenderCount);
		GLUtils.glBindVertexArray(0);
	}
}
