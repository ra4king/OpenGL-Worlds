package com.ra4king.fps.renderers;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.ra4king.fps.renderers.WorldRenderer.DrawElementsIndirectCommand;
import com.ra4king.fps.world.Chunk;
import com.ra4king.fps.world.Chunk.Block;
import com.ra4king.fps.world.Chunk.BlockType;
import com.ra4king.opengl.util.Stopwatch;
import com.ra4king.opengl.util.Utils;

public class ChunkRenderer {
	private Chunk chunk;
	private int dataVBO;
	private int chunkNumOffset;
	private int lastCubeRenderCount;
	
	private static final int TOTAL_CUBES = Chunk.CHUNK_CUBE_WIDTH * Chunk.CHUNK_CUBE_HEIGHT * Chunk.CHUNK_CUBE_DEPTH;
	public static final int CHUNK_DATA_SIZE = TOTAL_CUBES * 4 * 4;
	
	public ChunkRenderer(Chunk chunk, int dataVBO, int chunkNumOffset) {
		this.chunk = chunk;
		this.dataVBO = dataVBO;
		this.chunkNumOffset = chunkNumOffset;
	}
	
	public Chunk getChunk() {
		return chunk;
	}
	
	private void updateVBO() {
		Stopwatch.start("UpdateVBO");
		
		glBindBuffer(GL_ARRAY_BUFFER, dataVBO);
		
		ByteBuffer tempMappedBuffer = glMapBufferRange(GL_ARRAY_BUFFER, chunkNumOffset * CHUNK_DATA_SIZE, CHUNK_DATA_SIZE,
				GL_MAP_WRITE_BIT | GL_MAP_UNSYNCHRONIZED_BIT, null);
		if(tempMappedBuffer == null) {
			Utils.checkGLError("mapped buffer");
			System.exit(0);
		}
		
		FloatBuffer cubeBuffer = tempMappedBuffer.asFloatBuffer();
		
		int cubesDrawn = 0;
		
		Stopwatch.start("Buffer Fill");
		
		Stopwatch.start("isSurrounded");

		for(Block block : chunk.getBlocks()) {
			if(block == null || block.getType() == BlockType.AIR) {
				continue;
			}
	
			Stopwatch.resume();
			try {
				if(block.isSurrounded())
					continue;
			} finally {
				Stopwatch.suspend();
			}
			
			cubeBuffer.put(block.getX()).put(block.getY()).put(-block.getZ()).put(Chunk.CUBE_SIZE);
			
			cubesDrawn++;
		}
		
		Stopwatch.end();
		
		Stopwatch.stop();
		
		glUnmapBuffer(GL_ARRAY_BUFFER);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		lastCubeRenderCount = cubesDrawn;
		
		Stopwatch.stop();
	}
	
	public int getLastCubeRenderCount() {
		return lastCubeRenderCount;
	}
	
	public void render(DrawElementsIndirectCommand command) {
		if(chunk.hasChanged()) {
			updateVBO();
		}
		
		if(lastCubeRenderCount == 0) {
			return;
		}
		
		command.instanceCount = lastCubeRenderCount;
		command.baseInstance = chunkNumOffset * TOTAL_CUBES;
	}
}
