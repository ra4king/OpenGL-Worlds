package com.ra4king.fps.world;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL31.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

/**
 * @author ra4king
 */
public class Chunk {
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
			return cornerEquals((x / CUBES_SIDE) * CUBES_SIDE, (y / CUBES_SIDE) * CUBES_SIDE, (z / CUBES_SIDE) * CUBES_SIDE);
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
	
	private ChunkManager manager;
	
	private final BlockInfo[] blocks;
	private final ChunkInfo chunkInfo;
	
	private boolean modified = true;
	
	private ChunkRenderer renderer;
	
	private int cubeCount;
	
	public static final int CUBES_SIDE = 16;// GLUtils.get().VERSION >= 33 ? 16 : 8;
	public static final float CUBE_SIZE = 2;
	public static final float SPACING = CUBE_SIZE; // cannot be less than CUBE_SIZE
	
	public Chunk(ChunkManager manager, ChunkInfo chunkInfo, boolean random) {
		this.manager = manager;
		
		this.chunkInfo = chunkInfo;
		
		blocks = new BlockInfo[CUBES_SIDE * CUBES_SIDE * CUBES_SIDE];

		if(random)
			initializeRandomly();
		else
			initializeAll();
		
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
				
				int x = ix / (CUBES_SIDE * CUBES_SIDE);
				int y = (ix % (CUBES_SIDE * CUBES_SIDE)) / CUBES_SIDE;
				int z = ix % CUBES_SIDE;
				
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
				
				int x = ix / (CUBES_SIDE * CUBES_SIDE);
				int y = (ix % (CUBES_SIDE * CUBES_SIDE)) / CUBES_SIDE;
				int z = ix % CUBES_SIDE;
				
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
	
	public BlockInfo get(int x, int y, int z) {
		if(x < 0 || x >= CUBES_SIDE || y < 0 || y >= CUBES_SIDE || z < 0 || z >= CUBES_SIDE)
			return null;
		
		return blocks[x * CUBES_SIDE * CUBES_SIDE + y * CUBES_SIDE + z];
	}
	
	public void set(BlockType block, int x, int y, int z) {
		if(x >= CUBES_SIDE || x < 0 ||
				y >= CUBES_SIDE || y < 0 ||
				z >= CUBES_SIDE || z < 0)
			throw new IllegalArgumentException("Invalid block position.");
		
		BlockInfo blockInfo = blocks[x * CUBES_SIDE * CUBES_SIDE + y * CUBES_SIDE + z];
		if(blockInfo == null) {
			blocks[x * CUBES_SIDE * CUBES_SIDE + y * CUBES_SIDE + z] = new BlockInfo(chunkInfo, x, y, z, block);
			cubeCount++;
		}
		else
			blockInfo.type = block;
		
		modified = true;
	}
	
	public boolean remove(int x, int y, int z) {
		if(x >= CUBES_SIDE || x < 0 ||
				y >= CUBES_SIDE || y < 0 ||
				z >= CUBES_SIDE || z < 0)
			throw new IllegalArgumentException("Invalid block position.");
		
		BlockInfo block = blocks[x * CUBES_SIDE * CUBES_SIDE + y * CUBES_SIDE + z];
		blocks[x * CUBES_SIDE * CUBES_SIDE + y * CUBES_SIDE + z] = null;
		
		if(block != null) {
			cubeCount--;
			modified = true;
		}
		
		return block != null;
	}
	
	public long getCubesRendered() {
		long r = renderer.cubesRendered;
		renderer.cubesRendered = 0;
		return r;
	}
	
	public void render() {
		renderer.render();
	}
	
	private static final FloatBuffer tempCubeBuffer = BufferUtils.createFloatBuffer(CUBES_SIDE * CUBES_SIDE * CUBES_SIDE * 4);
	
	private class ChunkRenderer {
		private int dataVBO;
		
		private int lastRenderCount;
		
		public ChunkRenderer() {
			dataVBO = glGenBuffers();
		}
		
		private boolean isSurrounded(int x, int y, int z) {
			for(int ix = -1; ix < 2; ix++) {
				for(int iy = -1; iy < 2; iy++) {
					for(int iz = -1; iz < 2; iz++) {
						if(ix == 0 && iy == 0 && iz == 0)
							continue;
						
						boolean blocked;
						
						if(x + ix < 0 || x + ix >= CUBES_SIDE ||
								y + iy < 0 || y + iy >= CUBES_SIDE ||
								z + iz < 0 || z + iz >= CUBES_SIDE)
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
		
		private int updateVBO() {
			if(!modified)
				return lastRenderCount;
			
			modified = false;
			
			int cubesToDraw = 0;
			
			tempCubeBuffer.clear();
			
			for(int x = 0; x < CUBES_SIDE; x++) {
				for(int y = 0; y < CUBES_SIDE; y++) {
					for(int z = 0; z < CUBES_SIDE; z++) {
						BlockInfo block;
						if((block = get(x, y, z)) == null || isSurrounded(x, y, z))
							continue;
						
						cubesToDraw++;
						
						float size = x % CUBES_SIDE == 0 ||
								y % CUBES_SIDE == 0 ||
								z % CUBES_SIDE == 0 ? CUBE_SIZE / 2 : CUBE_SIZE;
						
						tempCubeBuffer.put((block.chunkInfo.chunkCornerX + x) * SPACING + size / 2)
								.put((block.chunkInfo.chunkCornerY + y) * SPACING + size / 2)
								.put(-((block.chunkInfo.chunkCornerZ + z) * SPACING + size / 2))
								.put(size / 2);
	}
				}
			}
			
			if(cubesToDraw == 0)
				return 0;
			
			tempCubeBuffer.flip();
			
			glBindBuffer(GL_ARRAY_BUFFER, dataVBO);
			
			if(cubesToDraw > lastRenderCount)
				glBufferData(GL_ARRAY_BUFFER, tempCubeBuffer, GL_STREAM_DRAW);
			else
				glBufferSubData(GL_ARRAY_BUFFER, 0, tempCubeBuffer);
			
			glBindBuffer(GL_ARRAY_BUFFER, 0);
			
			return lastRenderCount = cubesToDraw;
		}
		
		private long cubesRendered;
		
		public void render() {
			glBindBuffer(GL_ARRAY_BUFFER, dataVBO);
			glVertexAttribPointer(0, 3, GL_FLOAT, false, 4 * 4, 0);
			glVertexAttribPointer(1, 1, GL_FLOAT, false, 4 * 4, 3 * 4);
			
			glDrawArraysInstanced(GL_TRIANGLES, 0, 36, updateVBO());
			
			cubesRendered += lastRenderCount;
		}
	}
}
