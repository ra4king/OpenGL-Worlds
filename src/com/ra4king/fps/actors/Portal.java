package com.ra4king.fps.actors;

import com.ra4king.fps.Camera;
import com.ra4king.fps.OpenGLWorlds;
import com.ra4king.fps.world.World;
import com.ra4king.opengl.util.math.Vector2;
import com.ra4king.opengl.util.math.Vector3;

import net.indiespot.struct.cp.Struct;
import net.indiespot.struct.cp.TakeStruct;

/**
 * @author Roi Atalla
 */
public class Portal implements Actor {
	private OpenGLWorlds worldsManager;
	
	private World parentWorld, destWorld;
	private Vector3 position, destPosition;
	private Vector2 size;
	
	public Portal(OpenGLWorlds worldsManager, World parentWorld, Vector3 position, Vector2 size, World destWorld, Vector3 destPosition) {
		this.worldsManager = worldsManager;
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
			Struct.free(destPosition);
			Struct.free(size);
		} finally {
			super.finalize();
		}
	}
	
	public Camera getCamera() {
		return worldsManager.getCamera();
	}
	
	public World getParentWorld() {
		return parentWorld;
	}
	
	public World getDestWorld() {
		return destWorld;
	}
	
	@TakeStruct
	public Vector3 getPosition() {
		return position;
	}
	
	@TakeStruct
	public Vector3 getDestPosition() {
		return destPosition;
	}
	
	@TakeStruct
	public Vector2 getSize() {
		return size;
	}
	
	@Override
	public void update(long deltaTime) {
		Camera camera = getCamera();
		
		Vector3 delta = camera.getDelta();
		Vector3 prevPos = new Vector3(camera.getPosition()).sub(delta);
		
		Vector3 normal = new Vector3(0, size.y(), 0f).cross(new Vector3(size.x(), 0f, 0f));
		float d = -position.dot(normal);
		
		float dot = delta.dot(normal);
		
		if(dot != 0.0) {
			float t = (-prevPos.dot(normal) - d) / dot;
			
			if(t >= 0f && t <= 1f) {
				Vector3 intersection = new Vector3(delta).mult(t).add(prevPos);
				Vector3 offset = intersection.sub(position);
				
				if(offset.x() >= 0f && offset.x() <= size.x() &&
				     offset.y() <= 0f && offset.y() >= -size.y()) {
					worldsManager.setWorld(destWorld);
					camera.setPosition(new Vector3(destPosition).add(camera.getPosition()).sub(position));
					camera.getDelta().set(0f);
				}
			}
		}
	}
}
