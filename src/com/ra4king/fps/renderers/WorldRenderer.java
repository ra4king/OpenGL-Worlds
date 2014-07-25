package com.ra4king.fps.renderers;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.HashSet;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;

import com.ra4king.fps.Camera;
import com.ra4king.fps.GLUtils;
import com.ra4king.fps.GLUtils.FrustumCulling;
import com.ra4king.fps.actors.Bullet;
import com.ra4king.fps.world.Chunk;
import com.ra4king.fps.world.World;
import com.ra4king.opengl.util.ShaderProgram;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.math.Matrix3;
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.MatrixStack;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author Roi Atalla
 */
public class WorldRenderer {
	private static final int MAX_NUM_LIGHTS;

	private World world;
	
	private ShaderProgram worldProgram;
	
	private FrustumCulling culling;
	
	private int cubeVBO, indicesVBO;
	private HashSet<ChunkRenderer> chunkRenderers;

	private BulletRenderer bulletRenderer;
	private LightSystem lightSystem;
	
	private int projectionMatrixUniform;
	private int viewMatrixUniform;
	private int normalMatrixUniform;
	
	static {
		if(GLUtils.GL_VERSION >= 31)
			MAX_NUM_LIGHTS = 100;
		else
			MAX_NUM_LIGHTS = 30;
	}
	
	public WorldRenderer(World world) {
		this.world = world;
		
		glClearColor(0, 0, 0, 0);// 0.4f, 0.6f, 0.9f, 0f);
		
		glEnable(GL_DEPTH_TEST);
		glDepthRange(0, 1);
		
		glDepthMask(true);
		
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glFrontFace(GL_CW);
		
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		if(GLUtils.GL_VERSION >= 32)
			glEnable(GL_DEPTH_CLAMP);
	
		loadShaders();
		
		culling = new FrustumCulling();
		
		bulletRenderer = new BulletRenderer(world.getBulletManager());
		
		loadCube();

	chunkRenderers = new HashSet<>();
		for(Chunk chunk : world.getChunkManager().getChunks()) {
			chunkRenderers.add(new ChunkRenderer(chunk, cubeVBO, indicesVBO));
		}
	}
	
