package com.ra4king.fps.actors;

import com.ra4king.opengl.util.math.Vector3;

/**
 * @author Roi Atalla
 */
public class Bullet implements Actor {
	private final Vector3 position, velocity;
	private float range;
	private float size;

	private Vector3 color;

	private boolean isSolid;

	private final long life;
	private long age;

	public Bullet(Vector3 position, Vector3 velocity, float size, float range) {
		this(position, velocity, size, range, (long)5e9);
	}

	public Bullet(Vector3 position, Vector3 velocity, float size, float range, long lifeTime) {
		this(position, velocity, size, range, lifeTime, true);
	}

	// private static final Vector3[] colors = { new Vector3(1, 20f / 255f, 147f / 255f), new Vector3(1, 0, 0), new 
	// Vector3(0, 250f / 255f, 154f / 255f), new Vector3(0, 191f / 255f, 1) };

	public Bullet(Vector3 position, Vector3 velocity, float size, float range, long lifeTime, boolean isSolid) {
		this(
			position,
			velocity,
			size,
			range,
			lifeTime,
			isSolid,
			new Vector3((float)Math.random(), (float)Math.random(), (float)Math.random()));// colors[(int)
		// (Math.random() * colors.length)]);
	}

	public Bullet(Bullet other) {
		this(other.position, other.velocity, other.size, other.range, other.life, other.isSolid, other.color);
		this.age = other.age;
	}

	public Bullet(
		Vector3 position, Vector3 velocity, float size, float range, long lifeTime, boolean isSolid, Vector3 color) {
		this.position = new Vector3(position);
		this.velocity = new Vector3(velocity);
		this.size = size;
		this.range = range;
		life = lifeTime;

		this.isSolid = isSolid;

		this.color = new Vector3(color);
	}

	public boolean isAlive() {
		return age < life;
	}

	public boolean isSolid() {
		return isSolid;
	}

	public float getAlpha() {
		return (float)(life - age) / life;
	}

	public long getLife() {
		return life;
	}

	public long getAge() {
		return age;
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

	@Override
	public void update(long deltaTime) {
		age += deltaTime;

		position.add(new Vector3(velocity).mult(deltaTime / 1e9f));
	}

	@Override
	public String toString() {
		return "Bullet @ " + position.toString();
	}
}
