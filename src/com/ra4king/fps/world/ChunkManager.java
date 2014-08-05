package com.ra4king.fps.world;

import com.ra4king.fps.world.Chunk.Block;
import com.ra4king.fps.world.Chunk.BlockType;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author Roi Atalla
 */
public class ChunkManager {
	public static final int CHUNKS_SIDE = 3;
	
	// z * CHUNKS_SIDE * CHUNKS_SIDE + y * CHUNKS_SIDE + x
	private Chunk[] chunks;

	public ChunkManager(boolean random) {
		chunks = new Chunk[CHUNKS_SIDE * CHUNKS_SIDE * CHUNKS_SIDE];
	
		int totalCubes = 0;
		
		long before = System.nanoTime();
		
		for(int x = 0; x < CHUNKS_SIDE; x++) {
			for(int y = 0; y < CHUNKS_SIDE; y++) {
		for(int z = 0; z < CHUNKS_SIDE; z++) {
					chunks[z * CHUNKS_SIDE * CHUNKS_SIDE + y * CHUNKS_SIDE + x] = new Chunk(x * Chunk.CHUNK_CUBE_WIDTH, y * Chunk.CHUNK_CUBE_HEIGHT, z * Chunk.CHUNK_CUBE_DEPTH);
				}
			}
		}
		
		for(Chunk chunk : chunks) {
			chunk.setupBlocks(this, random);
			totalCubes += chunk.getCubeCount();
		}
		
		long after = System.nanoTime();
		
		System.out.printf("Total cubes %d generated in %.3f ms\n", totalCubes, (after - before) / 1e6);
	}
	
	private int posToArrayIndex(int x, int y, int z) {
		return z * CHUNKS_SIDE * CHUNKS_SIDE + y * CHUNKS_SIDE + x;
	}

	private int cubePosToArrayIndex(int x, int y, int z) {
		int ix = x / Chunk.CHUNK_CUBE_WIDTH;
		int iy = y / Chunk.CHUNK_CUBE_HEIGHT;
		int iz = z / Chunk.CHUNK_CUBE_DEPTH;
		
		if(ix < 0 || ix >= CHUNKS_SIDE ||
				iy < 0 || iy >= CHUNKS_SIDE ||
				iz < 0 || iz >= CHUNKS_SIDE)
			return -1;
		
		return posToArrayIndex(ix, iy, iz);
	}
	
	public Chunk[] getChunks() {
		return chunks;
	}
	
	private final Vector3 temp = new Vector3();
	
	public Block getBlock(Vector3 v, float radius) {
		int px = Math.round(v.x() / Chunk.SPACING);
		int py = Math.round(v.y() / Chunk.SPACING);
		int pz = Math.round(-v.z() / Chunk.SPACING);
		
		if(px < -1 || px > CHUNKS_SIDE * Chunk.CHUNK_CUBE_WIDTH ||
				py < -1 || py > CHUNKS_SIDE * Chunk.CHUNK_CUBE_HEIGHT ||
				pz < -1 || pz > CHUNKS_SIDE * Chunk.CHUNK_CUBE_DEPTH)
			return null;
		
		float lowestDistance = Float.MAX_VALUE;
		Block closestBlock = null;
		
		for(int a = -1; a < 2; a++) {
			for(int b = -1; b < 2; b++) {
				for(int c = -1; c < 2; c++) {
					Block block = getBlock(px + a, py + b, pz + c);
					
					if(block == null || block.getType() == BlockType.AIR)
						continue;
					
					float len = temp.set(px + a, py + b, -(pz + c)).mult(Chunk.SPACING).sub(v).lengthSquared();
					
					if(len < lowestDistance) {
						lowestDistance = len;
						closestBlock = block;
					}
				}
			}
		}
		
		final float d = Chunk.CUBE_SIZE * 0.5f + radius;
		
		return lowestDistance <= d * d ? closestBlock : null;
	}

	public Block getBlock(int x, int y, int z) {
		int i = cubePosToArrayIndex(x, y, z);
		if(i == -1)
			return null;
		
		Chunk chunk = chunks[i];
		return chunk.get(x % Chunk.CHUNK_CUBE_WIDTH, y % Chunk.CHUNK_CUBE_HEIGHT, z % Chunk.CHUNK_CUBE_DEPTH);
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
