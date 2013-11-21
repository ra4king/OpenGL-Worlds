package com.ra4king.fps;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;

import java.nio.FloatBuffer;
import java.util.HashMap;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.PixelFormat;

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
		
		new FPS().run(new PixelFormat(16, 0, 8, 0, 4));
	}
	
	private ShaderProgram program;
	private int projectionMatrixUniform;
	private int viewMatrixUniform;
	private int modelMatrixUniform;
	
	private int lightsUniformBufferVBO;
	private FloatBuffer lightsBuffer;
	
	private Matrix4 projectionMatrix;
	
	private Vector3 position;
	private Quaternion orientation;
	private boolean viewChanged;
	
	private long mouseCooldown;
	
	private boolean isPaused;
	
	private FrustumCulling culling;
	private ChunkManager chunkManager;
	private BulletManager bulletManager;
	
	// private Fractal fractal;
	
	public static boolean IS_33;
	
	public FPS() {
		super(false);//"FPS", 1280, 800, true);
	}
	
	@Override
	public void init() {
		IS_33 = GLContext.getCapabilities().OpenGL33;
		
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
		
		if(IS_33)
			glEnable(GL_DEPTH_CLAMP);
		
		reload();
		
		reset();
	}
	
	@Override
	public void resized() {
		super.resized();
		
		projectionMatrix = new Matrix4().clearToPerspectiveDeg(60, getWidth(), getHeight(), 1, 2000);
		
		viewChanged = true;
	}
	
	private void reset() {
		// float mid = ChunkManager.CHUNKS_SIDE * Chunk.CUBES * Chunk.SPACING / 2;
		position = new Vector3(0, 0, 0);// rsrmid, mid, -mid).mult(-1);
		orientation = new Quaternion();
		
		viewChanged = true;
	}
	
	private int mainLightPositionUniform, mainDiffuseColorUniform, mainAmbientColorUniform;
	private int numberOfLightsUniform;
	private int lightPositionsUniform, lightColorsUniform;
	
	private FloatBuffer lightsColorBuffer;
	
	private void reload() {
		if(FPS.IS_33)
			program = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream("fps.vert")), Utils.readFully(getClass().getResourceAsStream("fps.frag")));
		else {
			HashMap<Integer,String> attribs = new HashMap<>();
			attribs.put(0, "position");
			attribs.put(1, "normal");
			program = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream("fps2.1.vert")), Utils.readFully(getClass().getResourceAsStream("fps2.1.frag")), attribs);
		}
		
		lightsBuffer = BufferUtils.createFloatBuffer((BulletManager.MAX_BULLET_COUNT * 2 + 4) * 4 * 4);
		
		if(FPS.IS_33) {
			if(FPS.IS_33) {
				int lightsBlockIndex = program.getUniformBlockIndex("Lights");
				glUniformBlockBinding(program.getProgram(), lightsBlockIndex, 1);

				int cubeDataUniform = glGetUniformBlockIndex(program.getProgram(), "CubeData");
				glUniformBlockBinding(program.getProgram(), cubeDataUniform, 3);
			}
			
			lightsUniformBufferVBO = glGenBuffers();
			glBindBuffer(GL_UNIFORM_BUFFER, lightsUniformBufferVBO);
			glBufferData(GL_UNIFORM_BUFFER, lightsBuffer.capacity() * 4, GL_STREAM_DRAW);
			glBindBufferBase(GL_UNIFORM_BUFFER, 1, lightsUniformBufferVBO);
			glBindBuffer(GL_UNIFORM_BUFFER, 0);
		}
		else {
			mainLightPositionUniform = program.getUniformLocation("mainLightPosition");
			mainDiffuseColorUniform = program.getUniformLocation("mainDiffuseColor");
			mainAmbientColorUniform = program.getUniformLocation("mainAmbientColor");
			numberOfLightsUniform = program.getUniformLocation("numberOfLights");
			lightPositionsUniform = program.getUniformLocation("lightPositions");
			lightColorsUniform = program.getUniformLocation("lightColors");
			
			lightsColorBuffer = BufferUtils.createFloatBuffer(lightsBuffer.capacity());
		}
		
		projectionMatrixUniform = program.getUniformLocation("projectionMatrix");
		viewMatrixUniform = program.getUniformLocation("viewMatrix");
		modelMatrixUniform = program.getUniformLocation("modelMatrix");
		
		culling = new FrustumCulling();
		
		chunkManager = new ChunkManager();
		bulletManager = new BulletManager(chunkManager);
		
		// fractal = new Fractal();
	}
	
	private long deltaTimeBuffer;
	private long secondTimeBuffer;
	
	private Vector3 delta = new Vector3();
	
	@Override
	public void update(long deltaTime) {
		if(Keyboard.isKeyDown(Keyboard.KEY_R))
			reset();
		
		// fractal.update(deltaTime);
		
		deltaTimeBuffer += deltaTime;
		secondTimeBuffer += deltaTime;
		
		if(secondTimeBuffer >= 1e9) {
			// System.out.println(fractal.getDepth() + " " + fractal.getTotal());
			secondTimeBuffer -= 1e9;
		}
		
		Quaternion inverse = null;
		
		if(deltaTimeBuffer >= 1e9 / 120) {
			final float speed = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) | Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) ? 20 : 150) * deltaTimeBuffer / (float)1e9;
			final float rotSpeed = (2f / 15f) * speed;
			
			deltaTimeBuffer = 0;
			
			if(Mouse.isGrabbed()) {
				int dy = Mouse.getDY();
				if(dy != 0)
					orientation = Utils.angleAxisDeg(-dy * rotSpeed, new Vector3(1, 0, 0)).mult(orientation);
				
				int dx = Mouse.getDX();
				if(dx != 0)
					orientation = Utils.angleAxisDeg(dx * rotSpeed, new Vector3(0, 1, 0)).mult(orientation);
				
				if(dx != 0 || dy != 0)
					viewChanged = true;
			}
			
			if(Keyboard.isKeyDown(Keyboard.KEY_E)) {
				orientation = Utils.angleAxisDeg(rotSpeed, new Vector3(0, 0, 1)).mult(orientation);
				viewChanged = true;
			}
			if(Keyboard.isKeyDown(Keyboard.KEY_Q)) {
				orientation = Utils.angleAxisDeg(-rotSpeed, new Vector3(0, 0, 1)).mult(orientation);
				viewChanged = true;
			}
			
			orientation.normalize();
			
			inverse = orientation.copy().inverse();
			
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
			
			if(delta.length() != 0)
				viewChanged = true;
			
			position.add(inverse.mult(delta));
		}
		
		if(inverse == null)
			inverse = orientation.copy().inverse();
		
		if(Mouse.isButtonDown(0) && (System.nanoTime() - mouseCooldown) > (long)3e8) {
			int bulletSpeed = 400;
			
			bulletManager.addBullet(new Bullet(position.copy().mult(-1).add(inverse.mult(new Vector3(2, -1, -3))), inverse.mult(new Vector3(0, 0.005f, -1)).normalize().mult(bulletSpeed), BulletManager.BULLET_LIFE));
			bulletManager.addBullet(new Bullet(position.copy().mult(-1).add(inverse.mult(new Vector3(-2, -1, -3))), inverse.mult(new Vector3(0, 0.005f, -1)).normalize().mult(bulletSpeed), BulletManager.BULLET_LIFE));
			mouseCooldown = System.nanoTime();
		}
		
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
	private final Vector4 mainAmbientColor = new Vector4(0.001f, 0.001f, 0.001f, 1);
	
	private final Bullet aim = new Bullet(new Vector3(), new Vector3(), Long.MAX_VALUE, false, new Vector3(1));
	
	@Override
	public void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		program.begin();
		
		glUniformMatrix4(projectionMatrixUniform, false, projectionMatrix.toBuffer());
		
		Matrix4 viewMatrix = orientation.toMatrix().translate(position);
		
		culling.setupPlanes(projectionMatrix.copy().mult(viewMatrix));
		
		final float mainK = 0.001f;
		
		lightsBuffer.clear();
		
		if(IS_33)
			lightsBuffer.position(4 * 4);
		else
			lightsColorBuffer.clear();
		
		int bulletCount = bulletManager.getBulletLightData(viewMatrix, lightsBuffer, IS_33 ? lightsBuffer : lightsColorBuffer);
		
		lightsBuffer.flip();
		
		if(IS_33) {
			lightsBuffer.put(position.toBuffer());
			lightsBuffer.put(mainK);
			lightsBuffer.put(mainDiffuseColor.toBuffer());
			lightsBuffer.put(mainAmbientColor.toBuffer());
			
			lightsBuffer.put(bulletCount).put(0).put(0).put(0);
			
			lightsBuffer.rewind();
			
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
		
		glUniformMatrix4(viewMatrixUniform, false, viewMatrix.toBuffer());
		glUniformMatrix4(modelMatrixUniform, false, new Matrix4().clearToIdentity().toBuffer());
		
		// fractal.render(new MatrixStack());
		
		chunkManager.render(culling, viewChanged);
		program.end();
		
		bulletManager.render(projectionMatrix, new MatrixStack().setTop(viewMatrix), culling);
		
		glDepthMask(false);
		
		float oldSize = Bullet.SIZE;
		Bullet.SIZE = 4;
		
		glDisable(GL_DEPTH_TEST);
		bulletManager.render(new Matrix4().clearToOrtho(-getWidth() / 2, getWidth() / 2, -getHeight() / 2, getHeight() / 2, -1, 1), new MatrixStack(), null, aim);
		glEnable(GL_DEPTH_TEST);
		
		Bullet.SIZE = oldSize;
		
		glDepthMask(true);
		
		viewChanged = false;
	}
	
	public static class FrustumCulling {
		private enum Plane {
			LEFT, RIGHT, BOTTOM, TOP, NEAR, FAR;
			
			private Vector4 plane;
			
			public static final Plane[] values = values();
			
			public float distanceFromPoint(Vector3 point) {
				return new Vector3(plane).dot(point) + plane.w();
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
		
		public boolean isCubeInsideFrustum(Vector3 center, float sideLength) {
			Vector3 v = center.copy();
			
			for(Plane p : Plane.values) {
				float d = sideLength / 2;
				
				boolean isIn;
				
				isIn = p.distanceFromPoint(v.set(center).add(d, d, d)) >= 0;
				isIn |= p.distanceFromPoint(v.set(center).add(d, d, -d)) >= 0;
				isIn |= p.distanceFromPoint(v.set(center).add(d, -d, d)) >= 0;
				isIn |= p.distanceFromPoint(v.set(center).add(d, -d, -d)) >= 0;
				isIn |= p.distanceFromPoint(v.set(center).add(-d, d, d)) >= 0;
				isIn |= p.distanceFromPoint(v.set(center).add(-d, d, -d)) >= 0;
				isIn |= p.distanceFromPoint(v.set(center).add(-d, -d, d)) >= 0;
				isIn |= p.distanceFromPoint(v.set(center).add(-d, -d, -d)) >= 0;
				
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
