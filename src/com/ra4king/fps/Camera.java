package com.ra4king.fps;

import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.Quaternion;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author ra4king
 */
public class Camera {
	private final Matrix4 projectionMatrix;
	private float fov, width, height, near, far;
	
	private final Vector3 position;
	private final Quaternion orientation;
	
	private CameraUpdate cameraUpdate;
	
	public Camera(float fov, float near, float far) {
		this.fov = fov;
		this.near = near;
		this.far = far;
		
		projectionMatrix = new Matrix4();
		position = new Vector3();
		orientation = new Quaternion();
	}
	
	public void setWindowSize(float width, float height) {
		getProjectionMatrix().clearToPerspectiveDeg(fov, width, height, near, far);
	}
	
	public void update(long deltaTime) {
		if(cameraUpdate != null)
			cameraUpdate.update(deltaTime, this, projectionMatrix, position, orientation);
	}
	
	public CameraUpdate getCameraUpdate() {
		return cameraUpdate;
	}
	
	public void setCameraUpdate(CameraUpdate cameraUpdate) {
		this.cameraUpdate = cameraUpdate;
	}
	
	public Vector3 getPosition() {
		return position;
	}
	
	public void setPosition(Vector3 position) {
		this.position.set(position);
	}
	
	public Quaternion getOrientation() {
		return orientation;
	}
	
	public void setOrientation(Quaternion orientation) {
		this.orientation.set(orientation);
	}
	
	public Matrix4 getProjectionMatrix() {
		return projectionMatrix;
	}
	
	public void setProjectionMatrix(Matrix4 projectionMatrix) {
		this.projectionMatrix.set(projectionMatrix);
	}
	
	public static interface CameraUpdate {
		public void update(long deltaTime, Camera camera, Matrix4 projectionMatrix, Vector3 position, Quaternion orientation);
	}
}