package com.ra4king.fps.renderers;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.BufferUtils;

import com.ra4king.fps.GLUtils;
import com.ra4king.fps.GLUtils.FrustumCulling;
import com.ra4king.fps.actors.Bullet;
import com.ra4king.fps.world.BulletManager;
import com.ra4king.opengl.util.ShaderProgram;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.MatrixStack;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author Roi Atalla
 */
public class BulletRenderer {
	private BulletManager bulletManager;
	
	private ShaderProgram bulletProgram;
	private int projectionMatrixUniform, modelViewMatrixUniform;
	
	private FloatBuffer bulletDataBuffer;
	private int vao, bulletDataVBO;
	
	private final int BULLET_SIZE = 2 * 4 * 4;
	private int BULLET_BUFFER_SIZE = 1000 * BULLET_SIZE;
	
	public BulletRenderer(BulletManager bulletManager) {
		this.bulletManager = bulletManager;
		
		bulletProgram = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream(GLUtils.RESOURCES_ROOT_PATH + "shaders/bullet.vert")),
				Utils.readFully(getClass().getResourceAsStream(GLUtils.RESOURCES_ROOT_PATH + "shaders/bullet.frag")));
		
		projectionMatrixUniform = bulletProgram.getUniformLocation("projectionMatrix");
		modelViewMatrixUniform = bulletProgram.getUniformLocation("modelViewMatrix");
		
		float[] bulletMappings = {
				-0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f,
				0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f
		};
		
		FloatBuffer bulletMappingsBuffer = BufferUtils.createFloatBuffer(bulletMappings.length);
		bulletMappingsBuffer.put(bulletMappings).flip();
		
		vao = GLUtils.glGenVertexArrays();
		GLUtils.glBindVertexArray(vao);
		
		int bulletMappingsVBO = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, bulletMappingsVBO);
		glBufferData(GL_ARRAY_BUFFER, bulletMappingsBuffer, GL_STATIC_DRAW);
		
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
		
		bulletDataBuffer = BufferUtils.createFloatBuffer(BULLET_BUFFER_SIZE / 4);
		
		bulletDataVBO = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, bulletDataVBO);
		glBufferData(GL_ARRAY_BUFFER, BULLET_BUFFER_SIZE, GL_STREAM_DRAW);
		
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 4, GL_FLOAT, false, 2 * 4 * 4, 0);
		GLUtils.glVertexAttribDivisor(1, 1);
		
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(2, 4, GL_FLOAT, false, 2 * 4 * 4, 4 * 4);
		GLUtils.glVertexAttribDivisor(2, 1);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		GLUtils.glBindVertexArray(0);
	}
	
	private HashMap<Bullet,Vector3> cameraWorldPositions = new HashMap<>();
	
	public int getBulletLightData(Matrix4 viewMatrix, FloatBuffer bulletData, int maxBulletCount) {
		final float bulletK = 0.01f, hugeBulletK = 0.0001f, nonSolidBulletK = 0.05f;
		
		cameraWorldPositions.clear();
		sort(viewMatrix);
		
		ArrayList<Bullet> bullets = bulletManager.getBullets();
		
		int count = 0;
		
		for(int a = bullets.size() - 1; a >= 0 && count < maxBulletCount; a--) {
			Bullet b = bullets.get(a);
			Vector3 v = cameraWorldPositions.get(b);
			
			if(v.z() >= 0)
				continue;
			
			bulletData.put(v.toBuffer());
			bulletData.put(b.getRange());
			bulletData.put(b.getColor().toBuffer());
			bulletData.put((b.isSolid() ? (bulletManager.isMegaBullet(b) ? hugeBulletK : bulletK) : nonSolidBulletK) / b.getAlpha());
			
			count++;
		}
		
		return count;
	}
	
	private void sort(Matrix4 viewMatrix) {
		sort(viewMatrix, bulletManager.getBullets(), cameraWorldPositions);
	}
	
	private void sort(Matrix4 viewMatrix, List<Bullet> bullets, final HashMap<Bullet,Vector3> bulletPositions) {
		if(bulletPositions.isEmpty())
			for(Bullet b : bullets)
				bulletPositions.put(b, viewMatrix.mult(b.getPosition()));
		
		Collections.sort(bullets, (Bullet o1, Bullet o2) ->
				(int)Math.signum(bulletPositions.get(o1).z() - bulletPositions.get(o2).z())
				);
	}
	
	public void render(Matrix4 projectionMatrix, MatrixStack modelViewMatrix, FrustumCulling culling) {
		sort(modelViewMatrix.getTop());
		
		render(projectionMatrix, modelViewMatrix, bulletManager.getBullets(), culling);
	}
	
	public void render(Matrix4 projectionMatrix, MatrixStack modelViewMatrix, FrustumCulling culling, Bullet ... bullets) {
		List<Bullet> bulletList = Arrays.asList(bullets);
		sort(modelViewMatrix.getTop(), bulletList, new HashMap<>());
		
		render(projectionMatrix, modelViewMatrix, bulletList, culling);
	}
	
	private void render(Matrix4 projectionMatrix, MatrixStack modelViewMatrix, List<Bullet> bullets, FrustumCulling culling) {
		if(bullets.size() == 0)
			return;
		
		bulletProgram.begin();
		
		glUniformMatrix4(projectionMatrixUniform, false, projectionMatrix.toBuffer());
		glUniformMatrix4(modelViewMatrixUniform, false, modelViewMatrix.getTop().toBuffer());
		
		final int BULLET_COUNT = BULLET_SIZE / 2;
		
		boolean bulletCountChanged = bullets.size() * BULLET_COUNT > bulletDataBuffer.capacity();
		
		if(bulletCountChanged) {
			while(bullets.size() * BULLET_COUNT > BULLET_BUFFER_SIZE >> 2) {
				BULLET_BUFFER_SIZE *= 2;
			}
			
			bulletDataBuffer = BufferUtils.createFloatBuffer(BULLET_BUFFER_SIZE >> 2);
		}
		else {
			bulletDataBuffer.clear();
		}
		
		int bulletDrawnCount = 0;
		
		for(Bullet b : bullets) {
			if(culling != null && !culling.isCubeInsideFrustum(b.getPosition(), b.getSize()))
				continue;
			
			bulletDrawnCount++;
			
			bulletDataBuffer.put(b.getPosition().toBuffer()).put(b.getSize());
			bulletDataBuffer.put(b.getColor().toBuffer()).put(b.getAlpha());
		}
		
		bulletDataBuffer.flip();
		
		glBindBuffer(GL_ARRAY_BUFFER, bulletDataVBO);
		
		if(bulletCountChanged) {
			glBufferData(GL_ARRAY_BUFFER, bulletDataBuffer, GL_STREAM_DRAW);
		}
		else {
			glBufferData(GL_ARRAY_BUFFER, BULLET_BUFFER_SIZE, GL_STREAM_DRAW);
			glBufferSubData(GL_ARRAY_BUFFER, 0, bulletDataBuffer);
		}
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		GLUtils.glBindVertexArray(vao);
		
		glDepthMask(false);
		GLUtils.glDrawArraysInstanced(GL_TRIANGLES, 0, 6, bulletDrawnCount);
		glDepthMask(true);
	}
}
