package com.ra4king.fps.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.ra4king.fps.Camera;
import com.ra4king.fps.Camera.CameraUpdate;
import com.ra4king.fps.actors.Actor;
import com.ra4king.fps.actors.Block.BlockType;
import com.ra4king.fps.actors.Bullet;
import com.ra4king.opengl.util.Stopwatch;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.math.Quaternion;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author Roi Atalla
 */
public class World implements CameraUpdate {
	private ChunkManager chunkManager;
	private BulletManager bulletManager;
	
	private ArrayList<Actor> actors;
	
	private boolean isPaused;
	
	private long deltaTimeBuffer;
	
	private long bulletCooldown, blastCoolDown;
	
	public World(int chunksX, int chunksY, int chunksZ) {
		chunkManager = new ChunkManager(this, chunksX, chunksY, chunksZ);
		bulletManager = new BulletManager(chunkManager);
		
		actors = new ArrayList<>();
	}
	
	public void addActor(Actor actor) {
		actors.add(actor);
	}
	
	public void addBullet(Bullet bullet) {
		bulletManager.addBullet(bullet);
	}
	
	public List<Actor> getActors() {
		return actors;
	}
	
	public void clearAll() {
		chunkManager.clearAll();
	}
	
	public void generateAllBlocks() {
		final int width = chunkManager.CHUNKS_SIDE_X * Chunk.CHUNK_BLOCK_WIDTH;
		final int height = chunkManager.CHUNKS_SIDE_Y * Chunk.CHUNK_BLOCK_HEIGHT;
		final int depth = chunkManager.CHUNKS_SIDE_Z * Chunk.CHUNK_BLOCK_DEPTH;
		for(int x = 0; x < width; x++) {
			for(int y = 0; y < height; y++) {
				for(int z = 0; z < depth; z++) {
					chunkManager.setBlock(BlockType.SOLID, x, y, z);
				}
			}
		}
	}
	
	public void generateRandomBlocks() {
		NoiseGenerator generator = new NoiseGenerator(chunkManager.CHUNKS_SIDE_X * Chunk.CHUNK_BLOCK_WIDTH,
		                                               chunkManager.CHUNKS_SIDE_Y * Chunk.CHUNK_BLOCK_HEIGHT,
		                                               chunkManager.CHUNKS_SIDE_Z * Chunk.CHUNK_BLOCK_DEPTH);
		generator.generateBlocks();
	}
	
	public ChunkManager getChunkManager() {
		return chunkManager;
	}
	
	public BulletManager getBulletManager() {
		return bulletManager;
	}
	
	public void keyPressed(int key, char c) {
		if(key == Keyboard.KEY_P) {
			isPaused = !isPaused;
		}
	}
	
	public void update(long deltaTime) {
		Stopwatch.start("ChunkManager Update");
		chunkManager.update(deltaTime);
		Stopwatch.stop();
		
		for(Actor a : actors)
			a.update(deltaTime);
		
		if(!isPaused) {
			Stopwatch.start("BulletManager Update");
			bulletManager.update(deltaTime);
			Stopwatch.stop();
		}
	}
	
