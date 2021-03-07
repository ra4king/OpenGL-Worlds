package com.ra4king.fps.actors;

import java.nio.ByteBuffer;

import com.ra4king.fps.world.Chunk;

public class Block {
	public enum BlockType {
		AIR, SOLID;

		public static BlockType[] values = values();
	}

	// Size in bytes
	public static final int SIZE = 16;

	private int x;
	private int y;
	private int z;
	private int type;

	public Block(Chunk chunk, int x, int y, int z, BlockType type) {
		this.x = chunk.getCornerX() + x;
		this.y = chunk.getCornerY() + y;
		this.z = chunk.getCornerZ() + z;
		this.type = type.ordinal();
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

		return up != null && up.type != air && down != null && down.type != air && left != null && left.type != air &&
		       right != null && right.type != air && front != null && front.type != air && back != null &&
		       back.type != air;
	}

	public void serialize(ByteBuffer buffer) {
		buffer.putInt(x).putInt(y).putInt(z).putInt(type);
	}
}
