package com.ra4king.fps;

import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.Quaternion;
import com.ra4king.opengl.util.math.Vector3;

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
		projectionMatrix = new Matrix4();
		position = new Vector3(0f);
		delta = new Vector3(0f);
		orientation = new Quaternion();
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
		if (cameraUpdate != null) {
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

	public Vector3 getPosition() {
		return position;
	}

	public void setPosition(Vector3 position) {
		this.position.set(position);
	}

	public Vector3 getDelta() {
		return delta;
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

	public interface CameraUpdate {
		void updateCamera(Camera camera, long deltaTime);
	}
}
