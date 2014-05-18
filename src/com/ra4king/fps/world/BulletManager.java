package com.ra4king.fps.world;

import java.util.ArrayList;

import com.ra4king.fps.actors.Bullet;
import com.ra4king.fps.world.Chunk.BlockInfo;
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
	
	public void update(long deltaTime) {
		ArrayList<Bullet> temp = new ArrayList<>();
		
		for(Bullet bullet : bullets) {
			bullet.update(deltaTime);
			
			if(bullet.isAlive()) {
				boolean isAlive = true;
				
				if(bullet.isSolid()) {
					Vector3 pos = bullet.getPosition();
					
					BlockInfo block;
					if((block = chunkManager.getBlock(pos, 0.6f * bullet.getSize())) != null) {
						if(!chunkManager.removeBlock(block))
							System.out.println("UH OH!");
						else
							blocksDestroyed++;
						
						isAlive = false;
						
						for(int a = -1; a < 2; a++) {
							for(int b = -1; b < 2; b++) {
								for(int c = -1; c < 2; c++) {
									if(a != 0 && ((a == b && b == c) || (a == -b && b == -c)))
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
		bullets = temp;
	}
}
