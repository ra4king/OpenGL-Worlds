package com.ra4king.fps.world;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.BufferUtils;

import com.ra4king.fps.GLUtils;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.math.Matrix3;
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author Roi Atalla
 */
public class Chunk {
	public static final int CUBES_WIDTH = 16, CUBES_HEIGHT = 16, CUBES_DEPTH = 16;
	private static final int CUBE_DATA_SIZE = CUBES_WIDTH * CUBES_HEIGHT * CUBES_DEPTH *
			3 * /* 3 faces visible */
			4 * /* 4 vertices per face */
			2 * /* 2 attributes per vertex */
			3 * /* 3 floats per attribute */
			4 /* 4 bytes per float */;
	private static final int INDEX_DATA_SIZE = CUBES_WIDTH * CUBES_HEIGHT * CUBES_DEPTH *
			3 * /* 3 faces visible */
			6 * /* 6 indices per face */
			2 /* 2 bytes per index */;
	public static final float CUBE_SIZE = 2;
	public static final float SPACING = CUBE_SIZE; // cannot be less than CUBE_SIZE
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
	private static final FloatBuffer tempCubeBuffer, tempDirectCubeBuffer;
	private static final ShortBuffer tempIndicesBuffer, tempDirectIndicesBuffer;
	// z * width * height + y * width + x
	private final BlockInfo[] blocks;
	private final ChunkInfo chunkInfo;
	private ChunkManager manager;
	private ChunkRenderer renderer;
	private int cubeCount;
	private int lastTriangleRenderCount;
	static {
		tempCubeBuffer = FloatBuffer.allocate(CUBE_DATA_SIZE / 4);
		tempDirectCubeBuffer = BufferUtils.createFloatBuffer(CUBE_DATA_SIZE / 4);
		
		tempIndicesBuffer = ShortBuffer.allocate(INDEX_DATA_SIZE / 2);
		tempDirectIndicesBuffer = BufferUtils.createShortBuffer(INDEX_DATA_SIZE / 2);
	}
	
	public Chunk(ChunkManager manager, ChunkInfo chunkInfo, boolean random) {
		this.manager = manager;
		
		this.chunkInfo = chunkInfo;
		
		blocks = new BlockInfo[CUBES_WIDTH * CUBES_HEIGHT * CUBES_DEPTH];
		
		if(random)
			initializeRandomly();
		else
			initializeAll();
		
		renderer = new ChunkRenderer();
	}
	
