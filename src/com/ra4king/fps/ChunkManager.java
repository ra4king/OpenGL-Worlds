package com.ra4king.fps;

import java.util.HashSet;

import com.ra4king.fps.Chunk.BlockType;
import com.ra4king.fps.FPS.FrustumCulling;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author ra4king
 */
public class ChunkManager {
	private HashSet<Chunk> chunks;
	
	public static final int CHUNKS_SIDE = FPS.IS_33 ? 8 : 4;
	
	public ChunkManager() {
		chunks = new HashSet<>();
		
		int totalCubes = 0;
		
		Vector3 temp = new Vector3();
		for(int x = 0; x < CHUNKS_SIDE; x++)
			for(int y = 0; y < CHUNKS_SIDE; y++)
				for(int z = 0; z < CHUNKS_SIDE; z++) {
					Chunk chunk = new Chunk(temp.set(x * Chunk.CUBES * Chunk.SPACING, y * Chunk.CUBES * Chunk.SPACING, -z * Chunk.CUBES * Chunk.SPACING));
					chunks.add(chunk);
					
					totalCubes += chunk.getCubeCount();
				}
		
		System.out.println("Total cubes: " + totalCubes);
	}
	
	public BlockType getBlock(Vector3 v, float radius) {
		float distance = Chunk.CUBE_SIZE / 2 + radius;
		
		final float d = Chunk.CUBES * Chunk.SPACING;
		
		if(v.x() < -distance || v.x() > CHUNKS_SIDE * d + distance ||
				v.y() < -distance || v.y() > CHUNKS_SIDE * d + distance ||
				v.z() > distance || v.z() < -CHUNKS_SIDE * d - distance) {
			return null;
		}
		
		float ix = (int)(v.x() / d) * d;
		float iy = (int)(v.y() / d) * d;
		float iz = (int)(v.z() / d) * d;
		Vector3 corner = new Vector3(ix, iy, iz);
		
		int px = (int)(((v.x() + distance) % d) / Chunk.SPACING);
		int py = (int)(((v.y() + distance) % d) / Chunk.SPACING);
		int pz = (int)(((-v.z() + distance) % d) / Chunk.SPACING);
		
		for(Chunk chunk : chunks) {
			if(chunk.getCorner().equals(corner)) {
				BlockType block = chunk.get(px, py, pz);
				
				if(block == null)
					return null;
				
				Vector3 vBlock = chunk.getCorner().copy().add(new Vector3(px, py, -pz).mult(Chunk.SPACING));// add(Chunk.CUBE_SIZE / 2, Chunk.CUBE_SIZE / 2, -Chunk.CUBE_SIZE / 2);
				
				if(vBlock.sub(v).length() > distance)
					return null;
				
				return block;
			}
		}
		
		return null;
	}
	
	public void update(long deltaTime) {
		// BLAH
	}
	
	public void render(FrustumCulling culling, boolean viewChanged) {
		for(Chunk chunk : chunks)
			if(culling.isCubeInsideFrustum(chunk.getCorner(), Chunk.CUBES * Chunk.SPACING * Chunk.CUBE_SIZE))
				chunk.render(culling, viewChanged);
	}
}
