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
	private float fov, width, height, near, far;
	
	private final Vector3 position;
	private final Quaternion orientation;
	
	private CameraUpdate cameraUpdate;
	
	public Camera(float fov, float near, float far) {
		this.fov = fov;
		this.near = near;
		this.far = far;
		
		projectionMatrix = Struct.malloc(Matrix4.class);
		position = Struct.malloc(Vector3.class).set(0f);
		orientation = Struct.malloc(Quaternion.class).reset();
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
	
	public void setWindowSize(float width, float height) {
		getProjectionMatrix().clearToPerspectiveDeg(fov, width, height, near, far);
	}
	
	public void update(long deltaTime) {
		if(cameraUpdate != null)
			cameraUpdate.updateCamera(deltaTime, this, projectionMatrix, position, orientation);
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
	
	public static interface CameraUpdate {
		public void updateCamera(long deltaTime, Camera camera, Matrix4 projectionMatrix, Vector3 position, Quaternion orientation);
	}
}
