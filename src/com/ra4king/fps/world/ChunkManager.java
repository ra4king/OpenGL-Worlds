package com.ra4king.fps.world;

import com.ra4king.fps.world.Chunk.Block;
import com.ra4king.fps.world.Chunk.BlockType;
import com.ra4king.opengl.util.math.Vector3;

import net.indiespot.struct.cp.Struct;
import net.indiespot.struct.cp.TakeStruct;

/**
 * @author Roi Atalla
 */
public class ChunkManager {
	public static final int CHUNKS_SIDE_X = 5, CHUNKS_SIDE_Y = 5, CHUNKS_SIDE_Z = 5;
	
	// z * CHUNKS_SIDE * CHUNKS_SIDE + y * CHUNKS_SIDE + x
	private Chunk[] chunks;
	
	public ChunkManager() {
		chunks = new Chunk[CHUNKS_SIDE_X * CHUNKS_SIDE_Y * CHUNKS_SIDE_Z];
		
		long t0 = System.nanoTime();
		for(int x = 0; x < CHUNKS_SIDE_X; x++) {
			for(int y = 0; y < CHUNKS_SIDE_Y; y++) {
				for(int z = 0; z < CHUNKS_SIDE_Z; z++) {
					chunks[posToArrayIndex(x, y, z)] = new Chunk(this, x * Chunk.CHUNK_CUBE_WIDTH, y * Chunk.CHUNK_CUBE_HEIGHT, z * Chunk.CHUNK_CUBE_DEPTH);
				}
			}
		}
		long time = System.nanoTime() - t0;
		System.out.printf("Chunks created in %.3f ms\n", time / 1e6);
	}
	
	public void clearAll() {
		for(Chunk c : chunks)
			c.clearAll();
	}
	
	private int posToArrayIndex(int x, int y, int z) {
		return z * CHUNKS_SIDE_X * CHUNKS_SIDE_Y + y * CHUNKS_SIDE_X + x;
	}
	
	private int cubePosToArrayIndex(int x, int y, int z) {
		int ix = x / Chunk.CHUNK_CUBE_WIDTH;
		int iy = y / Chunk.CHUNK_CUBE_HEIGHT;
		int iz = z / Chunk.CHUNK_CUBE_DEPTH;
		
		if(ix < 0 || ix >= CHUNKS_SIDE_X ||
				iy < 0 || iy >= CHUNKS_SIDE_Y ||
				iz < 0 || iz >= CHUNKS_SIDE_Z)
			return -1;
		
		return posToArrayIndex(ix, iy, iz);
	}
	
	public Chunk[] getChunks() {
		return chunks;
	}
	
	private final Vector3 temp = new Vector3();
	
	@TakeStruct
	public Block getBlock(Vector3 v, float radius) {
		int px = Math.round(v.x() / Chunk.SPACING);
		int py = Math.round(v.y() / Chunk.SPACING);
		int pz = Math.round(-v.z() / Chunk.SPACING);
		
		// if(px < -1 || px > CHUNKS_SIDE_X * Chunk.CHUNK_CUBE_WIDTH ||
		// py < -1 || py > CHUNKS_SIDE_Y * Chunk.CHUNK_CUBE_HEIGHT ||
		// pz < -1 || pz > CHUNKS_SIDE_Z * Chunk.CHUNK_CUBE_DEPTH)
		// return Struct.typedNull(Block.class);
		
		float lowestDistance = Float.MAX_VALUE;
		Block closestBlock = Struct.typedNull(Block.class);
		
		final float halfSpacing = Chunk.SPACING * 0.5f;
		
		final int count = (int)Math.ceil(radius / Chunk.SPACING);
		
		for(int a = -count; a <= count; a++) {
			for(int b = -count; b <= count; b++) {
				for(int c = -count; c <= count; c++) {
					Block block = getBlock(px + a, py + b, pz + c);
					
					if(block == null || block.getType() == BlockType.AIR)
						continue;
					
					float len = temp.set(px + a, py + b, -(pz + c)).mult(Chunk.SPACING).add(halfSpacing, halfSpacing, -halfSpacing).sub(v).lengthSquared();
					
					if(len < lowestDistance) {
						lowestDistance = len;
						closestBlock = block;
					}
				}
			}
		}
		
		final float d = Chunk.CUBE_SIZE * 0.5f + radius;
		
		return lowestDistance <= d * d ? closestBlock : Struct.typedNull(Block.class);
	}
	
	@TakeStruct
	public Block[] getBlocks(Vector3 v, float radius) {
		int px = Math.round(v.x() / Chunk.SPACING);
		int py = Math.round(v.y() / Chunk.SPACING);
		int pz = Math.round(-v.z() / Chunk.SPACING);
		
		// if(px < -1 || px > CHUNKS_SIDE_X * Chunk.CHUNK_CUBE_WIDTH ||
		// py < -1 || py > CHUNKS_SIDE_Y * Chunk.CHUNK_CUBE_HEIGHT ||
		// pz < -1 || pz > CHUNKS_SIDE_Z * Chunk.CHUNK_CUBE_DEPTH)
		// return Struct.emptyArray(Block.class, 0);
		
		final float halfSpacing = Chunk.SPACING * 0.5f;
		
		final int count = (int)Math.ceil(radius / Chunk.SPACING);
		
		float dist = Chunk.CUBE_SIZE * 0.5f + radius;
		dist *= dist;
		
		Block[] blocks = Struct.emptyArray(Block.class, 100);
		int size = 0;
		
		for(int a = -count; a <= count; a++) {
			for(int b = -count; b <= count; b++) {
				for(int c = -count; c <= count; c++) {
					Block block = getBlock(px + a, py + b, pz + c);
					
					if(block == null || block.getType() == BlockType.AIR)
						continue;
					
					float len = temp.set(px + a, py + b, -(pz + c)).mult(Chunk.SPACING).add(halfSpacing, halfSpacing, -halfSpacing).sub(v).lengthSquared();
					
					if(len <= dist) {
						if(size >= blocks.length) {
							Block[] temp = Struct.emptyArray(Block.class, blocks.length * 2);
							System.arraycopy(blocks, 0, temp, 0, blocks.length);
							blocks = temp;
						}
						
						blocks[size++] = block;
					}
				}
			}
		}
		
		if(size != blocks.length) {
			Block[] temp = Struct.emptyArray(Block.class, size);
			System.arraycopy(blocks, 0, temp, 0, size);
			blocks = temp;
		}
		
		return blocks;
	}
	
	@TakeStruct
	public Block getBlock(int x, int y, int z) {
		int i = cubePosToArrayIndex(x, y, z);
		if(i == -1)
			return Struct.typedNull(Block.class);
		
		Chunk chunk = chunks[i];
		return chunk.get(x % Chunk.CHUNK_CUBE_WIDTH, y % Chunk.CHUNK_CUBE_HEIGHT, z % Chunk.CHUNK_CUBE_DEPTH);
	}
	
	public void setBlock(BlockType type, Block block) {
		setBlock(type, block.getX(), block.getY(), block.getZ());
	}
	
	public void setBlock(BlockType type, int x, int y, int z) {
		int i = cubePosToArrayIndex(x, y, z);
		if(i == -1)
			throw new IllegalArgumentException("Invalid cube position (" + x + "," + y + "," + z + ").");
		
		Chunk chunk = chunks[i];
		chunk.set(type, x % Chunk.CHUNK_CUBE_WIDTH, y % Chunk.CHUNK_CUBE_HEIGHT, z % Chunk.CHUNK_CUBE_DEPTH);
	}
	
	public void update(long deltaTime) {
		
	}
}
