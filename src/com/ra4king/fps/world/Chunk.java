package com.ra4king.fps.world;

/**
 * @author Roi Atalla
 */
public class Chunk {
	public static final int CHUNK_CUBE_WIDTH = 16, CHUNK_CUBE_HEIGHT = 16, CHUNK_CUBE_DEPTH = 16;
	public static final float CUBE_SIZE = 2;
	public static final float SPACING = CUBE_SIZE; // cannot be less than CUBE_SIZE
	
	// z * width * height + y * width + x
	private final BlockInfo[] blocks;
	private final ChunkInfo chunkInfo;
	private ChunkManager manager;
	private int cubeCount;
	
	private boolean hasChanged = true;
	
	public Chunk(ChunkManager manager, ChunkInfo chunkInfo, boolean random) {
		this.manager = manager;
		
		this.chunkInfo = chunkInfo;
		
		blocks = new BlockInfo[CHUNK_CUBE_WIDTH * CHUNK_CUBE_HEIGHT * CHUNK_CUBE_DEPTH];
		
		if(random)
			initializeRandomly();
		else
			initializeAll();
	}
	
	public ChunkManager getChunkManager() {
		return manager;
	}
	
	private int posToArrayIndex(int x, int y, int z) {
		return z * CHUNK_CUBE_WIDTH * CHUNK_CUBE_HEIGHT + y * CHUNK_CUBE_WIDTH + x;
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
				
				int rem = ix % (CHUNK_CUBE_WIDTH * CHUNK_CUBE_HEIGHT);
				int x = rem % CHUNK_CUBE_WIDTH;
				int y = rem / CHUNK_CUBE_WIDTH;
				int z = ix / (CHUNK_CUBE_WIDTH * CHUNK_CUBE_HEIGHT);
				
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
				
				int rem = ix % (CHUNK_CUBE_WIDTH * CHUNK_CUBE_HEIGHT);
				int x = rem % CHUNK_CUBE_WIDTH;
				int y = rem / CHUNK_CUBE_WIDTH;
				int z = ix / (CHUNK_CUBE_WIDTH * CHUNK_CUBE_HEIGHT);
				
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
	
	public boolean isValidPos(int x, int y, int z) {
		return !(x < 0 || x >= CHUNK_CUBE_WIDTH || y < 0 || y >= CHUNK_CUBE_HEIGHT || z < 0 || z >= CHUNK_CUBE_DEPTH);
	}
	
	public BlockInfo get(int x, int y, int z) {
		if(!isValidPos(x, y, z))
			return null;
		
		return blocks[posToArrayIndex(x, y, z)];
	}
	
	public BlockInfo[] getBlocks() {
		return blocks;
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
		
		hasChanged = true;
	}
	
	public boolean remove(int x, int y, int z) {
		if(!isValidPos(x, y, z))
			throw new IllegalArgumentException("Invalid block position.");
		
		int i = posToArrayIndex(x, y, z);
		BlockInfo block = blocks[i];
		blocks[i] = null;
		
		if(block != null) {
			cubeCount--;
			hasChanged = true;
		}
		
		return block != null;
	}
	
	public boolean hasChanged() {
		boolean changed = hasChanged;
		hasChanged = false;
		return changed;
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
			return cornerEquals((x / CHUNK_CUBE_WIDTH) * CHUNK_CUBE_WIDTH, (y / CHUNK_CUBE_HEIGHT) * CHUNK_CUBE_HEIGHT, (z / CHUNK_CUBE_DEPTH) * CHUNK_CUBE_DEPTH);
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
}
