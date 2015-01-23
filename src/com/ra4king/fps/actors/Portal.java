package com.ra4king.fps.actors;

import com.ra4king.fps.world.World;
import com.ra4king.opengl.util.math.Vector2;
import com.ra4king.opengl.util.math.Vector3;

import net.indiespot.struct.cp.CopyStruct;
import net.indiespot.struct.cp.Struct;

/**
 * @author Roi Atalla
 */
public class Portal {
	private World parentWorld, destWorld;
	private Vector3 position, destPosition;
	private Vector2 size;
	
	public Portal(World parentWorld, Vector3 position, Vector2 size, World destWorld, Vector3 destPosition) {
		this.parentWorld = parentWorld;
		this.destWorld = destWorld;
		this.position = Struct.malloc(Vector3.class).set(position);
		this.destPosition = Struct.malloc(Vector3.class).set(destPosition);
		this.size = Struct.malloc(Vector2.class).set(size);
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			Struct.free(position);
			Struct.free(size);
		} finally {
			super.finalize();
		}
	}
	
	public World getParentWorld() {
		return parentWorld;
	}
	
	public World getDestWorld() {
		return destWorld;
	}
	
	@CopyStruct
	public Vector3 getPosition() {
		return position;
	}
	
	@CopyStruct
	public Vector3 getDestPosition() {
		return destPosition;
	}
	
	@CopyStruct
	public Vector2 getSize() {
		return size;
	}
	
	public void update(long deltaTime) {
		destWorld.getCamera().setOrientation(parentWorld.getCamera().getOrientation());
		destWorld.getCamera().setPosition(new Vector3(destPosition).add(parentWorld.getCamera().getPosition()).sub(position));
	}
}
