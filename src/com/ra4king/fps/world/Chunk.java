package com.ra4king.fps.world;

import net.indiespot.struct.cp.Struct;
import net.indiespot.struct.cp.StructField;
import net.indiespot.struct.cp.StructType;
import net.indiespot.struct.cp.TakeStruct;

/**
 * @author Roi Atalla
 */
public class Chunk {
	public static final int CHUNK_BLOCK_WIDTH = 32, CHUNK_BLOCK_HEIGHT = 32, CHUNK_BLOCK_DEPTH = 32;
	public static final int TOTAL_BLOCKS = Chunk.CHUNK_BLOCK_WIDTH * Chunk.CHUNK_BLOCK_HEIGHT * Chunk.CHUNK_BLOCK_DEPTH;
	
	public static final float BLOCK_SIZE = 2;
	public static final float SPACING = BLOCK_SIZE; // cannot be less than BLOCK_SIZE
	
	private ChunkModifiedCallback callback;
	
	private int cornerX, cornerY, cornerZ; // block indices
			
	// z * width * height + y * width + x
	private final Block[] blocks; // structured array
	
	private int blockCount;
	
	private ChunkManager manager;
	
	public Chunk(ChunkManager manager, int cornerX, int cornerY, int cornerZ) {
		this.manager = manager;
		
		this.cornerX = cornerX;
		this.cornerY = cornerY;
		this.cornerZ = cornerZ;
		
		blocks = Struct.emptyArray(Block.class, CHUNK_BLOCK_WIDTH * CHUNK_BLOCK_HEIGHT * CHUNK_BLOCK_DEPTH);
	}
	
	private void setupBlocks() {
		blockCount = blocks.length;
		
		for(int i = 0; i < blockCount; i++) {
			int rem = i % (CHUNK_BLOCK_WIDTH * CHUNK_BLOCK_HEIGHT);
			int x = rem % CHUNK_BLOCK_WIDTH;
			int y = rem / CHUNK_BLOCK_WIDTH;
			int z = i / (CHUNK_BLOCK_WIDTH * CHUNK_BLOCK_HEIGHT);
			
			blocks[i] = callback.chunkInit(x, y, z, BlockType.AIR);
		}
	}
	
	public void setCallback(ChunkModifiedCallback callback) {
		this.callback = callback;
		setupBlocks();
	}
	
	public ChunkModifiedCallback getCallback() {
		return callback;
	}
	
	public ChunkManager getChunkManager() {
		return manager;
	}
	
	public int getCornerX() {
		return cornerX;
	}
	
	public int getCornerY() {
		return cornerY;
	}
	
	public int getCornerZ() {
		return cornerZ;
	}
	
	public boolean cornerEquals(int cornerX, int cornerY, int cornerZ) {
		return this.cornerX == cornerX && this.cornerY == cornerY && this.cornerZ == cornerZ;
	}
	
	public boolean containsBlock(int x, int y, int z) {
		return x >= cornerX && x < cornerX + CHUNK_BLOCK_WIDTH &&
				y >= cornerY && y < cornerY + CHUNK_BLOCK_HEIGHT &&
				z >= cornerZ && z < cornerZ + CHUNK_BLOCK_DEPTH;
	}
	
	public boolean containsBlock(Block block) {
		return containsBlock(block.getX(), block.getY(), block.getZ());
	}
	
	public int posToArrayIndex(int x, int y, int z) {
		if(!containsBlock(x, y, z))
			throw new IllegalArgumentException("Invalid coords.");
		
		x %= CHUNK_BLOCK_WIDTH;
		y %= CHUNK_BLOCK_HEIGHT;
		z %= CHUNK_BLOCK_DEPTH;
		
		return z * CHUNK_BLOCK_WIDTH * CHUNK_BLOCK_HEIGHT + y * CHUNK_BLOCK_WIDTH + x;
	}
	
	public int posToArrayIndex(Block block) {
		return posToArrayIndex(block.getX(), block.getY(), block.getZ());
	}
	
	public int getBlockCount() {
		return blockCount;
	}
	
	public boolean isValidPos(int x, int y, int z) {
		return x >= cornerX && x < cornerX + CHUNK_BLOCK_WIDTH && y >= cornerY && y < cornerY + CHUNK_BLOCK_HEIGHT && z >= cornerZ && z < cornerZ + CHUNK_BLOCK_DEPTH;
	}
	
	@TakeStruct
	public Block get(int x, int y, int z) {
		if(!isValidPos(x, y, z)) {
			return Struct.typedNull(Block.class);
		}
		
		return blocks[posToArrayIndex(x, y, z)];
	}
	
