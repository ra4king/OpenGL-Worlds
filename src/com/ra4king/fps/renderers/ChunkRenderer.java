package com.ra4king.fps.renderers;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.BufferUtils;

import com.ra4king.fps.GLUtils;
import com.ra4king.fps.world.Chunk;
import com.ra4king.fps.world.Chunk.BlockInfo;
import com.ra4king.fps.world.Chunk.ChunkInfo;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.math.Vector3;

public class ChunkRenderer {
	private Chunk chunk;
	private int chunkVAO;
	private int dataVBO;
	private int indicesVBO;
	private int lastTriangleRenderCount;
	
	private static final int CUBE_DATA_SIZE = Chunk.CHUNK_CUBE_WIDTH * Chunk.CHUNK_CUBE_HEIGHT * Chunk.CHUNK_CUBE_DEPTH *
			6 * /* 3 faces visible */
			4 * /* 4 vertices per face */
			2 * /* 2 attributes per vertex */
			3 * /* 3 floats per attribute */
			4 /* 4 bytes per float */;
	private static final int INDEX_DATA_SIZE = Chunk.CHUNK_CUBE_WIDTH * Chunk.CHUNK_CUBE_HEIGHT * Chunk.CHUNK_CUBE_DEPTH *
			6 * /* 3 faces visible */
			6 * /* 6 indices per face */
			2 /* 2 bytes per index */;
	
	private static final Vector3 normals[] = {
			new Vector3(0, 0, 1),
			new Vector3(0, 0, -1),
			new Vector3(0, 1, 0),
			new Vector3(0, -1, 0),
			new Vector3(1, 0, 0),
			new Vector3(-1, 0, 0)
	};
	private static final short[] indices = { 0, 1, 2, 2, 3, 0 };
	
	private static final Vector3 unitCube[] = {
			// front face triangle 1
			new Vector3(-0.5f, 0.5f, 0.5f),
			new Vector3(0.5f, 0.5f, 0.5f),
			new Vector3(0.5f, -0.5f, 0.5f),
			// front face triangle 2
			// new Vector3(0.5f, -0.5f, 0.5f),
			new Vector3(-0.5f, -0.5f, 0.5f),
			// new Vector3(-0.5f, 0.5f, 0.5f),
			
			// back face triangle 1
			new Vector3(0.5f, 0.5f, -0.5f),
			new Vector3(-0.5f, 0.5f, -0.5f),
			new Vector3(-0.5f, -0.5f, -0.5f),
			// back face triangle 2
			// new Vector3(-0.5f, -0.5f, -0.5f),
			new Vector3(0.5f, -0.5f, -0.5f),
			// new Vector3(0.5f, 0.5f, -0.5f),
			
			// top face triangle 1
			new Vector3(-0.5f, 0.5f, -0.5f),
			new Vector3(0.5f, 0.5f, -0.5f),
			new Vector3(0.5f, 0.5f, 0.5f),
			// top face triangle 2
			// new Vector3(0.5f, 0.5f, 0.5f),
			new Vector3(-0.5f, 0.5f, 0.5f),
			// new Vector3(-0.5f, 0.5f, -0.5f),
			
			// bottom face triangle 1
			new Vector3(-0.5f, -0.5f, 0.5f),
			new Vector3(0.5f, -0.5f, 0.5f),
			new Vector3(0.5f, -0.5f, -0.5f),
			// bottom face triangle 2
			// new Vector3(0.5f, -0.5f, -0.5f),
			new Vector3(-0.5f, -0.5f, -0.5f),
			// new Vector3(-0.5f, -0.5f, 0.5f),
			
			// right face triangle 1
			new Vector3(0.5f, 0.5f, 0.5f),
			new Vector3(0.5f, 0.5f, -0.5f),
			new Vector3(0.5f, -0.5f, -0.5f),
			// right face triangle 2
			// new Vector3(0.5f, -0.5f, -0.5f),
			new Vector3(0.5f, -0.5f, 0.5f),
			// new Vector3(0.5f, 0.5f, 0.5f),
			
			// left face triangle 1
			new Vector3(-0.5f, 0.5f, -0.5f),
			new Vector3(-0.5f, 0.5f, 0.5f),
			new Vector3(-0.5f, -0.5f, 0.5f),
			// left face triangle 2
			// new Vector3(-0.5f, -0.5f, 0.5f),
			new Vector3(-0.5f, -0.5f, -0.5f),
			// new Vector3(-0.5f, 0.5f, -0.5f)
	};
	
	private static final FloatBuffer tempCubeBuffer;
	private static final ShortBuffer tempIndicesBuffer;
	
	static {
		tempCubeBuffer = BufferUtils.createFloatBuffer(CUBE_DATA_SIZE / 4);
		tempIndicesBuffer = BufferUtils.createShortBuffer(INDEX_DATA_SIZE / 2);
	}
	