	private int posToArrayIndex(int x, int y, int z) {
		return z * CUBES_WIDTH * CUBES_HEIGHT + y * CUBES_WIDTH + x;
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
				
				int rem = ix % (CUBES_WIDTH * CUBES_HEIGHT);
				int x = rem % CUBES_WIDTH;
				int y = rem / CUBES_WIDTH;
				int z = ix / (CUBES_WIDTH * CUBES_HEIGHT);
				
				blocks[ix] = new BlockInfo(chunkInfo, x, y, z, BlockType.DIRT);
			} while(ix == -1);
		}
	}
	
	public void initializeAll() {
		cubeCount = blocks.length;
		
		for(int a = 0; a < cubeCount; a++) {
			int ix;
			do {
				ix = a;
				
				if(blocks[ix] != null) {
					ix = -1;
					continue;
				}
				
				int rem = ix % (CUBES_WIDTH * CUBES_HEIGHT);
				int x = rem % CUBES_WIDTH;
				int y = rem / CUBES_WIDTH;
				int z = ix / (CUBES_WIDTH * CUBES_HEIGHT);
				
				blocks[ix] = new BlockInfo(chunkInfo, x, y, z, BlockType.DIRT);
			} while(ix == -1);
		}
	}
	
	public int getCubeCount() {
		return cubeCount;
	}
	
	public ChunkInfo getChunkInfo() {
		return chunkInfo;
	}
	
	private boolean isValidPos(int x, int y, int z) {
		return !(x < 0 || x >= CUBES_WIDTH || y < 0 || y >= CUBES_HEIGHT || z < 0 || z >= CUBES_DEPTH);
	}
	
	public BlockInfo get(int x, int y, int z) {
		if(!isValidPos(x, y, z))
			return null;
		
		return blocks[posToArrayIndex(x, y, z)];
	}
	
	public void set(BlockType block, int x, int y, int z) {
		if(!isValidPos(x, y, z))
			throw new IllegalArgumentException("Invalid block position.");
		
		int i = posToArrayIndex(x, y, z);
		BlockInfo blockInfo = blocks[i];
		if(blockInfo == null) {
			blocks[i] = new BlockInfo(chunkInfo, x, y, z, block);
			cubeCount++;
		}
		else
			blockInfo.type = block;
	}
	
	public boolean remove(int x, int y, int z) {
		if(!isValidPos(x, y, z))
			throw new IllegalArgumentException("Invalid block position.");
		
		int i = posToArrayIndex(x, y, z);
		BlockInfo block = blocks[i];
		blocks[i] = null;
		
		if(block != null) {
			cubeCount--;
		}
		
		return block != null;
	}
	
	public int getLastTriangleRenderCount() {
		return lastTriangleRenderCount;
	}
	
	public void render(Matrix4 viewMatrix, Matrix3 normalMatrix) {
		renderer.render(viewMatrix, normalMatrix);
	}
	
	public enum BlockType {
		DIRT(1);
		
		public final int order;
		
		private BlockType(int order) {
			this.order = order;
		}
	}
	
	public static class BlockInfo {
		public final ChunkInfo chunkInfo;
		public final int x, y, z;
		public BlockType type;
		
		public BlockInfo(ChunkInfo chunkInfo, int x, int y, int z, BlockType type) {
			this.chunkInfo = chunkInfo;
			this.x = x;
			this.y = y;
			this.z = z;
			this.type = type;
		}
		
		public BlockInfo(BlockInfo b) {
			this.chunkInfo = new ChunkInfo(b.chunkInfo);
			this.x = b.x;
			this.y = b.y;
			this.z = b.z;
			this.type = b.type;
		}
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof BlockInfo) {
				BlockInfo blockInfo = (BlockInfo)o;
				return chunkInfo.equals(blockInfo.chunkInfo) && x == blockInfo.x && y == blockInfo.y && z == blockInfo.z;
			}
			
			return false;
		}
		
		public int getWorldX() {
			return chunkInfo.chunkCornerX + x;
		}
		
		public int getWorldY() {
			return chunkInfo.chunkCornerY + y;
		}
		
		public int getWorldZ() {
			return chunkInfo.chunkCornerZ + z;
		}
	}
	
	public static class ChunkInfo {
		public final int chunkCornerX, chunkCornerY, chunkCornerZ;
		
		public ChunkInfo(int chunkCornerX, int chunkCornerY, int chunkCornerZ) {
			this.chunkCornerX = chunkCornerX;
			this.chunkCornerY = chunkCornerY;
			this.chunkCornerZ = chunkCornerZ;
		}
		
		public ChunkInfo(ChunkInfo c) {
			this.chunkCornerX = c.chunkCornerX;
			this.chunkCornerY = c.chunkCornerY;
			this.chunkCornerZ = c.chunkCornerZ;
		}
		
		public boolean cornerEquals(int x, int y, int z) {
			return chunkCornerX == x && chunkCornerY == y && chunkCornerZ == z;
		}
		
		public boolean containsBlock(int x, int y, int z) {
			return cornerEquals((x / CUBES_WIDTH) * CUBES_WIDTH, (y / CUBES_HEIGHT) * CUBES_HEIGHT, (z / CUBES_DEPTH) * CUBES_DEPTH);
		}
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof ChunkInfo) {
				ChunkInfo chunkInfo = (ChunkInfo)o;
				return cornerEquals(chunkInfo.chunkCornerX, chunkInfo.chunkCornerY, chunkInfo.chunkCornerZ);
			}
			
			return false;
		}
	}
	
	private class ChunkRenderer {
		private int chunkVAO;
		private int dataVBO;
		private int indicesVBO;
		private Vector3 posTemp = new Vector3();
		private Vector3 cameraPosTemp = new Vector3();
		private Vector3 normalTemp = new Vector3();
		private Vector3 cubeTemp = new Vector3();
		public ChunkRenderer() {
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

		private boolean isSurrounded(BlockInfo block) {
			int x = block.x;
			int y = block.y;
			int z = block.z;
			
			for(int ix = -1; ix < 2; ix++) {
				for(int iy = -1; iy < 2; iy++) {
					for(int iz = -1; iz < 2; iz++) {
						if(ix == 0 && iy == 0 && iz == 0)
							continue;
						
						boolean blocked;
						
						if(!isValidPos(x + ix, y + iy, z + iz))
							blocked = manager.getBlock(chunkInfo.chunkCornerX + x + ix, chunkInfo.chunkCornerY + y + iy, chunkInfo.chunkCornerZ + z + iz) != null;
						else
							blocked = get(x + ix, y + iy, z + iz) != null;
						
						if(!blocked)
							return false;
					}
				}
			}
			
			return true;
		}
		
		public void render(Matrix4 viewMatrix, Matrix3 normalMatrix) {
			final boolean USE_MAPPED_BUFFERS = false;
			
			int trianglesDrawn = 0;
			
			short indexOffset = 0;
			
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
				cubeBuffer = Chunk.tempCubeBuffer;
				indicesBuffer = Chunk.tempIndicesBuffer;
				
				cubeBuffer.clear();
				indicesBuffer.clear();
			}
			
			for(BlockInfo block : blocks) {
				if(block == null || isSurrounded(block))
					continue;
				
				posTemp.set(block.getWorldX(), block.getWorldY(), -block.getWorldZ()).mult(CUBE_SIZE);
				
				viewMatrix.mult(posTemp, cameraPosTemp);
				cameraPosTemp.normalize();
				
				for(int a = 0; a < normals.length; a++) {
					Vector3 norm = normals[a];
					
					normalMatrix.mult(norm, normalTemp);
					
					normalTemp.normalize();
					
					float dot = normalTemp.dot(cameraPosTemp);
					
					if(dot < 0f) {
						for(int b = a * 4; b < a * 4 + 4; b++) {
							cubeTemp.set(unitCube[b]).mult(CUBE_SIZE);
							cubeTemp.add(block.getWorldX() * CUBE_SIZE, block.getWorldY() * CUBE_SIZE, -block.getWorldZ() * CUBE_SIZE);
							
							try {
								cubeBuffer.put(cubeTemp.toBuffer());
								cubeBuffer.put(norm.toBuffer());
							} catch(Exception exc) {
								System.out.println(cubeBuffer.position() + " " + cubeBuffer.capacity());
								exc.printStackTrace();
								System.exit(0);
							}
						}
						
						for(int b = 0; b < indices.length; b++) {
							indicesBuffer.put((short)(indices[b] + indexOffset));
						}
						
						indexOffset += 4;
						trianglesDrawn += 2;
					}
				}
			}
			
			if(USE_MAPPED_BUFFERS) {
				glUnmapBuffer(GL_ARRAY_BUFFER);
				glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);
			}
			else {
				cubeBuffer.flip();
				indicesBuffer.flip();

				tempDirectCubeBuffer.clear();
				tempDirectCubeBuffer.put(cubeBuffer);
	tempDirectCubeBuffer.flip();
				
				tempDirectIndicesBuffer.clear();
				tempDirectIndicesBuffer.put(indicesBuffer);
				tempDirectIndicesBuffer.flip();
				
				glBufferSubData(GL_ARRAY_BUFFER, 0, tempDirectCubeBuffer);
				glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, tempDirectIndicesBuffer);
			}
			
			glBindBuffer(GL_ARRAY_BUFFER, 0);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
			
			lastTriangleRenderCount = trianglesDrawn;
			
			if(lastTriangleRenderCount == 0) {
				return;
			}
			
			GLUtils.get().glBindVertexArray(chunkVAO);
			glDrawElements(GL_TRIANGLES, lastTriangleRenderCount * 3, GL_UNSIGNED_SHORT, 0);
			GLUtils.get().glBindVertexArray(0);
		}
	}
}
