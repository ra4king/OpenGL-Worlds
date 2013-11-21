package com.ra4king.fps;

import com.ra4king.opengl.util.math.Vector3;

/**
 * @author ra4king
 */
public class Bullet {
	public static float SIZE = 3;
	
	private final Vector3 position, velocity;
	
	private Vector3 color;
	
	private long alive;
	
	private boolean isSolid;
	
	private final long life;
	
	public Bullet(Vector3 position, Vector3 velocity) {
		this(position, velocity, (long)5e9);
	}
	
	public Bullet(Vector3 position, Vector3 velocity, long lifeTime) {
		this(position, velocity, lifeTime, true);
	}
	
	public Bullet(Vector3 position, Vector3 velocity, long lifeTime, boolean isSolid) {
		this(position, velocity, lifeTime, isSolid, Math.random() < 0.5 ? new Vector3(1, 0, 1) : new Vector3(57f / 255f, 1, 20f / 255f));//(float)Math.random(), (float)Math.random(), (float)Math.random()));
	}
	
	public Bullet(Vector3 position, Vector3 velocity, long lifeTime, boolean isSolid, Vector3 color) {
		this.position = position.copy();
		this.velocity = velocity.copy();
		life = lifeTime;

		this.isSolid = isSolid;
		
		this.color = color.copy();
	}
	
	public boolean isAlive() {
		return alive < life;
	}
	
	public boolean isSolid() {
		return isSolid;
	}
	
	public float getAlpha() {
		return (float)(life - alive) / life;
	}
	
	public Vector3 getPosition() {
		return position;
	}
	
	public Vector3 getVelocity() {
		return velocity;
	}
	
	public Vector3 getColor() {
		return color;
	}
	
	public void update(long deltaTime) {
		alive += deltaTime;
		
		position.add(velocity.copy().mult(deltaTime / (float)1e9));
	}
	
	@Override
	public String toString() {
		return "Bullet @ " + position;
	}
}
