package com.ra4king.fps.world;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.ra4king.fps.Camera;
import com.ra4king.fps.Camera.CameraUpdate;
import com.ra4king.fps.GLUtils;
import com.ra4king.fps.actors.Bullet;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.Quaternion;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author Roi Atalla
 */
public class World implements CameraUpdate {
	private float deltaTimeBuffer;
	
	private long mouseCooldown;
	
	private Camera camera;
	
	private ChunkManager chunkManager;
	private BulletManager bulletManager;
	
	private boolean isPaused;
	
	public World() {
		camera = new Camera(60, 1, 5000);
		camera.setCameraUpdate(this);
		
		chunkManager = new ChunkManager(false);
		bulletManager = new BulletManager(chunkManager);
		
		reset();
	}
	
	public Camera getCamera() {
		return camera;
	}
	
	public ChunkManager getChunkManager() {
		return chunkManager;
	}
	
	public BulletManager getBulletManager() {
		return bulletManager;
	}
	
	private void reset() {
		camera.setPosition(new Vector3(10, 10, -10));
		camera.setOrientation(Utils.lookAt(new Vector3(-161, -161, 161), camera.getPosition(), new Vector3(0, 1, 0)).toQuaternion().normalize());
	}
	
	public void resized() {
		camera.setWindowSize(GLUtils.get().getWidth(), GLUtils.get().getHeight());
	}
	
	public void keyPressed(int key, char c) {
		if(key == Keyboard.KEY_P)
			isPaused = !isPaused;
	}
	
	private long timePassed;
	
	public void update(long deltaTime) {
		camera.update(deltaTime);
		
		timePassed += deltaTime;
		
		while(timePassed >= 1e9) {
			timePassed -= 1e9;
			
			System.out.println("Bullets fired: " + bulletCount + "\tBlocks destroyed: " + bulletManager.getBlocksDestroyedCount());
		}
		
		chunkManager.update(deltaTime);
		
		if(!isPaused)
			bulletManager.update(deltaTime);
	}
	
	private final Vector3 delta = new Vector3();
	private final Quaternion inverse = new Quaternion();
	
	private final Vector3 rightBullet = new Vector3(2, -1, -3);
	private final Vector3 leftBullet = new Vector3(-2, -1, -3);
	
	private int bulletCount;
	
	@Override
	public void updateCamera(long deltaTime, Camera camera, Matrix4 projectionMatrix, Vector3 position, Quaternion orientation) {
		if(Keyboard.isKeyDown(Keyboard.KEY_R))
			reset();
		
		deltaTimeBuffer += deltaTime;
		
		if(deltaTimeBuffer >= 1e9 / 120) {
			final float speed = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) | Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) ? 150 : 20) * deltaTimeBuffer / (float)1e9;
			final float rotSpeed = (2f / 15f) * speed;
			
			deltaTimeBuffer = 0;
			
			if(Mouse.isGrabbed()) {
				int dy = Mouse.getDY();
				if(dy != 0)
					orientation.set(Utils.angleAxisDeg(-dy * rotSpeed, Vector3.RIGHT).mult(orientation));
				
				int dx = Mouse.getDX();
				if(dx != 0)
					orientation.set(Utils.angleAxisDeg(dx * rotSpeed, Vector3.UP).mult(orientation));
				
				// if(dx != 0 || dy != 0)
				// viewChanged = true;
			}
			
			if(Keyboard.isKeyDown(Keyboard.KEY_E)) {
				orientation.set(Utils.angleAxisDeg(-4 * rotSpeed, Vector3.FORWARD).mult(orientation));
				// viewChanged = true;
			}
			if(Keyboard.isKeyDown(Keyboard.KEY_Q)) {
				orientation.set(Utils.angleAxisDeg(4f * rotSpeed, Vector3.FORWARD).mult(orientation));
				// viewChanged = true;
			}
			
			orientation.normalize();
			
			inverse.set(orientation).inverse();
			
			delta.set(0, 0, 0);
			
			if(Keyboard.isKeyDown(Keyboard.KEY_W))
				delta.z(speed);
			if(Keyboard.isKeyDown(Keyboard.KEY_S))
				delta.z(delta.z() - speed);
			
			if(Keyboard.isKeyDown(Keyboard.KEY_D))
				delta.x(-speed);
			if(Keyboard.isKeyDown(Keyboard.KEY_A))
				delta.x(delta.x() + speed);
			
			if(Keyboard.isKeyDown(Keyboard.KEY_SPACE))
				delta.y(-speed);
			if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
				delta.y(delta.y() + speed);
			
			if(delta.x() != 0 || delta.y() != 0 || delta.z() != 0)
				position.add(inverse.mult(delta));
		}
		
		if((Mouse.isButtonDown(0) || Keyboard.isKeyDown(Keyboard.KEY_C)) && (System.nanoTime() - mouseCooldown) > (long)7e7) {
			int bulletSpeed = 200;
			
			bulletManager.addBullet(new Bullet(position.copy().mult(-1).add(inverse.mult(rightBullet)), inverse.mult(Vector3.FORWARD).mult(bulletSpeed), 3, 500));
			bulletManager.addBullet(new Bullet(position.copy().mult(-1).add(inverse.mult(leftBullet)), inverse.mult(Vector3.FORWARD).mult(bulletSpeed), 3, 500));
			mouseCooldown = System.nanoTime();
			
			bulletCount += 2;
		}
	}
}
