package com.ra4king.fps.world;

import java.util.ArrayList;
import java.util.HashMap;

import com.ra4king.fps.actors.Block;
import com.ra4king.fps.actors.Block.BlockType;
import com.ra4king.fps.actors.Bullet;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author Roi Atalla
 */
public class BulletManager {
	private ArrayList<Bullet> bullets;
	
	private HashMap<Bullet,Integer> megaBulletDestroyCount;
	private final int MAX_MEGA_BULLET_DESTROY_COUNT = 2000;
	
	private ChunkManager chunkManager;
	
	public BulletManager(ChunkManager chunkManager) {
		this.chunkManager = chunkManager;
		
		bullets = new ArrayList<>();
		
		megaBulletDestroyCount = new HashMap<>();
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
	
	public boolean isMegaBullet(Bullet bullet) {
		return bullet.getSize() >= 20;
	}
	
	private ArrayList<Bullet> temp = new ArrayList<>();
	
	public void update(long deltaTime) {
		for(Bullet bullet : bullets) {
			bullet.update(deltaTime);
			
			if(bullet.isAlive()) {
				boolean isAlive = true;
				
				if(bullet.isSolid()) {
					Vector3 pos = bullet.getPosition();
					
					boolean megaBullet = isMegaBullet(bullet);
					
					// Check if it's a normal sized bullet
					Block block;
					if(!megaBullet && (block = chunkManager.getBlock(pos, 0.5f * bullet.getSize())) != null && block.getType() != BlockType.AIR) {
						chunkManager.setBlock(BlockType.AIR, block);
						
						blocksDestroyed++;
						
						isAlive = false;
						
						for(int a = 0; a < 5; a++) {
							temp.add(new Bullet(pos, new Vector3((float)Math.random() * 2 - 1, (float)Math.random() * 2 - 1, (float)Math.random() * 2 - 1).normalize().mult(100), 1, 500, (long)2.5e8, false, new Vector3(1, 1, 1)));
						}
					}
					else if(megaBullet) { // It's a mega bullet!
						Block[] blocks = chunkManager.getBlocks(pos, 0.5f * bullet.getSize());
						
						if(blocks.length > 0) {
							int destroyCount = 0;
							
							for(Block b : blocks) {
								chunkManager.setBlock(BlockType.AIR, b);
								blocksDestroyed++;
								destroyCount++;
								
								temp.add(new Bullet(new Vector3(b.getX(), b.getY(), -b.getZ()).mult(Chunk.SPACING), new Vector3((float)Math.random() * 2 - 1, (float)Math.random() * 2 - 1, (float)Math.random() * 2 - 1).normalize().mult(100), 1, 500, (long)2.5e8, false, new Vector3(1, 1, 1)));
							}
							
							Integer i = megaBulletDestroyCount.get(bullet);
							i = i == null ? destroyCount : i + destroyCount;
							megaBulletDestroyCount.put(bullet, i);
							
							if(i >= MAX_MEGA_BULLET_DESTROY_COUNT) {
								megaBulletDestroyCount.remove(bullet);
								isAlive = false;
							}
							
							long ageIncrease = Math.round((double)i / MAX_MEGA_BULLET_DESTROY_COUNT * (bullet.getLife() - bullet.getAge()));
							bullet.increaseAge(ageIncrease);
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
