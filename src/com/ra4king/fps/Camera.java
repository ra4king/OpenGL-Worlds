package com.ra4king.fps;

import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.Quaternion;
import com.ra4king.opengl.util.math.Vector3;

import net.indiespot.struct.cp.Struct;
import net.indiespot.struct.cp.TakeStruct;

/**
 * @author Roi Atalla
 */
public class Camera {
	private final Matrix4 projectionMatrix;
	private float fov, near, far;
	
	private final Vector3 position;
	private final Vector3 delta;
	private final Quaternion orientation;
	
	private CameraUpdate cameraUpdate;
	
	public Camera() {
		projectionMatrix = Struct.malloc(Matrix4.class).clearToIdentity();
		position = Struct.malloc(Vector3.class).set(0f);
		delta = Struct.malloc(Vector3.class).set(0f);
		orientation = Struct.malloc(Quaternion.class).reset();
	}
	
	public Camera(float fov, float near, float far) {
		this();
		
		this.fov = fov;
		this.near = near;
		this.far = far;
	}
	
	public Camera(Camera other) {
		this();
		
		setCamera(other);
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			Struct.free(projectionMatrix);
			Struct.free(position);
			Struct.free(orientation);
		} finally {
			super.finalize();
		}
	}
	
	public void setCamera(Camera camera) {
		this.fov = camera.fov;
		this.near = camera.near;
		this.far = camera.far;
		
		projectionMatrix.set(camera.projectionMatrix);
		position.set(camera.position);
		orientation.set(camera.orientation);
	}
	
	public void setWindowSize(float width, float height) {
		projectionMatrix.clearToPerspectiveDeg(fov, width, height, near, far);
	}
	
	public void update(long deltaTime) {
		if(cameraUpdate != null) {
			Vector3 lastPos = new Vector3(position);
			cameraUpdate.updateCamera(this, deltaTime);
			delta.set(position).sub(lastPos);
		}
	}
	
	public CameraUpdate getCameraUpdate() {
		return cameraUpdate;
	}
	
	public void setCameraUpdate(CameraUpdate cameraUpdate) {
		this.cameraUpdate = cameraUpdate;
	}
	
	@TakeStruct
	public Vector3 getPosition() {
		return position;
	}
	
	public void setPosition(Vector3 position) {
		this.position.set(position);
	}
	
	@TakeStruct
	public Vector3 getDelta() {
		return delta;
	}
	
	@TakeStruct
	public Quaternion getOrientation() {
		return orientation;
	}
	
	public void setOrientation(Quaternion orientation) {
		this.orientation.set(orientation);
	}
	
	@TakeStruct
	public Matrix4 getProjectionMatrix() {
		return projectionMatrix;
	}
	
	public void setProjectionMatrix(Matrix4 projectionMatrix) {
		this.projectionMatrix.set(projectionMatrix);
	}
	
	public interface CameraUpdate {
		void updateCamera(Camera camera, long deltaTime);
	}
}
