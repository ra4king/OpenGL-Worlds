package com.ra4king.fps.actors;

import com.ra4king.opengl.util.math.Vector3;

/**
 * @author Roi Atalla
 */
public class Bullet {
	private final Vector3 position, velocity;
	private float range;
	private float size;
	
	private Vector3 color;
	
	private boolean isSolid;
	
	private final long life;
	private long alive;
	
	public Bullet(Vector3 position, Vector3 velocity, float size, float range) {
		this(position, velocity, size, range, (long)5e9);
	}
	
	public Bullet(Vector3 position, Vector3 velocity, float size, float range, long lifeTime) {
		this(position, velocity, size, range, lifeTime, true);
	}
	
	private static final Vector3[] colors = { new Vector3(1, 20f / 255f, 147f / 255f), new Vector3(1, 0, 0), new Vector3(0, 250f / 255f, 154f / 255f), new Vector3(0, 191f / 255f, 1) };
	
	public Bullet(Vector3 position, Vector3 velocity, float size, float range, long lifeTime, boolean isSolid) {
		this(position, velocity, size, range, lifeTime, isSolid, colors[(int)(Math.random() * colors.length)]);
	}
	
	public Bullet(Vector3 position, Vector3 velocity, float size, float range, long lifeTime, boolean isSolid, Vector3 color) {
		this.position = position.copy();
		this.velocity = velocity.copy();
		this.size = size;
		this.range = range;
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
	
	public float getSize() {
		return size;
	}
	
	public Vector3 getColor() {
		return color;
	}
	
	public float getRange() {
		return range;
	}
	
	private final Vector3 temp = new Vector3();
	
	public void update(long deltaTime) {
		alive += deltaTime;
		
		position.add(temp.set(velocity).mult(deltaTime / (float)1e9));
	}
	
	@Override
	public String toString() {
		return "Bullet @ " + position;
	}
}