	private void loadShaders() {
		int version;
		if(GLUtils.GL_VERSION >= 31)
			version = 31;
		else if(GLUtils.GL_VERSION == 30)
			version = 30;
		else
			version = 21;
		
		String shaderName = "fps" + version;
		
		HashMap<Integer,String> attributes = new HashMap<Integer,String>();
		attributes.put(0, "position");
		attributes.put(1, "normal");
		
		worldProgram = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream(GLUtils.SHADERS_ROOT_PATH + shaderName + ".vert")),
				Utils.readFully(getClass().getResourceAsStream(GLUtils.SHADERS_ROOT_PATH + shaderName + ".frag")),
		attributes);
		
		if(GLUtils.GL_VERSION >= 31)
			lightSystem = new UniformBufferLightSystem();
		else
			lightSystem = new UniformArrayLightSystem();
		
		lightSystem.setupLights(worldProgram);
		
		projectionMatrixUniform = worldProgram.getUniformLocation("projectionMatrix");
		viewMatrixUniform = worldProgram.getUniformLocation("viewMatrix");
		normalMatrixUniform = worldProgram.getUniformLocation("normalMatrix");
	}
	
	private void loadCube() {
		final short[] indices = { 0, 1, 2, 2, 3, 0 };
		
		final Vector3 normals[] = {
				new Vector3(0, 0, 1),
				new Vector3(0, 0, -1),
				new Vector3(0, 1, 0),
				new Vector3(0, -1, 0),
				new Vector3(1, 0, 0),
				new Vector3(-1, 0, 0)
		};
		
		final Vector3 unitCube[] = {
				// front face triangle 1
				new Vector3(-0.5f, 0.5f, 0.5f),
				new Vector3(0.5f, 0.5f, 0.5f),
				new Vector3(0.5f, -0.5f, 0.5f),
				// front face triangle 2
				// new Vector3(0.5f, -0.5f, 0.5f),
				new Vector3(-0.5f, -0.5f, 0.5f),
				// new Vector3(-0.5f, 0.5f, 0.5f),
				
				// back face triangle 1
				new Vector3(0.5f, 0.5f, -0.5f),
				new Vector3(-0.5f, 0.5f, -0.5f),
				new Vector3(-0.5f, -0.5f, -0.5f),
				// back face triangle 2
				// new Vector3(-0.5f, -0.5f, -0.5f),
				new Vector3(0.5f, -0.5f, -0.5f),
				// new Vector3(0.5f, 0.5f, -0.5f),
				
				// top face triangle 1
				new Vector3(-0.5f, 0.5f, -0.5f),
				new Vector3(0.5f, 0.5f, -0.5f),
				new Vector3(0.5f, 0.5f, 0.5f),
				// top face triangle 2
				// new Vector3(0.5f, 0.5f, 0.5f),
				new Vector3(-0.5f, 0.5f, 0.5f),
				// new Vector3(-0.5f, 0.5f, -0.5f),
				
				// bottom face triangle 1
				new Vector3(-0.5f, -0.5f, 0.5f),
				new Vector3(0.5f, -0.5f, 0.5f),
				new Vector3(0.5f, -0.5f, -0.5f),
				// bottom face triangle 2
				// new Vector3(0.5f, -0.5f, -0.5f),
				new Vector3(-0.5f, -0.5f, -0.5f),
				// new Vector3(-0.5f, -0.5f, 0.5f),
				
				// right face triangle 1
				new Vector3(0.5f, 0.5f, 0.5f),
				new Vector3(0.5f, 0.5f, -0.5f),
				new Vector3(0.5f, -0.5f, -0.5f),
				// right face triangle 2
				// new Vector3(0.5f, -0.5f, -0.5f),
				new Vector3(0.5f, -0.5f, 0.5f),
				// new Vector3(0.5f, 0.5f, 0.5f),
				
				// left face triangle 1
				new Vector3(-0.5f, 0.5f, -0.5f),
				new Vector3(-0.5f, 0.5f, 0.5f),
				new Vector3(-0.5f, -0.5f, 0.5f),
				// left face triangle 2
				// new Vector3(-0.5f, -0.5f, 0.5f),
				new Vector3(-0.5f, -0.5f, -0.5f),
				// new Vector3(-0.5f, 0.5f, -0.5f)
		};
		
		FloatBuffer cubeBuffer = BufferUtils.createFloatBuffer(unitCube.length * 2 * 3);
		for(int a = 0; a < unitCube.length; a++) {
			cubeBuffer.put(unitCube[a].toBuffer());
			cubeBuffer.put(normals[a / 4].toBuffer());
		}
		cubeBuffer.flip();
		
		ShortBuffer indicesBuffer = BufferUtils.createShortBuffer(indices.length * 6);
		for(int a = 0; a < 6 * indices.length; a++) {
			indicesBuffer.put((short)(indices[a % 6] + (a / indices.length) * 4));
		}
		indicesBuffer.flip();
		
		cubeVBO = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, cubeVBO);
		glBufferData(GL_ARRAY_BUFFER, cubeBuffer, GL_STATIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		indicesVBO = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesVBO);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	}
	
	private long timePassed;
	private int frameCount, chunksRendered;
	private boolean isKeyDown;
	private long renderTime;
	
	public void update(long deltaTime) {
		timePassed += deltaTime;
		
		if(Keyboard.isKeyDown(Keyboard.KEY_R)) {
			if(!isKeyDown) {
				loadShaders();
				System.out.println("Shaders reloaded.");
				isKeyDown = true;
			}
		}
		else {
			isKeyDown = false;
		}
		
		while(timePassed >= 1e9) {
			timePassed -= 1e9;
			
			if(frameCount == 0)
				continue;
			
			long triangles = 0;
			for(ChunkRenderer chunkRenderer : chunkRenderers)
				triangles += chunkRenderer.getLastCubeRenderCount();
			
			System.out.printf("Average of %d chunks, %d cubes updated in %.3f ms\n",
					chunksRendered / frameCount, triangles, (double)renderTime / (1e6 * frameCount));

			renderTime = 0;
			
			frameCount = chunksRendered = 0;
		}
	}
	
	private final Vector3 mainDiffuseColor = new Vector3(1f, 1f, 1f);
	private final Vector3 mainAmbientColor = new Vector3(0.1f, 0.1f, 0.1f);
	private final Bullet aim = new Bullet(new Vector3(), new Vector3(), 4, 0, Long.MAX_VALUE, false, new Vector3(1));
	private final Matrix4 viewMatrix = new Matrix4(), cullingProjectionMatrix = new Matrix4();
	private final Matrix3 normalMatrix = new Matrix3();
	private final MatrixStack tempStack = new MatrixStack();
	private final Vector3 cameraPosTemp = new Vector3();
	private final Vector3 renderTemp = new Vector3();
	
	public void render() {
		Camera camera = world.getCamera();
		
		// Convert Camera's Quaternion to a Matrix4 and translate it by the camera's position
		world.getCamera().getOrientation().toMatrix(viewMatrix).translate(cameraPosTemp.set(camera.getPosition()).mult(-1));
		
		worldProgram.begin();
		
		glUniformMatrix4(projectionMatrixUniform, false, camera.getProjectionMatrix().toBuffer());
		glUniformMatrix4(viewMatrixUniform, false, viewMatrix.toBuffer());
		
		normalMatrix.set(viewMatrix).inverse().transpose();
		glUniformMatrix3(normalMatrixUniform, false, normalMatrix.toBuffer());
		
		final float mainK = 0.00001f;
		lightSystem.renderLights(mainDiffuseColor, mainK, mainAmbientColor, viewMatrix, bulletRenderer);
		
		// Setting up the 6 planes that define the edges of the frustum
		culling.setupPlanes(cullingProjectionMatrix.set(camera.getProjectionMatrix()).mult(viewMatrix));
		
		long before = System.nanoTime();
		
		float halfSpacing = Chunk.SPACING * 0.5f;
		
		for(ChunkRenderer chunkRenderer : chunkRenderers) {
			Chunk chunk = chunkRenderer.getChunk();
			
			if(culling.isRectPrismInsideFrustum(renderTemp.set(chunk.getChunkInfo().chunkCornerX, chunk.getChunkInfo().chunkCornerY, -chunk.getChunkInfo().chunkCornerZ)
					.mult(Chunk.SPACING).sub(halfSpacing, halfSpacing, halfSpacing),
					Chunk.CHUNK_CUBE_WIDTH * Chunk.SPACING,
					Chunk.CHUNK_CUBE_HEIGHT * Chunk.SPACING,
					-Chunk.CHUNK_CUBE_DEPTH * Chunk.SPACING)) {
				chunkRenderer.render();
				chunksRendered++;
			}
		}
		
		renderTime += System.nanoTime() - before;
		
		worldProgram.end();
		
		bulletRenderer.render(camera.getProjectionMatrix(), tempStack.clear().setTop(viewMatrix), culling);
		
		glDepthMask(false);
		
		glDisable(GL_DEPTH_TEST);
		bulletRenderer.render(new Matrix4().clearToOrtho(-GLUtils.getWidth() / 2, GLUtils.getWidth() / 2, -GLUtils.getHeight() / 2, GLUtils.getHeight() / 2, -1, 1), new
	MatrixStack(), null, aim);
		glEnable(GL_DEPTH_TEST);
		
		glDepthMask(true);
		
		frameCount++;
	}
	
	private static interface LightSystem {
		void setupLights(ShaderProgram program);
		
		void renderLights(Vector3 diffuseColor, float mainK, Vector3 ambientColor, Matrix4 viewMatrix, BulletRenderer bulletRenderer);
	}
	
	private static class UniformArrayLightSystem implements LightSystem {
		private FloatBuffer lightsBuffer;
		private int ambientLightUniform;
		private int numLightsUniform;
		private int lightsArrayUniform;
		
		private int lastBulletCount = -1;
		
		@Override
		public void setupLights(ShaderProgram program) {
			lightsBuffer = BufferUtils.createFloatBuffer(MAX_NUM_LIGHTS * 2 * 4);
			ambientLightUniform = program.getUniformLocation("ambientLight");
			numLightsUniform = program.getUniformLocation("numberOfLights");
			lightsArrayUniform = program.getUniformLocation("lights");
			
			if(ambientLightUniform == -1)
				throw new IllegalArgumentException("Invalid program, missing ambientLight uniform");
			if(numLightsUniform == -1)
				throw new IllegalArgumentException("Invalid program, missing numberOfLights uniform");
			if(lightsArrayUniform == -1)
				throw new IllegalArgumentException("Invalid program, missing lights array uniform");
		}
		
		@Override
		public void renderLights(Vector3 diffuseColor, float mainK, Vector3 ambientColor, Matrix4 viewMatrix, BulletRenderer bulletRenderer) {
			lightsBuffer.clear();
			
			int bulletCount = bulletRenderer.getBulletLightData(viewMatrix, lightsBuffer, MAX_NUM_LIGHTS - 1);
			
			if(lastBulletCount == 0 && bulletCount == 0)
				return;
			
			lastBulletCount = bulletCount;
			
			lightsBuffer.put(0f).put(0f).put(0f); // camera position
			lightsBuffer.put(5000f); // camera light range
			lightsBuffer.put(diffuseColor.toBuffer());
			lightsBuffer.put(mainK);
			
			lightsBuffer.flip();
			
			glUniform4(lightsArrayUniform, lightsBuffer);
			glUniform1i(numLightsUniform, bulletCount + 1);
			glUniform3(ambientLightUniform, ambientColor.toBuffer());
		}
	}
	
	private static class UniformBufferLightSystem implements LightSystem {
		private FloatBuffer bulletsBuffer;
		private int lightsUniformBufferVBO;
		
		private int lastBulletCount = -1;
		
		@Override
		public void setupLights(ShaderProgram program) {
			bulletsBuffer = BufferUtils.createFloatBuffer(MAX_NUM_LIGHTS * 2 * 4);
			
			final int BUFFER_BLOCK_BINDING = 1;
			
			int lightsBlockIndex = program.getUniformBlockIndex("Lights");
			
			if(lightsBlockIndex == -1) {
				throw new IllegalArgumentException("Uniform Block 'Lights not found.");
			}
			
			glUniformBlockBinding(program.getProgram(), lightsBlockIndex, BUFFER_BLOCK_BINDING);
			
			lightsUniformBufferVBO = glGenBuffers();
			glBindBuffer(GL_UNIFORM_BUFFER, lightsUniformBufferVBO);
			glBufferData(GL_UNIFORM_BUFFER, 4 * (MAX_NUM_LIGHTS * 2 * 4 + 4), GL_STREAM_DRAW);
			glBindBufferBase(GL_UNIFORM_BUFFER, BUFFER_BLOCK_BINDING, lightsUniformBufferVBO);
			glBindBuffer(GL_UNIFORM_BUFFER, 0);
		}
		
		@Override
		public void renderLights(Vector3 diffuseColor, float mainK, Vector3 ambientColor, Matrix4 viewMatrix, BulletRenderer bulletRenderer) {
			bulletsBuffer.clear();
			
			// Fill buffer with each bullet's position as each bullet is a light source
			int bulletCount = bulletRenderer.getBulletLightData(viewMatrix, bulletsBuffer, MAX_NUM_LIGHTS - 1);
			
			if(lastBulletCount == 0 && bulletCount == 0)
				return;
			
			lastBulletCount = bulletCount;
			
			glBindBuffer(GL_UNIFORM_BUFFER, lightsUniformBufferVBO);
			
			ByteBuffer mapBuffer = glMapBuffer(GL_UNIFORM_BUFFER, GL_WRITE_ONLY, null);
			
			FloatBuffer lightsBuffer = mapBuffer.asFloatBuffer();
			
			bulletsBuffer.flip();
			
			lightsBuffer.put(ambientColor.toBuffer());
			lightsBuffer.put(bulletCount + 1);
			
			lightsBuffer.put(bulletsBuffer);
			
			lightsBuffer.put(0).put(0).put(0); // camera position
			lightsBuffer.put(5000); // camera light range
			lightsBuffer.put(diffuseColor.toBuffer());
			lightsBuffer.put(mainK);
			
			lightsBuffer.flip();
			
			glUnmapBuffer(GL_UNIFORM_BUFFER);
			
			glBindBuffer(GL_UNIFORM_BUFFER, 0);
		}
	}
}
