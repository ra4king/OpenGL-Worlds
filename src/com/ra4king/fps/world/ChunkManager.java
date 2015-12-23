package com.ra4king.fps.world;

import com.ra4king.fps.actors.Block;
import com.ra4king.fps.actors.Block.BlockType;
import com.ra4king.opengl.util.math.Vector3;

import net.indiespot.struct.cp.Struct;
import net.indiespot.struct.cp.TakeStruct;

/**
 * @author Roi Atalla
 */
public class ChunkManager {
	public final int CHUNKS_SIDE_X, CHUNKS_SIDE_Y, CHUNKS_SIDE_Z;
	
	private World world;
	
	// z * CHUNKS_SIDE_X * CHUNKS_SIDE_Y + y * CHUNKS_SIDE_X + x
	private Chunk[] chunks;
	
	public ChunkManager(World world, int chunksX, int chunksY, int chunksZ) {
		this.world = world;
		
		this.CHUNKS_SIDE_X = chunksX;
		this.CHUNKS_SIDE_Y = chunksY;
		this.CHUNKS_SIDE_Z = chunksZ;
		
		chunks = new Chunk[CHUNKS_SIDE_X * CHUNKS_SIDE_Y * CHUNKS_SIDE_Z];
		
		long t0 = System.nanoTime();
		for(int x = 0; x < CHUNKS_SIDE_X; x++) {
			for(int y = 0; y < CHUNKS_SIDE_Y; y++) {
				for(int z = 0; z < CHUNKS_SIDE_Z; z++) {
					chunks[posToArrayIndex(x, y, z)] = new Chunk(this, x * Chunk.CHUNK_BLOCK_WIDTH, y * Chunk.CHUNK_BLOCK_HEIGHT, z * Chunk.CHUNK_BLOCK_DEPTH);
				}
			}
		}
		long time = System.nanoTime() - t0;
		System.out.printf("Chunks created in %.3f ms\n", time / 1e6);
	}
	
	public World getWorld() {
		return world;
	}
	
	public void clearAll() {
		for(Chunk c : chunks)
			c.clearAll();
	}
	
	private int posToArrayIndex(int x, int y, int z) {
		if(x < 0 || x >= CHUNKS_SIDE_X ||
				y < 0 || y >= CHUNKS_SIDE_Y ||
				z < 0 || z >= CHUNKS_SIDE_Z)
			return -1;
		
		return z * CHUNKS_SIDE_X * CHUNKS_SIDE_Y + y * CHUNKS_SIDE_X + x;
	}
	
	private int blockPosToArrayIndex(int x, int y, int z) {
		int ix = x / Chunk.CHUNK_BLOCK_WIDTH;
		int iy = y / Chunk.CHUNK_BLOCK_HEIGHT;
		int iz = z / Chunk.CHUNK_BLOCK_DEPTH;
		
		return posToArrayIndex(ix, iy, iz);
	}
	
	public Chunk[] getChunks() {
		return chunks;
	}
	
	/**
	 * In Chunk positions.
	 */
	public Chunk getChunk(int x, int y, int z) {
		int pos = posToArrayIndex(x, y, z);
		if(pos < 0 || pos >= chunks.length)
			return null;
		return chunks[pos];
	}
	
	/**
	 * In world block position.
	 */
	public Chunk getChunkContaining(int x, int y, int z) {
		int pos = blockPosToArrayIndex(x, y, z);
		if(pos == -1)
			return null;
		return chunks[pos];
	}
	
	public Chunk getChunkContaining(Block block) {
		return getChunkContaining(block.getX(), block.getY(), block.getZ());
	}
	
	@TakeStruct
	public Block getBlock(Vector3 v, float radius) {
		int px = Math.round(v.x() / Chunk.SPACING);
		int py = Math.round(v.y() / Chunk.SPACING);
		int pz = Math.round(-v.z() / Chunk.SPACING);
		
		float lowestDistance = Float.MAX_VALUE;
		Block closestBlock = Struct.nullStruct(Block.class);
		
		final int count = (int)Math.ceil(radius / Chunk.SPACING);
		
		for(int a = -count; a <= count; a++) {
			for(int b = -count; b <= count; b++) {
				for(int c = -count; c <= count; c++) {
					Block block = getBlock(px + a, py + b, pz + c);
					
					if(block == null || block.getType() == BlockType.AIR)
						continue;
					
					float len = new Vector3(px + a, py + b, -(pz + c)).mult(Chunk.SPACING).sub(v).lengthSquared();
					
					if(len < lowestDistance) {
						lowestDistance = len;
						closestBlock = block;
					}
				}
			}
		}
		
		final float d = Chunk.BLOCK_SIZE * 0.5f + radius;
		
		return lowestDistance <= d * d ? closestBlock : Struct.nullStruct(Block.class);
	}
	
	public Block[] getBlocks(Vector3 v, float radius) {
		// Get approximate index (x,y,z) coordinate
		int px = Math.round(v.x() / Chunk.SPACING);
		int py = Math.round(v.y() / Chunk.SPACING);
		int pz = Math.round(-v.z() / Chunk.SPACING);
		
		// How many cubes fit in radius
		final int count = (int)Math.ceil(radius / Chunk.SPACING);
		
		// (half cube + radius)^s = distSqr^2
		float distSqr = Chunk.BLOCK_SIZE * 0.5f + radius;
		distSqr *= distSqr;
		
		Block[] blocks = Struct.nullArray(Block.class, 300);
		int size = 0;
		
		// Test against -count.xyz to +count.xyz offset from the p.xyz index
		for(int a = -count; a <= count; a++) {
			for(int b = -count; b <= count; b++) {
				for(int c = -count; c <= count; c++) {
					Block block = getBlock(px + a, py + b, pz + c);
					
					if(block == null || block.getType() == BlockType.AIR)
						continue;
					
					// Length squared of ((px,py,pz)+(a,b,c) - v)
					float lenSqr = new Vector3(px + a, py + b, -(pz + c)).mult(Chunk.SPACING).sub(v).lengthSquared();
					
					if(lenSqr <= distSqr) {
						if(size >= blocks.length) {
							Block[] temp = Struct.nullArray(Block.class, blocks.length * 2);
							System.arraycopy(blocks, 0, temp, 0, blocks.length);
							blocks = temp;
						}
						
						blocks[size++] = block;
					}
				}
			}
		}
		
		if(size != blocks.length) {
			Block[] temp = Struct.nullArray(Block.class, size);
			System.arraycopy(blocks, 0, temp, 0, size);
			blocks = temp;
		}
		
		return blocks;
	}
	
	@TakeStruct
	public Block getBlock(int x, int y, int z) {
		int i = blockPosToArrayIndex(x, y, z);
		if(i == -1) {
			return Struct.nullStruct(Block.class);
		}
		
		return chunks[i].get(x, y, z);
	}
	
	public void setBlock(BlockType type, Block block) {
		setBlock(type, block.getX(), block.getY(), block.getZ());
	}
	
	public void setBlock(BlockType type, int x, int y, int z) {
		int i = blockPosToArrayIndex(x, y, z);
		if(i == -1)
			throw new IllegalArgumentException("Invalid cube position (" + x + "," + y + "," + z + ").");
		
		chunks[i].set(type, x, y, z);
	}
	
	public void update(long deltaTime) {
		
	}
}
