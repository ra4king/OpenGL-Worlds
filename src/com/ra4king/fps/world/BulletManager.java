package com.ra4king.fps.world;

import java.util.ArrayList;

import com.ra4king.fps.actors.Bullet;
import com.ra4king.fps.world.Chunk.Block;
import com.ra4king.fps.world.Chunk.Lalalala;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author Roi Atalla
 */
public class BulletManager {
	private ArrayList<Bullet> bullets;
	
	private ChunkManager chunkManager;
	
	public BulletManager(ChunkManager chunkManager) {
		this.chunkManager = chunkManager;
		
		bullets = new ArrayList<>();
	}
	
	public void addBullet(Bullet bullet) {
		bullets.add(bullet);
	}
	
	public ArrayList<Bullet> getBullets() {
		return bullets;
	}
	
	private int blocksDestroyed;
	
	public int getBlocksDestroyedCount() {
		return blocksDestroyed;
	}
	
	private ArrayList<Bullet> temp = new ArrayList<>();

	public void update(long deltaTime) {
		for(Bullet bullet : bullets) {
			bullet.update(deltaTime);
			
			if(bullet.isAlive()) {
				boolean isAlive = true;
				
				if(bullet.isSolid()) {
					Vector3 pos = bullet.getPosition();
					
					Block block;
					if((block = chunkManager.getBlock(pos, 0.5f * bullet.getSize())) != null && block.getType() != Lalalala.AIR) {
						chunkManager.setBlock(Lalalala.AIR, block.getX(), block.getY(), block.getZ());
						
						blocksDestroyed++;
						
						isAlive = false;
						
						for(int a = -1; a < 2; a++) {
							for(int b = -1; b < 2; b++) {
								for(int c = -1; c < 2; c++) {
									if(a != 0 && b != 0 && c != 0)
										temp.add(new Bullet(pos, new Vector3(a, b, c).normalize().mult(100), 1, 500, (long)2.5e8, false, new Vector3(1, 1, 1)));
							}
							}
						}
					}
				}
				
				if(isAlive)
					temp.add(bullet);
			}
		}
		
		bullets.clear();
		
		ArrayList<Bullet> old = bullets;
		bullets = temp;
		temp = old;
	}
}
