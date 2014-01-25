package com.ra4king.fps;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.APPLEVertexArrayObject;
import org.lwjgl.opengl.ARBVertexArrayObject;

import com.ra4king.fps.FPS.FrustumCulling;
import com.ra4king.opengl.util.ShaderProgram;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.MatrixStack;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author ra4king
 */
public class BulletManager {
	private ArrayList<Bullet> bullets;
	
	private ChunkManager chunkManager;
	
	private HashMap<Bullet,Vector3> cameraWorldPositions = new HashMap<>();
	
	public static final int MAX_BULLET_COUNT = GLUtils.get().VERSION >= 33 ? 2000 : 100;
	public static final long BULLET_LIFE = (long)8e9;
	
	private final BulletRenderer renderer = new BulletRenderer();
	
	public BulletManager(ChunkManager chunkManager) {
		this.chunkManager = chunkManager;
		
		bullets = new ArrayList<>();
	}
	
	public void addBullet(Bullet bullet) {
		bullets.add(bullet);
		
		cameraWorldPositions.clear();
	}
	
	public int getBulletCount() {
		return bullets.size();
	}
	
	public void update(long deltaTime) {
		ArrayList<Bullet> temp = new ArrayList<>();
		
		for(Bullet bullet : bullets) {
			bullet.update(deltaTime);
			
			if(bullet.isAlive()) {
				boolean isAlive = true;
				
				if(false) {// bullet.isSolid()) {
					Vector3 pos = bullet.getPosition();
					
					if(chunkManager.getBlock(pos, 0.5f * Bullet.SIZE) != null) {
						for(int a = -1; a < 2; a++) {
							for(int b = -1; b < 2; b++) {
								for(int c = -1; c < 2; c++) {
									if(a == 0 && b == 0 && c == 0)
										continue;
									
									temp.add(new Bullet(pos, new Vector3(a, b, c).normalize().mult(100), (long)5e8, false));
									
									isAlive = false;
								}
							}
						}
					}
				}
				
				if(isAlive)
					temp.add(bullet);
			}
		}
		
		bullets.clear();
		bullets = temp;
		
		cameraWorldPositions.clear();
	}
	
	public int getBulletLightData(Matrix4 viewMatrix, FloatBuffer lightPositions, FloatBuffer lightColors) {
		int bulletCount = 0;
		
		final float bulletK = 0.0001f, nonSolidBulletK = 0.05f;
		
		sort(viewMatrix);
		
		for(Bullet b : bullets) {
			if(bulletCount < MAX_BULLET_COUNT) {
				bulletCount++;
				
				lightPositions.put(cameraWorldPositions.get(b).toBuffer());
				lightPositions.put((b.isSolid() ? bulletK : nonSolidBulletK) / b.getAlpha());
				
				lightColors.put(b.getColor().toBuffer()).put(1);
			}
		}
		
		return bulletCount;
	}
	
	private void sort(Matrix4 viewMatrix) {
		if(cameraWorldPositions.isEmpty())
			sort(viewMatrix, bullets, cameraWorldPositions);
	}
	
	private void sort(Matrix4 viewMatrix, List<Bullet> bullets, final HashMap<Bullet,Vector3> bulletPositions) {
		if(bulletPositions.isEmpty())
			for(Bullet b : bullets)
				bulletPositions.put(b, viewMatrix.mult(b.getPosition()));
		
		Collections.sort(bullets, new Comparator<Bullet>() {
			@Override
			public int compare(Bullet o1, Bullet o2) {
				return (int)Math.signum(bulletPositions.get(o1).z() - bulletPositions.get(o2).z());
			}
		});
	}
	
	public void render(Matrix4 projectionMatrix, MatrixStack modelViewMatrix, FrustumCulling culling) {
		sort(modelViewMatrix.getTop());
		
		renderer.render(projectionMatrix, modelViewMatrix, bullets, culling);
	}
	
	public void render(Matrix4 projectionMatrix, MatrixStack modelViewMatrix, FrustumCulling culling, Bullet ... bullets) {
		List<Bullet> bulletList = Arrays.asList(bullets);
		sort(modelViewMatrix.getTop(), bulletList, new HashMap<Bullet,Vector3>());
		
		renderer.render(projectionMatrix, modelViewMatrix, bulletList, culling);
	}
	
