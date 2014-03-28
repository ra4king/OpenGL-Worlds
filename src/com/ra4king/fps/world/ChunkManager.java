package com.ra4king.fps.world;

import java.util.HashSet;

import com.ra4king.fps.world.Chunk.BlockInfo;
import com.ra4king.fps.world.Chunk.BlockType;
import com.ra4king.fps.world.Chunk.ChunkInfo;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author ra4king
 */
public class ChunkManager {
	private HashSet<Chunk> chunks;
	
	public static final int CHUNKS_SIDE = 5;
	
	public ChunkManager(boolean random) {
		chunks = new HashSet<>();
		
		int totalCubes = 0;
		
		for(int x = 0; x < CHUNKS_SIDE; x++)
			for(int y = 0; y < CHUNKS_SIDE; y++)
				for(int z = 0; z < CHUNKS_SIDE; z++) {
					Chunk chunk = new Chunk(this, new ChunkInfo(x * Chunk.CUBES_SIDE, y * Chunk.CUBES_SIDE, z * Chunk.CUBES_SIDE), random);
					chunks.add(chunk);
					
					totalCubes += chunk.getCubeCount();
				}
		
		System.out.println("Total cubes: " + totalCubes);
	}
	
	public HashSet<Chunk> getChunks() {
		return chunks;
	}

	private final Vector3 temp = new Vector3();
	
	public BlockInfo getBlock(int x, int y, int z) {
		int chunkX = (x / Chunk.CUBES_SIDE) * Chunk.CUBES_SIDE;
		int chunkY = (y / Chunk.CUBES_SIDE) * Chunk.CUBES_SIDE;
		int chunkZ = (z / Chunk.CUBES_SIDE) * Chunk.CUBES_SIDE;
		
		for(Chunk c : chunks) {
			if(c.getChunkInfo().cornerEquals(chunkX, chunkY, chunkZ))
				return c.get(x % Chunk.CUBES_SIDE, y % Chunk.CUBES_SIDE, z % Chunk.CUBES_SIDE);
		}
		
		return null;
	}
	
	public BlockInfo getBlock(Vector3 v, float radius) {
		final float d = Chunk.CUBE_SIZE / 2 + radius;
		
		final float chunkLength = Chunk.CUBES_SIDE * Chunk.SPACING;
		
		if(v.x() < -d || v.x() > CHUNKS_SIDE * chunkLength + d ||
				v.y() < -d || v.y() > CHUNKS_SIDE * chunkLength + d ||
				v.z() > d || v.z() < -CHUNKS_SIDE * chunkLength - d) {
			return null;
		}
		
		int ix = Math.round((int)(v.x() / chunkLength) * Chunk.CUBES_SIDE); //
		int iy = Math.round((int)(v.y() / chunkLength) * Chunk.CUBES_SIDE); // Chunk position
		int iz = Math.round((int)(-v.z() / chunkLength) * Chunk.CUBES_SIDE); //
		
		int px = Math.round((v.x() - ix * Chunk.SPACING) / Chunk.SPACING); //
		int py = Math.round((v.y() - iy * Chunk.SPACING) / Chunk.SPACING); // Cube position in chunk
		int pz = Math.round((-v.z() - iz * Chunk.SPACING) / Chunk.SPACING); //
		
		for(Chunk chunk : chunks) {
			if(chunk.getChunkInfo().cornerEquals(ix, iy, iz)) {
				// BlockInfo block = chunk.get(px, py, pz);
				// if(block != null) {
				// temp.set(ix + px, iy + py, -(iz + pz)).mult(Chunk.SPACING);
				//
				// if(temp.sub(v).lengthSquared() <= d * d)
				// return new BlockInfo(block);
				// }
				
				float lowestDistance = Float.MAX_VALUE;
				BlockInfo closestBlock = null;
				
				for(int a = -1; a < 2; a++) {
					for(int b = -1; b < 2; b++) {
						for(int c = -1; c < 2; c++) {
							BlockInfo block = chunk.get(px + a, py + b, pz + c);
							
							if(block == null)
								continue;
							
							float len = temp.set(ix + px + a, iy + py + b, -(iz + pz + c)).mult(Chunk.SPACING).sub(v).lengthSquared();
							
							if(len < lowestDistance) {
								lowestDistance = len;
								closestBlock = block;
							}
						}
	}
				}
				
				return lowestDistance <= d * d ? closestBlock : null;
			}
	}
	
		return null;
	}
	
	public void setBlock(int cubeX, int cubeY, int cubeZ) {
		for(Chunk chunk : chunks) {
			if(chunk.getChunkInfo().containsBlock(cubeX, cubeY, cubeZ)) {
				chunk.set(BlockType.DIRT, cubeX % Chunk.CUBES_SIDE, cubeY % Chunk.CUBES_SIDE, cubeZ % Chunk.CUBES_SIDE);
				return;
			}
		}
	}
	
	public boolean removeBlock(BlockInfo block) {
		for(Chunk chunk : chunks) {
			if(chunk.getChunkInfo().equals(block.chunkInfo) && chunk.remove(block.x, block.y, block.z))
				return true;
		}
		
		return false;
	}
	
	public void update(long deltaTime) {
		
	}
}