	@Override
	public void updateCamera(Camera camera, long deltaTime) {
		Vector3 position = camera.getPosition();
		Quaternion orientation = camera.getOrientation();
		
		deltaTimeBuffer += deltaTime;
		
		Quaternion inverse;
		
		if(deltaTimeBuffer >= 1e9 / 120) {
			final float speed = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) | Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) ? 150 : 20) * deltaTimeBuffer / (float)1e9;
			final float rotSpeed = (2f / 15f) * speed;
			
			deltaTimeBuffer = 0;
			
			if(Mouse.isGrabbed()) {
				int dy = Mouse.getDY();
				if(dy != 0) {
					orientation.set(Utils.angleAxisDeg(-dy * rotSpeed, Vector3.RIGHT).mult(orientation));
				}
				
				int dx = Mouse.getDX();
				if(dx != 0) {
					orientation.set(Utils.angleAxisDeg(dx * rotSpeed, Vector3.UP).mult(orientation));
				}
			}
			
			if(Keyboard.isKeyDown(Keyboard.KEY_E)) {
				orientation.set(Utils.angleAxisDeg(-4f * rotSpeed, Vector3.FORWARD).mult(orientation));
			}
			if(Keyboard.isKeyDown(Keyboard.KEY_Q)) {
				orientation.set(Utils.angleAxisDeg(4f * rotSpeed, Vector3.FORWARD).mult(orientation));
			}
			
			orientation.normalize();
			
			inverse = new Quaternion(orientation).inverse();
			
			Vector3 delta = new Vector3(0f, 0f, 0f);
			
			if(Keyboard.isKeyDown(Keyboard.KEY_W)) {
				delta.z(-speed);
			}
			if(Keyboard.isKeyDown(Keyboard.KEY_S)) {
				delta.z(delta.z() + speed);
			}
			
			if(Keyboard.isKeyDown(Keyboard.KEY_D)) {
				delta.x(speed);
			}
			if(Keyboard.isKeyDown(Keyboard.KEY_A)) {
				delta.x(delta.x() - speed);
			}
			
			if(Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
				delta.y(speed);
			}
			if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
				delta.y(delta.y() - speed);
			}
			
			if(delta.x() != 0f || delta.y() != 0f || delta.z() != 0f) {
				position.add(inverse.mult3(delta, delta));
			}
		} else {
			inverse = new Quaternion(orientation).inverse();
		}
		
		long diff;
		if((Mouse.isButtonDown(0) || Keyboard.isKeyDown(Keyboard.KEY_C)) && (diff = System.nanoTime() - bulletCooldown) > (long)5e7) {
			int bulletSpeed = 500;
			
			bulletManager.addBullet(new Bullet(new Vector3(position).add(inverse.mult3(new Vector3(1f, -1f, 0f), new 
			                                                                                                     Vector3())), inverse.mult3(Vector3.FORWARD, new Vector3()).mult(bulletSpeed), 1, 30));
			bulletManager.addBullet(new Bullet(new Vector3(position).add(inverse.mult3(new Vector3(-1f, -1f, 0f), new 
			                                                                                                      Vector3())), inverse.mult3(Vector3.FORWARD, new Vector3()).mult(bulletSpeed), 1, 30));
			bulletCooldown += diff;
		}
		
		if((Mouse.isButtonDown(1) || Keyboard.isKeyDown(Keyboard.KEY_V)) && (diff = System.nanoTime() - blastCoolDown) > (long)3e8) {
			int blastSpeed = 200;
			
			bulletManager.addBullet(new Bullet(new Vector3(position).add(inverse.mult3(new Vector3(0, 0, -2f), new Vector3())), inverse.mult3(Vector3.FORWARD, new Vector3()).mult(blastSpeed), 10, 100));
			
			blastCoolDown += diff;
		}
	}
	
	private class NoiseGenerator {
		private int width, height, depth;
		private double[][][] noise;
		
		public NoiseGenerator(int width, int height, int depth) {
			this.width = width;
			this.height = height;
			this.depth = depth;
			noise = new double[width][height][depth];
			
			generateNoise();
		}
		
		public void generateBlocks() {
			for(int x = 0; x < width; x++) {
				for(int y = 0; y < height; y++) {
					for(int z = 0; z < depth; z++) {
						float value = (float)turbulence(x, y, z, 64);
						
						if(value >= 0.55f) {
							chunkManager.setBlock(BlockType.SOLID, x, y, z);
						}
					}
				}
			}
		}
		
		private void generateNoise() {
			Random random = new Random();
			
			for(int x = 0; x < width; x++) {
				for(int y = 0; y < height; y++) {
					for(int z = 0; z < depth; z++) {
						noise[x][y][z] = random.nextDouble();
					}
				}
			}
		}
		
		private double smoothNoise(double x, double y, double z) {
			double fractX = x - (int)x;
			double fractY = y - (int)y;
			double fractZ = z - (int)z;
			
			int x1 = ((int)x + width) % width;
			int y1 = ((int)y + height) % height;
			int z1 = ((int)z + depth) % depth;
			
			int x2 = (x1 + width - 1) % width;
			int y2 = (y1 + height - 1) % height;
			int z2 = (z1 + depth - 1) % depth;
			
			double value = 0;
			value += fractX * fractY * fractZ * noise[x1][y1][z1];
			value += fractX * (1 - fractY) * fractZ * noise[x1][y2][z1];
			value += fractX * fractY * (1 - fractZ) * noise[x1][y1][z2];
			value += (1 - fractX) * fractY * fractZ * noise[x2][y1][z1];
			value += (1 - fractX) * (1 - fractY) * fractZ * noise[x2][y2][z1];
			value += (1 - fractX) * fractY * (1 - fractZ) * noise[x2][y1][z2];
			value += fractX * (1 - fractY) * (1 - fractZ) * noise[x1][y2][z2];
			value += (1 - fractX) * (1 - fractY) * (1 - fractZ) * noise[x2][y2][z2];
			
			return value;
		}
		
		private double turbulence(double x, double y, double z, double size) {
			double value = 0.0, initialSize = size;
			
			while(size >= 1) {
				value += smoothNoise(x / size, y / size, z / size) * size;
				size /= 2.0;
			}
			
			return 0.5 * value / initialSize;
		}
	}
}
