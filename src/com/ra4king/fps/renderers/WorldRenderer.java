package com.ra4king.fps.renderers;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glUniform4;
import static org.lwjgl.opengl.GL20.glUniformMatrix3;
import static org.lwjgl.opengl.GL20.glUniformMatrix4;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.*;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.OpenGLException;

import com.ra4king.fps.Camera;
import com.ra4king.fps.OpenGLWorlds;
import com.ra4king.fps.actors.Actor;
import com.ra4king.fps.actors.Bullet;
import com.ra4king.fps.actors.Portal;
import com.ra4king.fps.world.Chunk;
import com.ra4king.fps.world.World;
import com.ra4king.opengl.util.PNGDecoder;
import com.ra4king.opengl.util.PNGDecoder.Format;
import com.ra4king.opengl.util.ShaderProgram;
import com.ra4king.opengl.util.Stopwatch;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.buffers.BufferStorage;
import com.ra4king.opengl.util.math.Matrix3;
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.MatrixStack;
import com.ra4king.opengl.util.math.Vector3;
import com.ra4king.opengl.util.math.Vector4;
import com.ra4king.opengl.util.render.RenderUtils;
import com.ra4king.opengl.util.render.RenderUtils.FrustumCulling;

/**
 * @author Roi Atalla
 */
public class WorldRenderer {
	private static final int MAX_NUM_LIGHTS = 100;
	
	private OpenGLWorlds game;
	private World world;
	
	private ShaderProgram blocksProgram;
	private int projectionMatrixUniform;
	private int viewMatrixUniform;
	private int normalMatrixUniform;
	
	private ShaderProgram deferredProgram;
	private int deferredFBO, deferredVAO;
	
	private static final int CUBE_TEXTURE_BINDING = 0;
	private static final int CAMERA_POSITIONS_TEXTURE_BINDING = 1;
	private static final int NORMALS_TEXTURE_BINDING = 2;
	private static final int TEX_COORDS_TEXTURE_BINDING = 3;
	private static final int DEPTH_TEXTURE_BINDING = 4;
	
	private static int cubeTexture;
	private int cameraPositionsTexture, normalsTexture, texCoordsTexture, depthTexture;
	
	private FrustumCulling culling;
	
	private IntBuffer commandsBuffer;
	
	private int chunkVAO, commandsVBO;
	private ChunkRenderer[] chunkRenderers;
	private BufferStorage chunkRendererStorage;
	
	private int chunksRendered, blocksRendered;
	
	private BulletRenderer bulletRenderer;
	private LightSystem lightSystem;
	
	private ArrayList<PortalRenderer> portalRenderers;
	
	static {
		cubeTexture = loadTexture("crate.png");
	}
	
