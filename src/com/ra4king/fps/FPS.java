package com.ra4king.fps;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.PixelFormat;

import com.ra4king.fps.Camera.CameraUpdate;
import com.ra4king.opengl.util.GLProgram;
import com.ra4king.opengl.util.ShaderProgram;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.MatrixStack;
import com.ra4king.opengl.util.math.Quaternion;
import com.ra4king.opengl.util.math.Vector3;
import com.ra4king.opengl.util.math.Vector4;

/**
 * @author ra4king
 */
public class FPS extends GLProgram {
	public static void main(String[] args) throws Exception {
		if(args.length > 0 && args[0].equals("ide")) {
			String os = System.getProperty("os.name").toLowerCase();
			
			String libraryPath;
			if(os.contains("win"))
				libraryPath = "E:/Roi Atalla/";
			else if(os.contains("linux"))
				libraryPath = "/home/ra4king/Dropbox/";
			else {
				System.out.println("System not supported!");
				return;
			}
			
			libraryPath += "Documents/Programming Files/Java Files/Personal Projects/Libraries/lwjgl/natives/";
			
			System.setProperty("org.lwjgl.librarypath", libraryPath);
		}
		
		new FPS().run(new PixelFormat(16, 0, 8, 0, 4));
	}
	
	private ShaderProgram program;
	private int projectionMatrixUniform;
	private int modelViewMatrixUniform;
	
	private int lightsUniformBufferVBO;
	private FloatBuffer lightsBuffer;
	
	private boolean isPaused;
	
	private Camera camera;
	
	private FrustumCulling culling;
	private ChunkManager chunkManager;
	private BulletManager bulletManager;
	
	// private Fractal fractal;
	
	public FPS() {
		super("FPS", 1280, 800, true);
	}
	
	@Override
	public void init() {
		GLUtils.get();
		
		setFPS(0);
		
		Mouse.setGrabbed(true);
		
		glClearColor(0, 0, 0, 0);// 0.4f, 0.6f, 0.9f, 0f);
		
		glEnable(GL_DEPTH_TEST);
		glDepthRange(0, 1);
		
		glDepthMask(true);
		
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glFrontFace(GL_CW);
		
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		if(GLUtils.get().VERSION >= 32)
			glEnable(GL_DEPTH_CLAMP);
		
		camera = new Camera(60, 1, 5000);
		camera.setCameraUpdate(new CameraUpdate() {
			private float deltaTimeBuffer;
			private final Vector3 delta = new Vector3();
			private final Quaternion inverse = new Quaternion();
			
			private long mouseCooldown;
			
			@Override
			public void update(long deltaTime, Camera camera, final Matrix4 projectionMatrix, final Vector3 position, final Quaternion orientation) {
				if(Keyboard.isKeyDown(Keyboard.KEY_R))
					reset();
				
				deltaTimeBuffer += deltaTime;
				
				if(deltaTimeBuffer >= 1e9 / 120) {
					final float speed = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) | Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) ? 20 : 150) * deltaTimeBuffer / (float)1e9;
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
						orientation.set(Utils.angleAxisDeg(-2 * rotSpeed, Vector3.FORWARD).mult(orientation));
						// viewChanged = true;
					}
					if(Keyboard.isKeyDown(Keyboard.KEY_Q)) {
						orientation.set(Utils.angleAxisDeg(2 * rotSpeed, Vector3.FORWARD).mult(orientation));
						// viewChanged = true;
					}
					
					orientation.normalize();
					
					inverse.set(orientation).inverse();
					
					delta.set(0, 0, 0);
					
					if(Keyboard.isKeyDown(Keyboard.KEY_W))
						delta.z(delta.z() + speed);
					if(Keyboard.isKeyDown(Keyboard.KEY_S))
						delta.z(-speed);
					
					if(Keyboard.isKeyDown(Keyboard.KEY_D))
						delta.x(-speed);
					if(Keyboard.isKeyDown(Keyboard.KEY_A))
						delta.x(delta.x() + speed);
					
					if(Keyboard.isKeyDown(Keyboard.KEY_SPACE))
						delta.y(-speed);
					if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
						delta.y(delta.y() + speed);
					
					// if(delta.length() != 0)
					// viewChanged = true;
					
					position.add(inverse.mult(delta));
				}
				