	public ChunkRenderer(Chunk chunk) {
		this.chunk = chunk;
		
		chunkVAO = GLUtils.get().glGenVertexArrays();
		GLUtils.get().glBindVertexArray(chunkVAO);
		
		dataVBO = glGenBuffers();
		indicesVBO = glGenBuffers();
		
		glBindBuffer(GL_ARRAY_BUFFER, dataVBO);
		glBufferData(GL_ARRAY_BUFFER, CUBE_DATA_SIZE, GL_STREAM_DRAW);
		
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesVBO);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, INDEX_DATA_SIZE, GL_STREAM_DRAW);
		
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 2 * 3 * 4, 0);
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 3, GL_FLOAT, false, 2 * 3 * 4, 3 * 4);
		
		GLUtils.get().glBindVertexArray(0);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	}
	
	public Chunk getChunk() {
		return chunk;
	}
	
	private static Vector3 cubeTemp = new Vector3();
	
	private void updateVBO() {
		final boolean USE_MAPPED_BUFFERS = true;
		
		int trianglesDrawn = 0;
		
		glBindBuffer(GL_ARRAY_BUFFER, dataVBO);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesVBO);
		
		FloatBuffer cubeBuffer;
		ShortBuffer indicesBuffer;
		
		if(USE_MAPPED_BUFFERS) {
			ByteBuffer tempMappedBuffer = glMapBufferRange(GL_ARRAY_BUFFER, 0, CUBE_DATA_SIZE, GL_MAP_WRITE_BIT, null);
			if(tempMappedBuffer == null) {
				Utils.checkGLError("mapped buffer");
				System.exit(0);
			}
			cubeBuffer = tempMappedBuffer.asFloatBuffer();
			
			tempMappedBuffer = glMapBufferRange(GL_ELEMENT_ARRAY_BUFFER, 0, INDEX_DATA_SIZE, GL_MAP_WRITE_BIT, null);
			if(tempMappedBuffer == null) {
				Utils.checkGLError("mapped buffer 2");
				System.exit(0);
			}
			indicesBuffer = tempMappedBuffer.asShortBuffer();
		}
		else {
			cubeBuffer = tempCubeBuffer;
			indicesBuffer = tempIndicesBuffer;
			
			cubeBuffer.clear();
			indicesBuffer.clear();
		}
		
		short indexOffset = 0;
		
		for(BlockInfo block : chunk.getBlocks()) {
			if(block == null || isSurrounded(block)) {
				continue;
			}
			
			for(int b = 0; b < 6 * 4; b++) {
				cubeTemp.set(unitCube[b]).add(0.5f, 0.5f, -0.5f).mult(Chunk.CUBE_SIZE);
				cubeTemp.add(block.getWorldX() * Chunk.CUBE_SIZE, block.getWorldY() * Chunk.CUBE_SIZE, -block.getWorldZ() * Chunk.CUBE_SIZE);
				
				try {
					cubeBuffer.put(cubeTemp.toBuffer());
					cubeBuffer.put(normals[b / 4].toBuffer());
				} catch(Exception exc) {
					System.out.println(cubeBuffer.position() + " " + cubeBuffer.capacity());
					exc.printStackTrace();
					System.exit(0);
				}
			}
			
			for(int i = 0; i < 6 * indices.length; i++) {
				indicesBuffer.put((short)(indices[i % indices.length] + (i / indices.length) * 4 + indexOffset));
			}
			
			indexOffset += 24;
			trianglesDrawn += 12;
		}
		
		if(USE_MAPPED_BUFFERS) {
			glUnmapBuffer(GL_ARRAY_BUFFER);
			glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);
		}
		else {
			cubeBuffer.flip();
			indicesBuffer.flip();
			
			glBufferSubData(GL_ARRAY_BUFFER, 0, cubeBuffer);
			glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, indicesBuffer);
		}
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		
		lastTriangleRenderCount = trianglesDrawn;
	}
	
	public int getLastTriangleRenderCount() {
		return lastTriangleRenderCount;
	}
	
	private boolean isSurrounded(Chunk.BlockInfo block) {
		int x = block.x;
		int y = block.y;
		int z = block.z;
		
		ChunkInfo chunkInfo = chunk.getChunkInfo();
		
		for(int ix = -1; ix < 2; ix++) {
			for(int iy = -1; iy < 2; iy++) {
				for(int iz = -1; iz < 2; iz++) {
					if(ix == 0 && iy == 0 && iz == 0) {
						continue;
					}
					
					boolean blocked;
					
					if(!chunk.isValidPos(x + ix, y + iy, z + iz)) {
						blocked = chunk.getChunkManager().getBlock(chunkInfo.chunkCornerX + x + ix, chunkInfo.chunkCornerY + y + iy, chunkInfo.chunkCornerZ + z + iz) != null;
					}
					else {
						blocked = chunk.get(x + ix, y + iy, z + iz) != null;
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

		if(lastTriangleRenderCount == 0) {
			return;
		}
		
		GLUtils.get().glBindVertexArray(chunkVAO);
		glDrawElements(GL_TRIANGLES, lastTriangleRenderCount * 3, GL_UNSIGNED_SHORT, 0);
		GLUtils.get().glBindVertexArray(0);
	}
}
