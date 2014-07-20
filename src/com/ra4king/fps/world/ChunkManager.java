package com.ra4king.fps.world;

import java.util.HashSet;

import com.ra4king.fps.world.Chunk.BlockInfo;
import com.ra4king.fps.world.Chunk.BlockType;
import com.ra4king.fps.world.Chunk.ChunkInfo;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author Roi Atalla
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
					Chunk chunk = new Chunk(this, new ChunkInfo(x * Chunk.CUBES_WIDTH, y * Chunk.CUBES_HEIGHT, z * Chunk.CUBES_DEPTH), random);
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
		int chunkX = (x / Chunk.CUBES_WIDTH) * Chunk.CUBES_WIDTH;
		int chunkY = (y / Chunk.CUBES_HEIGHT) * Chunk.CUBES_HEIGHT;
		int chunkZ = (z / Chunk.CUBES_DEPTH) * Chunk.CUBES_DEPTH;
		
		for(Chunk c : chunks) {
			if(c.getChunkInfo().cornerEquals(chunkX, chunkY, chunkZ))
				return c.get(x % Chunk.CUBES_WIDTH, y % Chunk.CUBES_HEIGHT, z % Chunk.CUBES_DEPTH);
		}
		
		return null;
	}
	
	public BlockInfo getBlock(Vector3 v, float radius) {
		final float d = Chunk.CUBE_SIZE / 2 + radius;
		
		final float chunkWidth = Chunk.CUBES_WIDTH * Chunk.SPACING;
		final float chunkHeight = Chunk.CUBES_HEIGHT * Chunk.SPACING;
		final float chunkDepth = Chunk.CUBES_DEPTH * Chunk.SPACING;
		
		if(v.x() < -d || v.x() > CHUNKS_SIDE * chunkWidth + d ||
				v.y() < -d || v.y() > CHUNKS_SIDE * chunkHeight + d ||
				v.z() > d || v.z() < -CHUNKS_SIDE * chunkDepth - d) {
			return null;
		}
		
		int ix = Math.round((int)(v.x() / chunkWidth) * Chunk.CUBES_WIDTH); //
		int iy = Math.round((int)(v.y() / chunkHeight) * Chunk.CUBES_HEIGHT); // Chunk position
		int iz = Math.round((int)(-v.z() / chunkDepth) * Chunk.CUBES_DEPTH); //
		
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
				chunk.set(BlockType.DIRT, cubeX % Chunk.CUBES_WIDTH, cubeY % Chunk.CUBES_HEIGHT, cubeZ % Chunk.CUBES_DEPTH);
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
	
	private long elapsedTime;
	
	public void update(long deltaTime) {
		elapsedTime += deltaTime;
		
		if(elapsedTime > 1e9) {
			elapsedTime -= 1e9;
			
			long triangles = 0;
			for(Chunk chunk : chunks)
				triangles += chunk.getLastTriangleRenderCount();
			
			System.out.printf("%d triangles rendered\n", triangles);
		}
	}
}