				if((Mouse.isButtonDown(0) || Keyboard.isKeyDown(Keyboard.KEY_C)) && (System.nanoTime() - mouseCooldown) > (long)3e8) {
					int bulletSpeed = 400;
					
					bulletManager.addBullet(new Bullet(position.copy().mult(-1).add(inverse.mult(rightBullet)), inverse.mult(Vector3.FORWARD).mult(bulletSpeed), BulletManager.BULLET_LIFE));
					bulletManager.addBullet(new Bullet(position.copy().mult(-1).add(inverse.mult(leftBullet)), inverse.mult(Vector3.FORWARD).mult(bulletSpeed), BulletManager.BULLET_LIFE));
					mouseCooldown = System.nanoTime();
				}
			}
		});
		
		reload();
	}
	
	private void reset() {
		// float mid = ChunkManager.CHUNKS_SIDE * Chunk.CUBES * Chunk.SPACING / 2;
		camera.getPosition().set(0, 0, 0);// rsrmid, mid, -mid).mult(-1);
		camera.getOrientation().reset();
	}
	
	@Override
	public void resized() {
		super.resized();
		
		camera.setWindowSize(getWidth(), getHeight());
	}
	
	private int mainLightPositionUniform, mainDiffuseColorUniform, mainAmbientColorUniform;
	private int numberOfLightsUniform;
	private int lightPositionsUniform, lightColorsUniform;
	public static int cubesUniform;
	
	private FloatBuffer lightsColorBuffer;
	
	private void reload() {
		if(GLUtils.get().VERSION >= 33)
			program = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream("fps.vert")), Utils.readFully(getClass().getResourceAsStream("fps.frag")));
		else if(GLUtils.get().VERSION >= 30)
			program = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream("fps3.0.vert")), Utils.readFully(getClass().getResourceAsStream("fps3.0.frag")));
		else {
			throw new RuntimeException("2.1 and below not supported.");
			
			// program = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream("fps2.1.vert")), Utils.readFully(getClass().getResourceAsStream("fps2.1.frag")));
		}
		
		lightsBuffer = BufferUtils.createFloatBuffer((BulletManager.MAX_BULLET_COUNT * 2 + 4) * 4 * 4);
		
		if(GLUtils.get().VERSION >= 33) {
			int lightsBlockIndex = program.getUniformBlockIndex("Lights");
			glUniformBlockBinding(program.getProgram(), lightsBlockIndex, 1);
			
			int cubeDataUniform = glGetUniformBlockIndex(program.getProgram(), "CubeData");
			glUniformBlockBinding(program.getProgram(), cubeDataUniform, 3);
			
			lightsUniformBufferVBO = glGenBuffers();
			glBindBuffer(GL_UNIFORM_BUFFER, lightsUniformBufferVBO);
			glBufferData(GL_UNIFORM_BUFFER, lightsBuffer.capacity() * 4, GL_STREAM_DRAW);
			glBindBufferBase(GL_UNIFORM_BUFFER, 1, lightsUniformBufferVBO);
			glBindBuffer(GL_UNIFORM_BUFFER, 0);
		}
		else {
			cubesUniform = program.getUniformLocation("cubes");
			
			mainLightPositionUniform = program.getUniformLocation("mainLightPosition");
			mainDiffuseColorUniform = program.getUniformLocation("mainDiffuseColor");
			mainAmbientColorUniform = program.getUniformLocation("mainAmbientColor");
			numberOfLightsUniform = program.getUniformLocation("numberOfLights");
			lightPositionsUniform = program.getUniformLocation("lightPositions");
			lightColorsUniform = program.getUniformLocation("lightColors");
			
			lightsColorBuffer = BufferUtils.createFloatBuffer(lightsBuffer.capacity());
		}
		
		projectionMatrixUniform = program.getUniformLocation("projectionMatrix");
		modelViewMatrixUniform = program.getUniformLocation("modelViewMatrix");
		
		culling = new FrustumCulling();
		
		chunkManager = new ChunkManager();
		bulletManager = new BulletManager(chunkManager);
		
		// fractal = new Fractal();
	}
	
	private long secondTimeBuffer;
	
	private final Vector3 delta = new Vector3();
	
	private final Vector3 rightBullet = new Vector3(2, -1, -3);
	private final Vector3 leftBullet = new Vector3(-2, -1, -3);
	
	@Override
	public void update(long deltaTime) {
		secondTimeBuffer += deltaTime;
		
		if(secondTimeBuffer >= 1e9) {
			// System.out.println(fractal.getDepth() + " " + fractal.getTotal());
			secondTimeBuffer -= 1e9;
		}
		
		camera.update(deltaTime);
		
		// fractal.update(deltaTime);
		
		if(!isPaused)
			bulletManager.update(deltaTime);
	}
	
	@Override
	public void keyPressed(int key, char c) {
		if(key == Keyboard.KEY_ESCAPE)
			Mouse.setGrabbed(!Mouse.isGrabbed());
		
		if(key == Keyboard.KEY_T) {
			reload();
			resized();
		}
		
		if(key == Keyboard.KEY_P)
			isPaused = !isPaused;
	}
	
	@Override
	public boolean shouldStop() {
		return false;
	}
	
	private final Vector4 mainDiffuseColor = new Vector4(1f, 1f, 1f, 1);
	private final Vector4 mainAmbientColor = new Vector4(0.01f, 0.01f, 0.01f, 1);
	
	private final Bullet aim = new Bullet(new Vector3(), new Vector3(), Long.MAX_VALUE, false, new Vector3(1));
	
	private final Matrix4 viewMatrix = new Matrix4(), projectionViewMatrix = new Matrix4();
	
	@Override
	public void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		program.begin();
		
		glUniformMatrix4(projectionMatrixUniform, false, camera.getProjectionMatrix().toBuffer());
		
		camera.getOrientation().toMatrix(viewMatrix).translate(camera.getPosition());
		
		culling.setupPlanes(projectionViewMatrix.set(camera.getProjectionMatrix()).mult(viewMatrix));
		
		final float mainK = 0.001f;
		
		lightsBuffer.clear();
		
		if(GLUtils.get().VERSION >= 33)
			lightsBuffer.position(4 * 4);
		else
			lightsColorBuffer.clear();
		
		int bulletCount = bulletManager.getBulletLightData(viewMatrix, lightsBuffer, GLUtils.get().VERSION >= 33 ? lightsBuffer : lightsColorBuffer);
		
		lightsBuffer.flip();
		
		Vector3 position = camera.getPosition();
		if(GLUtils.get().VERSION >= 33) {
			lightsBuffer.put(position.toBuffer());
			lightsBuffer.put(mainK);
			lightsBuffer.put(mainDiffuseColor.toBuffer());
			lightsBuffer.put(mainAmbientColor.toBuffer());
			
			lightsBuffer.put(bulletCount).put(0).put(0).put(0).rewind();
			
			glBindBuffer(GL_UNIFORM_BUFFER, lightsUniformBufferVBO);
			glBufferSubData(GL_UNIFORM_BUFFER, 0, lightsBuffer);
			glBindBuffer(GL_UNIFORM_BUFFER, 0);
		}
		else {
			glUniform4f(mainLightPositionUniform, position.x(), position.y(), position.z(), mainK);
			glUniform4(mainDiffuseColorUniform, mainDiffuseColor.toBuffer());
			glUniform4(mainAmbientColorUniform, mainAmbientColor.toBuffer());
			glUniform1f(numberOfLightsUniform, bulletCount);
			
			lightsColorBuffer.flip();
			
			glUniform4(lightPositionsUniform, lightsBuffer);
			glUniform4(lightColorsUniform, lightsColorBuffer);
		}
		
		glUniformMatrix4(modelViewMatrixUniform, false, viewMatrix.toBuffer());
		
		// fractal.render(new MatrixStack());
		
		chunkManager.render(culling);// viewChanged);
		program.end();
		
		bulletManager.render(camera.getProjectionMatrix(), new MatrixStack().setTop(viewMatrix), culling);
		
		glDepthMask(false);
		
		float oldSize = Bullet.SIZE;
		Bullet.SIZE = 4;
		
		glDisable(GL_DEPTH_TEST);
		bulletManager.render(new Matrix4().clearToOrtho(-getWidth() / 2, getWidth() / 2, -getHeight() / 2, getHeight() / 2, -1, 1), new MatrixStack(), null, aim);
		glEnable(GL_DEPTH_TEST);
		
		Bullet.SIZE = oldSize;
		
		glDepthMask(true);
		
		// viewChanged = false;
	}
	
	public static class FrustumCulling {
		private enum Plane {
			LEFT, RIGHT, BOTTOM, TOP, NEAR, FAR;
			
			private Vector4 plane;
			
			public static final Plane[] values = values();
			
			private final Vector3 temp = new Vector3();
			
			public float distanceFromPoint(Vector3 point) {
				return temp.set(plane).dot(point) + plane.w();
			}
		}
		
		public void setupPlanes(Matrix4 matrix) {
			Plane.LEFT.plane = getPlane(matrix, 1);
			Plane.RIGHT.plane = getPlane(matrix, -1);
			Plane.BOTTOM.plane = getPlane(matrix, 2);
			Plane.TOP.plane = getPlane(matrix, -2);
			Plane.NEAR.plane = getPlane(matrix, 3);
			Plane.FAR.plane = getPlane(matrix, -3);
		}
		
		private Vector4 getPlane(Matrix4 matrix, int row) {
			int scale = row < 0 ? -1 : 1;
			row = Math.abs(row) - 1;
			
			return new Vector4(
					matrix.get(3) + scale * matrix.get(row),
					matrix.get(7) + scale * matrix.get(row + 4),
					matrix.get(11) + scale * matrix.get(row + 8),
					matrix.get(15) + scale * matrix.get(row + 12)).normalize();
		}
		
		private final Vector3 temp = new Vector3();
		
		public boolean isCubeInsideFrustum(Vector3 center, float sideLength) {
			for(Plane p : Plane.values) {
				float d = sideLength / 2;
				
				boolean isIn;
				
				isIn = p.distanceFromPoint(temp.set(center).add(d, d, d)) >= 0;
				isIn |= p.distanceFromPoint(temp.set(center).add(d, d, -d)) >= 0;
				isIn |= p.distanceFromPoint(temp.set(center).add(d, -d, d)) >= 0;
				isIn |= p.distanceFromPoint(temp.set(center).add(d, -d, -d)) >= 0;
				isIn |= p.distanceFromPoint(temp.set(center).add(-d, d, d)) >= 0;
				isIn |= p.distanceFromPoint(temp.set(center).add(-d, d, -d)) >= 0;
				isIn |= p.distanceFromPoint(temp.set(center).add(-d, -d, d)) >= 0;
				isIn |= p.distanceFromPoint(temp.set(center).add(-d, -d, -d)) >= 0;
				
				if(!isIn)
					return false;
			}
			
			return true;
		}
		
		public boolean isPointInsideFrustum(Vector3 point) {
			boolean isIn = true;
			
			for(Plane p : Plane.values)
				isIn &= p.distanceFromPoint(point) > 0;
			
			return isIn;
		}
	}
}
