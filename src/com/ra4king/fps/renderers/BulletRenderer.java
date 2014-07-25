package com.ra4king.fps.renderers;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
	
	public BulletRenderer(BulletManager bulletManager) {
		this.bulletManager = bulletManager;
		
		bulletDataBuffer = BufferUtils.createFloatBuffer(500 * 2 * 4);
		
		bulletProgram = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream(GLUtils.SHADERS_ROOT_PATH + "bullet.vert")),
				Utils.readFully(getClass().getResourceAsStream(GLUtils.SHADERS_ROOT_PATH + "bullet.frag")));
		
		projectionMatrixUniform = bulletProgram.getUniformLocation("projectionMatrix");
		modelViewMatrixUniform = bulletProgram.getUniformLocation("modelViewMatrix");
		
		bulletDataVBO = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, bulletDataVBO);
		glBufferData(GL_ARRAY_BUFFER, bulletDataBuffer.capacity() * 4, GL_STREAM_DRAW);
		
		float[] bulletMappings = {
				-0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f,
				0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f
		};
		
		FloatBuffer bulletMappingsBuffer = BufferUtils.createFloatBuffer(bulletMappings.length);
		bulletMappingsBuffer.put(bulletMappings).flip();
		
		int bulletMappingsVBO = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, bulletMappingsVBO);
		glBufferData(GL_ARRAY_BUFFER, bulletMappingsBuffer, GL_STATIC_DRAW);
		
		vao = GLUtils.glGenVertexArrays();
		GLUtils.glBindVertexArray(vao);
		
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
		
		glBindBuffer(GL_ARRAY_BUFFER, bulletDataVBO);
		glEnableVertexAttribArray(1);
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(1, 4, GL_FLOAT, false, 8 * 4, 0);
		glVertexAttribPointer(2, 4, GL_FLOAT, false, 8 * 4, 4 * 4);
		glVertexAttribDivisor(1, 1);
		glVertexAttribDivisor(2, 1);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		GLUtils.glBindVertexArray(0);
	}
	
	private HashMap<Bullet,Vector3> cameraWorldPositions = new HashMap<>();
	
	public int getBulletLightData(Matrix4 viewMatrix, FloatBuffer bulletData, int maxBulletCount) {
		final float bulletK = 0.01f, nonSolidBulletK = 0.05f;
		
		cameraWorldPositions.clear();
		sort(viewMatrix);
		
		ArrayList<Bullet> bullets = bulletManager.getBullets();
		
		int count = Math.min(bullets.size(), maxBulletCount);
		
		for(int a = 0; a < count; a++) {
			Bullet b = bullets.get(a);
			
			bulletData.put(cameraWorldPositions.get(b).toBuffer());
			bulletData.put(b.getRange());
			bulletData.put(b.getColor().toBuffer());
			bulletData.put((b.isSolid() ? bulletK : nonSolidBulletK) / b.getAlpha());
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
		
		Collections.sort(bullets, new Comparator<Bullet>() {
			@Override
			public int compare(Bullet o1, Bullet o2) {
				return (int)Math.signum(bulletPositions.get(o1).z() - bulletPositions.get(o2).z());
			}
		});
	}
	
	public void render(Matrix4 projectionMatrix, MatrixStack modelViewMatrix, FrustumCulling culling) {
		sort(modelViewMatrix.getTop());
		
		render(projectionMatrix, modelViewMatrix, bulletManager.getBullets(), culling);
	}
	
	public void render(Matrix4 projectionMatrix, MatrixStack modelViewMatrix, FrustumCulling culling, Bullet ... bullets) {
		List<Bullet> bulletList = Arrays.asList(bullets);
		sort(modelViewMatrix.getTop(), bulletList, new HashMap<Bullet,Vector3>());
		
		render(projectionMatrix, modelViewMatrix, bulletList, culling);
	}
	
	private void render(Matrix4 projectionMatrix, MatrixStack modelViewMatrix, List<Bullet> bullets, FrustumCulling culling) {
		if(bullets.size() == 0)
			return;
		
		bulletProgram.begin();
		
		glUniformMatrix4(projectionMatrixUniform, false, projectionMatrix.toBuffer());
		glUniformMatrix4(modelViewMatrixUniform, false, modelViewMatrix.getTop().toBuffer());
		
		boolean bulletCountChanged = bullets.size() * 2 * 4 > bulletDataBuffer.capacity();
		
		if(bulletCountChanged)
			bulletDataBuffer = BufferUtils.createFloatBuffer(bullets.size() * 2 * 4);
		else
			bulletDataBuffer.clear();
		
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
		
		if(bulletCountChanged)
			glBufferData(GL_ARRAY_BUFFER, bulletDataBuffer, GL_STREAM_DRAW);
		else
			glBufferSubData(GL_ARRAY_BUFFER, 0, bulletDataBuffer);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		GLUtils.glBindVertexArray(vao);
		
		glDepthMask(false);
		glDrawArraysInstanced(GL_TRIANGLES, 0, 6, bulletDrawnCount);
		glDepthMask(true);
		
		GLUtils.glBindVertexArray(0);
		
		bulletProgram.end();
	}
}