	private class BulletRenderer {
		private ShaderProgram bulletProgram;
		private int projectionMatrixUniform, modelViewMatrixUniform;
		private int bulletDataUniform;
		
		private FloatBuffer bulletDataBuffer;
		private int vao, bulletDataVBO;
		
		private final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
		private final boolean HAS_VAO = GLUtils.get().VERSION >= 30;
		
		private BulletRenderer() {
			bulletDataBuffer = BufferUtils.createFloatBuffer(MAX_BULLET_COUNT * 2 * 4);
			
			vao = glGenVertexArrays();
			
			if(GLUtils.get().VERSION >= 33)
				bulletProgram = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream("bullet.vert")), Utils.readFully(getClass().getResourceAsStream("bullet.frag")));
			else if(GLUtils.get().VERSION >= 30)
				bulletProgram = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream("bullet3.0.vert")), Utils.readFully(getClass().getResourceAsStream("bullet3.0.frag")));
			else
				bulletProgram = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream("bullet2.1.vert")), Utils.readFully(getClass().getResourceAsStream("bullet2.1.frag")));
			
			projectionMatrixUniform = bulletProgram.getUniformLocation("projectionMatrix");
			modelViewMatrixUniform = bulletProgram.getUniformLocation("modelViewMatrix");
			
			if(GLUtils.get().VERSION >= 33) {
				int bulletDataUniform = bulletProgram.getUniformBlockIndex("BulletData");
				glUniformBlockBinding(bulletProgram.getProgram(), bulletDataUniform, 2);
				
				bulletDataVBO = glGenBuffers();
				glBindBuffer(GL_UNIFORM_BUFFER, bulletDataVBO);
				glBufferData(GL_UNIFORM_BUFFER, bulletDataBuffer.capacity() * 4, GL_STREAM_DRAW);
				glBindBufferBase(GL_UNIFORM_BUFFER, 2, bulletDataVBO);
				glBindBuffer(GL_UNIFORM_BUFFER, 0);
			}
			else
				bulletDataUniform = bulletProgram.getUniformLocation("bulletData");
		}
		
		public void render(Matrix4 projectionMatrix, MatrixStack modelViewMatrix, List<Bullet> bullets, FrustumCulling culling) {
			glDepthMask(false);
			
			bulletProgram.begin();
			
			glUniformMatrix4(projectionMatrixUniform, false, projectionMatrix.toBuffer());
			glUniformMatrix4(modelViewMatrixUniform, false, modelViewMatrix.getTop().toBuffer());
			
			bulletDataBuffer.clear();
			
			int bulletsDrawn = 0;
			for(int a = 0; a < bullets.size() && bulletsDrawn < MAX_BULLET_COUNT; a++) {
				Bullet b = bullets.get(a);
				
				// if(culling != null && !culling.isCubeInsideFrustum(b.getPosition(), Bullet.SIZE))
				// continue;
				
				bulletsDrawn++;
				
				bulletDataBuffer.put(b.getPosition().toBuffer()).put(Bullet.SIZE);
				bulletDataBuffer.put(b.getColor().toBuffer()).put(b.getAlpha());
			}
			
			bulletDataBuffer.flip();
			
			if(GLUtils.get().VERSION >= 31) {
				glBindBuffer(GL_UNIFORM_BUFFER, bulletDataVBO);
				glBufferSubData(GL_UNIFORM_BUFFER, 0, bulletDataBuffer);
				glBindBuffer(GL_UNIFORM_BUFFER, 0);
			}
			else
				glUniform4(bulletDataUniform, bulletDataBuffer);
			
			if(HAS_VAO)
				glBindVertexArray(vao);
			else if(IS_MAC)
				APPLEVertexArrayObject.glBindVertexArrayAPPLE(vao);
			else
				ARBVertexArrayObject.glBindVertexArray(vao);
			
			glDrawArrays(GL_TRIANGLES, 0, bulletsDrawn * 6);
			
			if(HAS_VAO)
				glBindVertexArray(vao);
			else if(IS_MAC)
				APPLEVertexArrayObject.glBindVertexArrayAPPLE(vao);
			else
				ARBVertexArrayObject.glBindVertexArray(vao);
			
			bulletProgram.end();
			
			glDepthMask(true);
		}
	}
}