	@TakeStruct
	public Block[] getNeighbors(int x, int y, int z) {
		Block[] neighbors = new Block[6];
		int idx = 0;
		
		for(int a = -1; a < 2; a++) {
			if(a == 0)
				continue;
			
			Block block = get(x + a, y, z);
			if(block != Struct.typedNull(Block.class)) {
				neighbors[idx++] = block;
			}
		}
		
		for(int b = -1; b < 2; b++) {
			if(b == 0)
				continue;
			
			Block block = get(x, y + b, z);
			if(block != Struct.typedNull(Block.class)) {
				neighbors[idx++] = block;
			}
		}
		
		for(int c = -1; c < 2; c++) {
			if(c == 0)
				continue;
			
			Block block = get(x, y, z + c);
			if(block != Struct.typedNull(Block.class)) {
				neighbors[idx++] = block;
			}
		}
		
		return neighbors;
	}
	
	@TakeStruct
	public Block[] getNeighbors(Block b) {
		return getNeighbors(b.getX() % Chunk.CHUNK_BLOCK_WIDTH, b.getY() % Chunk.CHUNK_BLOCK_HEIGHT, b.getZ() % Chunk.CHUNK_BLOCK_DEPTH);
	}
	
	public Block[] getBlocks() {
		return blocks;
	}
	
	public void set(BlockType blockType, int x, int y, int z) {
		if(!isValidPos(x, y, z)) {
			throw new IllegalArgumentException("Invalid block position.");
		}
		
		int i = posToArrayIndex(x, y, z);
		
		Block block = blocks[i];
		
		if(block.getType() == blockType) {
			return;
		}
		
		if(block == Struct.typedNull(Block.class)) {
			// blocks[i] = callback.chunkInit(x, y, z, blockType);
			// All blocks must be initialized, this shouldn't happen
			throw new IllegalStateException(String.format("Block at (%d,%d,%d) is null!", x, y, z));
		}
		
		if(blockType != BlockType.AIR) {
			if(block.getType() == BlockType.AIR) {
				blockCount++; // Air -> Not Air
			}
			
			block.setType(blockType);
			callback.chunkModified(block);
		} else if(blockType != block.getType()) { // Not Air -> Air
			block.setType(blockType);
			callback.chunkRemoved(block);
			blockCount--;
		}
	}
	
	public void clearAll() {
		for(int x = 0; x < CHUNK_BLOCK_WIDTH; x++) {
			for(int y = 0; y < CHUNK_BLOCK_HEIGHT; y++) {
				for(int z = 0; z < CHUNK_BLOCK_DEPTH; z++) {
					set(BlockType.AIR, cornerX + x, cornerY + y, cornerZ + z);
				}
			}
		}
	}
	
	public static interface ChunkModifiedCallback {
		Block chunkInit(int x, int y, int z, BlockType block);
		
		void chunkRemoved(Block block);
		
		void chunkModified(Block block);
	}
	
	public static enum BlockType {
		AIR, SOLID;
		
		public static BlockType[] values = values();
	}
	
	@StructType(sizeof = 16)
	public static class Block {
		@StructField(offset = 0)
		private int x;
		@StructField(offset = 4)
		private int y;
		@StructField(offset = 8)
		private int z;
		@StructField(offset = 12)
		private int type;
		
		@TakeStruct
		public Block init(Chunk chunk, int x, int y, int z, BlockType type) {
			this.x = chunk.cornerX + x;
			this.y = chunk.cornerY + y;
			this.z = chunk.cornerZ + z;
			this.type = type.ordinal();
			
			return this;
		}
		
		public int getX() {
			return x;
		}
		
		public int getY() {
			return y;
		}
		
		public int getZ() {
			return z;
		}
		
		public BlockType getType() {
			return BlockType.values[type];
		}
		
		public void setType(BlockType type) {
			this.type = type.ordinal();
		}
		
		public boolean equals(Block b) {
			return this.x == b.x && this.y == b.y && this.z == b.z;
		}
		
		public boolean isSurrounded(Chunk chunk) {
			Block up = chunk.getChunkManager().getBlock(x, y + 1, z);
			Block down = chunk.getChunkManager().getBlock(x, y - 1, z);
			Block left = chunk.getChunkManager().getBlock(x - 1, y, z);
			Block right = chunk.getChunkManager().getBlock(x + 1, y, z);
			Block front = chunk.getChunkManager().getBlock(x, y, z - 1);
			Block back = chunk.getChunkManager().getBlock(x, y, z + 1);
			
			int air = BlockType.AIR.ordinal();
			
			return up != null && up.type != air &&
					down != null && down.type != air &&
					left != null && left.type != air &&
					right != null && right.type != air &&
					front != null && front.type != air &&
					back != null && back.type != air;
		}
	}
}
