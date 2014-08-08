package com.ra4king.fps.world;

/**
 * @author Roi Atalla
 */
public class Chunk {
	public static final int CHUNK_CUBE_WIDTH = 32, CHUNK_CUBE_HEIGHT = 32, CHUNK_CUBE_DEPTH = 32;
	public static final float CUBE_SIZE = 2;
	public static final float SPACING = CUBE_SIZE; // cannot be less than CUBE_SIZE
	
	private int cornerX, cornerY, cornerZ; // block indices

	// z * width * height + y * width + x
	private final Block[] blocks;
	// private final ChunkInfo chunkInfo;
	
	private ChunkManager manager;
	private int cubeCount;
	
	private boolean hasChanged = true;
	
	public Chunk(int cornerX, int cornerY, int cornerZ) {
		this.cornerX = cornerX;
		this.cornerY = cornerY;
		this.cornerZ = cornerZ;
		
		blocks = new Block[CHUNK_CUBE_WIDTH * CHUNK_CUBE_HEIGHT * CHUNK_CUBE_DEPTH];
	}
	
	public void setupBlocks(ChunkManager manager, boolean random) {
		this.manager = manager;
		
		if(random)
			initializeRandomly();
		else
			initializeAll();
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
		return cornerEquals((x / CHUNK_CUBE_WIDTH) * CHUNK_CUBE_WIDTH, (y / CHUNK_CUBE_HEIGHT) * CHUNK_CUBE_HEIGHT, (z / CHUNK_CUBE_DEPTH) * CHUNK_CUBE_DEPTH);
	}
	
	private int posToArrayIndex(int x, int y, int z) {
		return z * CHUNK_CUBE_WIDTH * CHUNK_CUBE_HEIGHT + y * CHUNK_CUBE_WIDTH + x;
	}
	
	public void initializeRandomly() {
		cubeCount = (int)(Math.random() * blocks.length / 10);
		
		for(int a = 0; a < cubeCount; a++) {
			int i;
			do {
				i = (int)(Math.random() * blocks.length);
			} while(blocks[i] != null && blocks[i].type != BlockType.AIR);

			int rem = i % (CHUNK_CUBE_WIDTH * CHUNK_CUBE_HEIGHT);
			int x = rem % CHUNK_CUBE_WIDTH;
			int y = rem / CHUNK_CUBE_WIDTH;
			int z = i / (CHUNK_CUBE_WIDTH * CHUNK_CUBE_HEIGHT);
			
			blocks[i] = new Block((short)x, (short)y, (short)z, BlockType.SOLID);
		}
	}
	
	public void initializeAll() {
		cubeCount = blocks.length;
		
		for(int i = 0; i < cubeCount; i++) {
			int rem = i % (CHUNK_CUBE_WIDTH * CHUNK_CUBE_HEIGHT);
			int x = rem % CHUNK_CUBE_WIDTH;
			int y = rem / CHUNK_CUBE_WIDTH;
			int z = i / (CHUNK_CUBE_WIDTH * CHUNK_CUBE_HEIGHT);
			
			blocks[i] = new Block((short)x, (short)y, (short)z, BlockType.SOLID);
		}
	}
	
	public int getCubeCount() {
		return cubeCount;
	}
	
	public boolean isValidPos(int x, int y, int z) {
		return !(x < 0 || x >= CHUNK_CUBE_WIDTH || y < 0 || y >= CHUNK_CUBE_HEIGHT || z < 0 || z >= CHUNK_CUBE_DEPTH);
	}
	
	public Block get(int x, int y, int z) {
		if(!isValidPos(x, y, z))
			return null;
		
		return blocks[posToArrayIndex(x, y, z)];
	}
	
	public Block[] getBlocks() {
		return blocks;
	}
	
	public void set(BlockType block, int x, int y, int z) {
		if(!isValidPos(x, y, z))
			throw new IllegalArgumentException("Invalid block position.");
		
		int i = posToArrayIndex(x, y, z);
		
		if(blocks[i].type == block)
			return;
		
		if(blocks[i].type == BlockType.AIR)
			cubeCount++;
		
		blocks[i].setType(block);
	}
	
	/**
	 * @return Returns and reset the hasChanged the property, which is true if a block was set or removed.
	 */
	public boolean hasChanged() {
		boolean changed = hasChanged;
		hasChanged = false;
		return changed;
	}
	
	public enum BlockType {
		AIR(0), SOLID(1);
		
		public final int order;
		
		private BlockType(int order) {
			this.order = order;
		}
	}
	
	public class Block {
		private short x, y, z;
		private BlockType type;
		
		private Block up;
		private Block down;
		private Block left;
		private Block right;
		private Block front;
		private Block back;
		
		public Block(short x, short y, short z, BlockType type) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.type = type;
		}
		
		public short getX() {
			return x;
		}
		
		public int getWorldX() {
			return cornerX + x;
		}
		
		public short getY() {
			return y;
		}
		
		public int getWorldY() {
			return cornerY + y;
		}
		
		public short getZ() {
			return z;
		}
		
		public int getWorldZ() {
			return cornerZ + z;
		}
		
		public BlockType getType() {
			return type;
		}
		
		public void setType(BlockType type) {
			this.type = type;
			
			hasChanged = true;
		}
		
		@Override
		public boolean equals(Object o) {
			return o != null && o instanceof Block && ((Block)o).type == this.type;
		}
		
		public boolean isSurrounded() {
			int x = getWorldX();
			int y = getWorldY();
			int z = getWorldZ();
			
			Block up = manager.getBlock(x, y + 1, z);
			Block down = manager.getBlock(x, y - 1, z);
			Block left = manager.getBlock(x - 1, y, z);
			Block right = manager.getBlock(x + 1, y, z);
			Block front = manager.getBlock(x, y, z - 1);
			Block back = manager.getBlock(x, y, z + 1);
			
			return up != null && up.type != BlockType.AIR &&
					down != null && down.type != BlockType.AIR &&
					left != null && left.type != BlockType.AIR &&
					right != null && right.type != BlockType.AIR &&
					front != null && front.type != BlockType.AIR &&
					back != null && back.type != BlockType.AIR;
		}
		
		public Block getUp() {
			return up;
		}
		
		public Block getDown() {
			return down;
		}
		
		public Block getLeft() {
			return left;
		}
		
		public Block getRight() {
			return right;
		}
		
		public Block getFront() {
			return front;
		}
		
		public Block getBack() {
			return back;
		}
	}
}