	public WorldRenderer(OpenGLWorlds game, World world) {
		this.game = game;
		this.world = world;
		
		if(RenderUtils.GL_VERSION >= 32) {
			glEnable(GL_DEPTH_CLAMP);
		}
		
		culling = new FrustumCulling();
		
		loadShaders();
		
		bulletRenderer = new BulletRenderer(world.getBulletManager());
		
		setupBlockVAO();
		
		setupDeferredFBO();
		setupDeferredVAO();
		
		final int COMMANDS_BUFFER_SIZE = chunkRenderers.length * 4 * 4;
		commandsBuffer = BufferUtils.createIntBuffer(COMMANDS_BUFFER_SIZE / 4);
		
		commandsVBO = glGenBuffers();
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, commandsVBO);
		glBufferData(GL_DRAW_INDIRECT_BUFFER, COMMANDS_BUFFER_SIZE, GL_STREAM_DRAW);
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
	}
	
	public void loadActors() {
		portalRenderers = new ArrayList<>();
		for(Actor actor : world.getActors()) {
			if(actor instanceof Portal) {
				Portal portal = (Portal)actor;
				portalRenderers.add(new PortalRenderer(portal, game.getRenderer(portal.getDestWorld())));
			}
		}
	}
	
	private void loadShaders() {
		blocksProgram = new ShaderProgram(Utils.readFully(Resources.getInputStream("shaders/blocks.vert")),
		                                   Utils.readFully(Resources.getInputStream("shaders/blocks.geom")),
		                                   Utils.readFully(Resources.getInputStream("shaders/blocks.frag")));
		
		projectionMatrixUniform = blocksProgram.getUniformLocation("projectionMatrix");
		viewMatrixUniform = blocksProgram.getUniformLocation("viewMatrix");
		normalMatrixUniform = blocksProgram.getUniformLocation("normalMatrix");
		
		blocksProgram.begin();
		glUniform1f(blocksProgram.getUniformLocation("cubeSize"), Chunk.BLOCK_SIZE);
		blocksProgram.end();
		
		deferredProgram = new ShaderProgram(Utils.readFully(Resources.getInputStream("shaders/deferred.vert")),
		                                     Utils.readFully(Resources.getInputStream("shaders/deferred.frag")));
		
		lightSystem = new UniformBufferLightSystem();
		lightSystem.setupLights(deferredProgram);
	}
	
	private static int loadTexture(String texName) {
		try(InputStream in = Resources.getInputStream("textures/" + texName)) {
			PNGDecoder decoder = new PNGDecoder(in);
			
			int width = decoder.getWidth();
			int height = decoder.getHeight();
			
			ByteBuffer data = BufferUtils.createByteBuffer(width * height * 4);
			decoder.decode(data, width * 4, Format.RGBA);
			data.flip();
			
			int tex = glGenTextures();
			glBindTexture(GL_TEXTURE_2D, tex);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, glGetInteger(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT));
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
			glGenerateMipmap(GL_TEXTURE_2D);
			glBindTexture(GL_TEXTURE_2D, 0);
			
			return tex;
		}
		catch(Exception exc) {
			throw new RuntimeException("Failed to load " + texName, exc);
		}
	}
	
	private void setupBlockVAO() {
		final int DATA_VBO_SIZE = world.getChunkManager().getChunks().length * ChunkRenderer.CHUNK_DATA_SIZE;
		
		chunkVAO = RenderUtils.glGenVertexArrays();
		RenderUtils.glBindVertexArray(chunkVAO);
		
		chunkRendererStorage = new BufferStorage(GL_ARRAY_BUFFER, DATA_VBO_SIZE, true, 3);
		System.out.println("DATA_VBO_SIZE=" + DATA_VBO_SIZE);
		
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_UNSIGNED_INT, false, 4 * 4, 0);
		
		glEnableVertexAttribArray(1);
		glVertexAttribIPointer(1, 1, GL_UNSIGNED_INT, 4 * 4, 3 * 4);
		
		RenderUtils.glBindVertexArray(0);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		Chunk[] chunks = world.getChunkManager().getChunks();
		chunkRenderers = new ChunkRenderer[chunks.length];
		for(int i = 0; i < chunkRenderers.length; i++) {
			chunkRenderers[i] = new ChunkRenderer(chunks[i], chunkRendererStorage, i);
		}
	}
	
	private void setupDeferredFBO() {
		if(deferredFBO != 0) {
			glDeleteFramebuffers(deferredFBO);
			
			IntBuffer texs = BufferUtils.createIntBuffer(4).put(cameraPositionsTexture).put(normalsTexture).put(texCoordsTexture).put(depthTexture);
			texs.flip();
			glDeleteTextures(texs);
		}
		
		deferredFBO = glGenFramebuffers();
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, deferredFBO);
		
		cameraPositionsTexture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, cameraPositionsTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB32F, RenderUtils.getWidth(), RenderUtils.getHeight(), 0, GL_RGB, GL_FLOAT, (ByteBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, cameraPositionsTexture, 0);
		
		normalsTexture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, normalsTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB32F, RenderUtils.getWidth(), RenderUtils.getHeight(), 0, GL_RGB, GL_FLOAT, (ByteBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, normalsTexture, 0);
		
		texCoordsTexture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, texCoordsTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB32F, RenderUtils.getWidth(), RenderUtils.getHeight(), 0, GL_RG, GL_FLOAT, (ByteBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, texCoordsTexture, 0);
		
		depthTexture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, depthTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, RenderUtils.getWidth(), RenderUtils.getHeight(), 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0);
		
		glBindTexture(GL_TEXTURE_2D, 0);
		
		IntBuffer drawBuffers = BufferUtils.createIntBuffer(3).put(new int[] {
		  GL_COLOR_ATTACHMENT0,
		  GL_COLOR_ATTACHMENT1,
		  GL_COLOR_ATTACHMENT2,
		  });
		drawBuffers.flip();
		glDrawBuffers(drawBuffers);
		
		int fboStatus = glCheckFramebufferStatus(GL_FRAMEBUFFER);
		if(fboStatus != GL_FRAMEBUFFER_COMPLETE) {
			throw new OpenGLException("FBO not complete, status: " + fboStatus);
		}
		
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
		
		deferredProgram.begin();
		glUniform1i(deferredProgram.getUniformLocation("cubeTexture"), CUBE_TEXTURE_BINDING);
		glUniform1i(deferredProgram.getUniformLocation("cameraPositions"), CAMERA_POSITIONS_TEXTURE_BINDING);
		glUniform1i(deferredProgram.getUniformLocation("normals"), NORMALS_TEXTURE_BINDING);
		glUniform1i(deferredProgram.getUniformLocation("texCoords"), TEX_COORDS_TEXTURE_BINDING);
		glUniform1i(deferredProgram.getUniformLocation("depth"), DEPTH_TEXTURE_BINDING);
		deferredProgram.end();
	}
	
	private void setupDeferredVAO() {
		FloatBuffer verts = BufferUtils.createFloatBuffer(8).put(new float[] {
		  1, 1,
		  1, -1,
		  -1, 1,
		  -1, -1
		});
		verts.flip();
		
		int vbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, verts, GL_STATIC_DRAW);
		
		deferredVAO = RenderUtils.glGenVertexArrays();
		RenderUtils.glBindVertexArray(deferredVAO);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
		RenderUtils.glBindVertexArray(0);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}
	
	public int getChunksRenderedCount() {
		return chunksRendered;
	}
	
	public int getBlocksRenderedCount() {
		return blocksRendered;
	}
	
	public void resized() {
		setupDeferredFBO();
		
		portalRenderers.forEach(PortalRenderer::resized);
	}
	
	public void update(long deltaTime) {
		for(PortalRenderer portalRenderer : portalRenderers)
			portalRenderer.update(deltaTime);
	}
	
	private final Bullet aim = new Bullet(new Vector3(), new Vector3(), 4, 0, Long.MAX_VALUE, false, new Vector3(1));
	
	private final MatrixStack tempStack = new MatrixStack();
	
	private final DrawArraysIndirectCommand command = new DrawArraysIndirectCommand();
	
	public void render(Vector4 clipPlane, Portal surroundingPortal, int currentFbo, Camera camera) {
		glClearColor(0.4f, 0.6f, 0.9f, 0f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		Stopwatch.start("WorldRender Setup");
		
		// Convert Camera's Quaternion to a Matrix4 and translate it by the camera's position
		Matrix4 viewMatrix = camera.getOrientation().toMatrix(new Matrix4());
		viewMatrix.translate(new Vector3(camera.getPosition()).mult(-1));
		
		// Setting up the 6 planes that define the edges of the frustum
		culling.setupPlanes(new Matrix4(camera.getProjectionMatrix()).mult(viewMatrix));
		
		Stopwatch.stop();
		
		Stopwatch.start("ChunkRenderers");
		
		chunksRendered = 0;
		blocksRendered = 0;
		
		float halfSpacing = Chunk.SPACING * 0.5f;
		
		chunkRendererStorage.nextBuffer();
		for(ChunkRenderer chunkRenderer : chunkRenderers) {
			chunkRenderer.update();
		}
		
		commandsBuffer.clear();
		
		int currentOffset = chunkRendererStorage.getBufferIndex() * chunkRenderers.length * Chunk.TOTAL_BLOCKS;
		
		command.baseInstance = 0;
		command.instanceCount = 1;
		
		for(ChunkRenderer chunkRenderer : chunkRenderers) {
			Chunk chunk = chunkRenderer.getChunk();
			
			if(culling.isRectPrismInsideFrustum(new Vector3(chunk.getCornerX(), chunk.getCornerY(), -chunk.getCornerZ())
			                                      .mult(Chunk.SPACING).sub(halfSpacing, halfSpacing, -halfSpacing),
			  Chunk.CHUNK_BLOCK_WIDTH * Chunk.SPACING,
			  Chunk.CHUNK_BLOCK_HEIGHT * Chunk.SPACING,
			  -Chunk.CHUNK_BLOCK_DEPTH * Chunk.SPACING)) {
				if(chunkRenderer.render(command, currentOffset)) {
					commandsBuffer.put(command.toBuffer());
					
					chunksRendered++;
					blocksRendered += chunkRenderer.getLastCubeRenderCount();
				}
			}
		}
		
		commandsBuffer.flip();
		
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, commandsVBO);
		glBufferData(GL_DRAW_INDIRECT_BUFFER, commandsBuffer, GL_STREAM_DRAW);
		
		{
			glBindFramebuffer(GL_DRAW_FRAMEBUFFER, deferredFBO);
			
			glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			
			glDisable(GL_BLEND);
			
			blocksProgram.begin();
			
			glUniformMatrix4(projectionMatrixUniform, false, camera.getProjectionMatrix().toBuffer());
			glUniformMatrix4(viewMatrixUniform, false, viewMatrix.toBuffer());
			
			glUniformMatrix3(normalMatrixUniform, false, new Matrix3().set4x4(viewMatrix).inverse().transpose().toBuffer());
			
			if(clipPlane != null) {
				glEnable(GL_CLIP_DISTANCE0);
				glUniform4(blocksProgram.getUniformLocation("clipPlane"), clipPlane.toBuffer());
			}
			
			RenderUtils.glBindVertexArray(chunkVAO);
			glMultiDrawArraysIndirect(GL_POINTS, 0, chunksRendered, 0);
			
			glDisable(GL_CLIP_DISTANCE0);
		}
		
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
		
		{
			glBindFramebuffer(GL_DRAW_FRAMEBUFFER, currentFbo);
			
			glActiveTexture(GL_TEXTURE0 + CUBE_TEXTURE_BINDING);
			glBindTexture(GL_TEXTURE_2D, cubeTexture);
			
			glActiveTexture(GL_TEXTURE0 + CAMERA_POSITIONS_TEXTURE_BINDING);
			glBindTexture(GL_TEXTURE_2D, cameraPositionsTexture);
			
			glActiveTexture(GL_TEXTURE0 + NORMALS_TEXTURE_BINDING);
			glBindTexture(GL_TEXTURE_2D, normalsTexture);
			
			glActiveTexture(GL_TEXTURE0 + TEX_COORDS_TEXTURE_BINDING);
			glBindTexture(GL_TEXTURE_2D, texCoordsTexture);
			
			glActiveTexture(GL_TEXTURE0 + DEPTH_TEXTURE_BINDING);
			glBindTexture(GL_TEXTURE_2D, depthTexture);
			
			glEnable(GL_BLEND);
			
			deferredProgram.begin();
			
			final float mainK = 0.001f;
			lightSystem.renderLights(new Vector3(0.5f, 0.5f, 0.5f), mainK, new Vector3(0.01f, 0.01f, 0.01f), viewMatrix, bulletRenderer);
			
			glProvokingVertex(GL_FIRST_VERTEX_CONVENTION);
			
			RenderUtils.glBindVertexArray(deferredVAO);
			glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		}
		
		Stopwatch.stop();
		
		for(PortalRenderer portalRenderer : portalRenderers) {
			if(portalRenderer.getPortal().getDestPortal() == surroundingPortal) {
				continue;
			}
			
			portalRenderer.render(currentFbo, camera, viewMatrix, culling);
		}
		
		Stopwatch.start("BulletRenderer");
		
		bulletRenderer.render(camera.getProjectionMatrix(), tempStack.setTop(viewMatrix), culling);
		
		glDisable(GL_DEPTH_TEST);
		bulletRenderer.render(new Matrix4().clearToOrtho(-RenderUtils.getWidth() / 2, RenderUtils.getWidth() / 2, -RenderUtils.getHeight() / 2, RenderUtils.getHeight() / 2, -1, 1), new MatrixStack(), null, aim);
		glEnable(GL_DEPTH_TEST);
		
		Stopwatch.stop();
	}
	
	public static class DrawArraysIndirectCommand {
		public int count;
		public int instanceCount;
		public int first;
		public int baseInstance;
		
		private static final IntBuffer commandBuffer = BufferUtils.createIntBuffer(5);
		
		public IntBuffer toBuffer() {
			commandBuffer.clear();
			commandBuffer.put(count).put(instanceCount).put(first).put(baseInstance).flip();
			return commandBuffer;
		}
	}
	
	private interface LightSystem {
		void setupLights(ShaderProgram program);
		
		void renderLights(Vector3 diffuseColor, float mainK, Vector3 ambientColor, Matrix4 viewMatrix, BulletRenderer bulletRenderer);
	}
	
	private static class UniformBufferLightSystem implements LightSystem {
		private static final int LIGHTS_UNIFORM_BUFFER_SIZE = 4 * (MAX_NUM_LIGHTS * 2 * 4 + 4);
		private static int BUFFER_BLOCK_BINDING_INIT = 1;
		
		private final int BUFFER_BLOCK_BINDING;
		
		private FloatBuffer bulletsBuffer;
		private int lightsUniformBufferVBO;
		
		private int lastBulletCount = -1;
		
		public UniformBufferLightSystem() {
			BUFFER_BLOCK_BINDING = BUFFER_BLOCK_BINDING_INIT++;
		}
		
		@Override
		public void setupLights(ShaderProgram program) {
			bulletsBuffer = BufferUtils.createFloatBuffer(MAX_NUM_LIGHTS * 2 * 4);
			
			int lightsBlockIndex = program.getUniformBlockIndex("Lights");
			
			if(lightsBlockIndex == -1) {
				throw new IllegalArgumentException("Uniform Block 'Lights' not found.");
			}
			
			glUniformBlockBinding(program.getProgram(), lightsBlockIndex, BUFFER_BLOCK_BINDING);
			
			lightsUniformBufferVBO = glGenBuffers();
			glBindBuffer(GL_UNIFORM_BUFFER, lightsUniformBufferVBO);
			glBufferData(GL_UNIFORM_BUFFER, LIGHTS_UNIFORM_BUFFER_SIZE, GL_STREAM_DRAW);
			glBindBuffer(GL_UNIFORM_BUFFER, 0);
		}
		
		@Override
		public void renderLights(Vector3 diffuseColor, float mainK, Vector3 ambientColor, Matrix4 viewMatrix, BulletRenderer bulletRenderer) {
			Stopwatch.start("LightSystem render UBO");
			
			try {
				bulletsBuffer.clear();
				
				// Fill buffer with each bullet's position as each bullet is a light source
				int bulletCount = bulletRenderer.getBulletLightData(viewMatrix, bulletsBuffer, MAX_NUM_LIGHTS - 1);
				
				if(lastBulletCount == 0 && bulletCount == 0) {
					return;
				}
				
				lastBulletCount = bulletCount;
				
				glBindBufferBase(GL_UNIFORM_BUFFER, BUFFER_BLOCK_BINDING, lightsUniformBufferVBO);
				
				glBindBuffer(GL_UNIFORM_BUFFER, lightsUniformBufferVBO);
				ByteBuffer lightsMappedBuffer = glMapBufferRange(GL_UNIFORM_BUFFER, 0, LIGHTS_UNIFORM_BUFFER_SIZE,
				  GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT | GL_MAP_UNSYNCHRONIZED_BIT, null);
				
				if(lightsMappedBuffer == null) {
					Utils.checkGLError("lights map buffer");
					throw new OpenGLException("lightsMappedBuffer == null ... no GL error?!");
				}
				
				FloatBuffer lightsBuffer = lightsMappedBuffer.asFloatBuffer();
				
				lightsBuffer.put(new Vector3(0).toBuffer());//ambientColor.toBuffer());
				lightsBuffer.put(bulletCount + 1);
				
				bulletsBuffer.flip();
				lightsBuffer.put(bulletsBuffer);
				
				lightsBuffer.put(0).put(0).put(0); // camera position
				lightsBuffer.put(5000);
				lightsBuffer.put(diffuseColor.toBuffer());
				lightsBuffer.put(mainK);
				
				glUnmapBuffer(GL_UNIFORM_BUFFER);
				
				glBindBuffer(GL_UNIFORM_BUFFER, 0);
			}
			finally {
				Stopwatch.stop();
			}
		}
	}
}
