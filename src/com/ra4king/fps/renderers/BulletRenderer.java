package com.ra4king.fps.renderers;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.BufferUtils;

import com.ra4king.fps.actors.Bullet;
import com.ra4king.fps.world.BulletManager;
import com.ra4king.opengl.util.ShaderProgram;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.MatrixStack;
import com.ra4king.opengl.util.math.Vector3;
import com.ra4king.opengl.util.render.RenderUtils;
import com.ra4king.opengl.util.render.RenderUtils.FrustumCulling;

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
		
		bulletProgram = new ShaderProgram(Utils.readFully(Resources.getInputStream("shaders/bullet.vert")),
				                                 Utils.readFully(Resources.getInputStream("shaders/bullet.frag")));
		
		projectionMatrixUniform = bulletProgram.getUniformLocation("projectionMatrix");
		modelViewMatrixUniform = bulletProgram.getUniformLocation("modelViewMatrix");
		
		float[] bulletMappings = {
				-0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f,
				0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f
		};
		
		FloatBuffer bulletMappingsBuffer = BufferUtils.createFloatBuffer(bulletMappings.length);
		bulletMappingsBuffer.put(bulletMappings).flip();
		
		vao = RenderUtils.glGenVertexArrays();
		RenderUtils.glBindVertexArray(vao);
		
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
		RenderUtils.glVertexAttribDivisor(1, 1);
		
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(2, 4, GL_FLOAT, false, 2 * 4 * 4, 4 * 4);
		RenderUtils.glVertexAttribDivisor(2, 1);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		RenderUtils.glBindVertexArray(0);
	}
	
	public int getBulletLightData(Matrix4 viewMatrix, FloatBuffer bulletData, int maxBulletCount) {
		final float bulletK = 0.1f, hugeBulletK = 0.01f, nonSolidBulletK = 0.5f;
		
		BulletVectorPair[] bulletVectorPairs = new BulletVectorPair[bulletManager.getBullets().size()];
		for(int a = 0; a < bulletVectorPairs.length; a++)
			bulletVectorPairs[a] = new BulletVectorPair(null, new Vector3());
		sort(bulletManager.getBullets(), viewMatrix, bulletVectorPairs);
		
		int count = 0;
		
		for(int a = bulletVectorPairs.length - 1; a >= 0 && count < maxBulletCount; a--) {
			Bullet b = bulletVectorPairs[a].bullet;
			Vector3 v = bulletVectorPairs[a].vector;
			
			if(v.z() >= 0) {
				continue;
			}
			
			bulletData.put(v.toBuffer());
			bulletData.put(b.getRange());
			bulletData.put(b.getColor().toBuffer());
			bulletData.put((b.isSolid() ? (bulletManager.isMegaBullet(b) ? hugeBulletK : bulletK) : nonSolidBulletK) / b.getAlpha());
			
			count++;
		}
		
		return count;
	}
	
	private static void sort(List<Bullet> bullets, Matrix4 viewMatrix, BulletVectorPair[] sortedBullets) {
		if(bullets.size() != sortedBullets.length) {
			throw new IllegalArgumentException("sortedBullets array is invalid length!");
		}
		
		for(int a = 0; a < bullets.size(); a++) {
			Bullet b = bullets.get(a);
			sortedBullets[a].bullet = bullets.get(a);
			viewMatrix.mult3(b.getPosition(), 1.0f, sortedBullets[a].vector);
		}
		
		Arrays.sort(sortedBullets);
	}
	
	public void render(Matrix4 projectionMatrix, MatrixStack modelViewMatrix, FrustumCulling culling) {
		BulletVectorPair[] bulletVectorPairs = new BulletVectorPair[bulletManager.getBullets().size()];
		for(int a = 0; a < bulletVectorPairs.length; a++)
			bulletVectorPairs[a] = new BulletVectorPair(null, new Vector3());
		
		sort(bulletManager.getBullets(), modelViewMatrix.getTop(), bulletVectorPairs);
		
		render(projectionMatrix, modelViewMatrix, bulletVectorPairs, culling);
	}
	
	public void render(Matrix4 projectionMatrix, MatrixStack modelViewMatrix, FrustumCulling culling, Bullet... bullets) {
		List<Bullet> bulletList = Arrays.asList(bullets);
		
		BulletVectorPair[] bulletVectorPairs = new BulletVectorPair[bulletList.size()];
		for(int a = 0; a < bulletVectorPairs.length; a++)
			bulletVectorPairs[a] = new BulletVectorPair(null, new Vector3());
		
		sort(bulletList, modelViewMatrix.getTop(), bulletVectorPairs);
		
		render(projectionMatrix, modelViewMatrix, bulletVectorPairs, culling);
	}
	
	private void render(Matrix4 projectionMatrix, MatrixStack modelViewMatrix, BulletVectorPair[] bullets, FrustumCulling culling) {
		if(bullets.length == 0) {
			return;
		}
		
		bulletProgram.begin();
		
		glUniformMatrix4(projectionMatrixUniform, false, projectionMatrix.toBuffer());
		glUniformMatrix4(modelViewMatrixUniform, false, modelViewMatrix.getTop().toBuffer());
		
		final int BULLET_COUNT = BULLET_SIZE / 2;
		
		boolean bulletCountChanged = bullets.length * BULLET_COUNT > bulletDataBuffer.capacity();
		
		if(bulletCountChanged) {
			while(bullets.length * BULLET_COUNT > BULLET_BUFFER_SIZE >> 2) {
				BULLET_BUFFER_SIZE *= 2;
			}
			
			bulletDataBuffer = BufferUtils.createFloatBuffer(BULLET_BUFFER_SIZE >> 2);
		} else {
			bulletDataBuffer.clear();
		}
		
		int bulletDrawnCount = 0;
		
		for(BulletVectorPair bvp : bullets) {
			if(culling != null && !culling.isCubeInsideFrustum(bvp.bullet.getPosition(), bvp.bullet.getSize())) {
				continue;
			}
			
			bulletDrawnCount++;
			
			bulletDataBuffer.put(bvp.bullet.getPosition().toBuffer()).put(bvp.bullet.getSize());
			bulletDataBuffer.put(bvp.bullet.getColor().toBuffer()).put(bvp.bullet.getAlpha());
		}
		
		bulletDataBuffer.flip();
		
		glBindBuffer(GL_ARRAY_BUFFER, bulletDataVBO);
		
		if(bulletCountChanged) {
			glBufferData(GL_ARRAY_BUFFER, bulletDataBuffer, GL_STREAM_DRAW);
		} else {
			glBufferData(GL_ARRAY_BUFFER, BULLET_BUFFER_SIZE, GL_STREAM_DRAW);
			glBufferSubData(GL_ARRAY_BUFFER, 0, bulletDataBuffer);
		}
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		RenderUtils.glBindVertexArray(vao);
		
		glDepthMask(false);
		RenderUtils.glDrawArraysInstanced(GL_TRIANGLES, 0, 6, bulletDrawnCount);
		glDepthMask(true);
	}
	
	private static class BulletVectorPair implements Comparable {
		Bullet bullet;
		Vector3 vector;
		
		BulletVectorPair(Bullet bullet, Vector3 vector) {
			this.bullet = bullet;
			this.vector = vector;
		}
		
		@Override
		public int compareTo(Object o) {
			if(o instanceof BulletVectorPair) {
				BulletVectorPair bvp = (BulletVectorPair)o;
				return (int)Math.signum(this.vector.z() - bvp.vector.z());
			}
			
			throw new IllegalArgumentException("Wat");
		}
	}
}
