package com.ra4king.fps.renderers;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
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
import java.nio.ShortBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GLSync;
import org.lwjgl.opengl.OpenGLException;

import com.ra4king.fps.Camera;
import com.ra4king.fps.GLUtils;
import com.ra4king.fps.GLUtils.FrustumCulling;
import com.ra4king.fps.actors.Bullet;
import com.ra4king.fps.world.Chunk;
import com.ra4king.fps.world.World;
import com.ra4king.opengl.util.PNGDecoder;
import com.ra4king.opengl.util.PNGDecoder.Format;
import com.ra4king.opengl.util.ShaderProgram;
import com.ra4king.opengl.util.Stopwatch;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.buffers.BufferStorage;
import com.ra4king.opengl.util.buffers.GLBuffer;
import com.ra4king.opengl.util.math.Matrix3;
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.MatrixStack;
import com.ra4king.opengl.util.math.Vector2;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author Roi Atalla
 */
public class WorldRenderer {
	private static final int MAX_NUM_LIGHTS;
	
	private World world;
	
	private ShaderProgram blocksProgram;
	private int projectionMatrixUniform;
	private int viewMatrixUniform;
	private int normalMatrixUniform;
	
	private ShaderProgram deferredProgram;
	private int resolutionUniform;
	private int deferredFBO, deferredVAO;
	
	private FrustumCulling culling;
	
	private final int COMMANDS_BUFFER_SIZE;
	
	private int chunkVAO, cubeVBO, indicesVBO, commandsVBO;
	private ChunkRenderer[] chunkRenderers;
	private GLSync chunkRenderFence;
	
	private BulletRenderer bulletRenderer;
	private LightSystem lightSystem;
	
	static {
		if(GLUtils.GL_VERSION >= 31) {
			MAX_NUM_LIGHTS = 100;
		} else {
			MAX_NUM_LIGHTS = 50;
		}
	}
	
	public WorldRenderer(World world) {
		this.world = world;
		
		glEnable(GL_DEPTH_TEST);
		
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glFrontFace(GL_CW);
		
		if(GLUtils.GL_VERSION >= 32) {
			glEnable(GL_DEPTH_CLAMP);
		}
		
		culling = new FrustumCulling();
		
		loadShaders();
		
		bulletRenderer = new BulletRenderer(world.getBulletManager());
		
		loadCube();
		setupBlockVAO();
		
		setupDeferredFBO();
		setupDeferredVAO();
		
		COMMANDS_BUFFER_SIZE = chunkRenderers.length * 5 * 4;
		
		commandsVBO = glGenBuffers();
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, commandsVBO);
		glBufferData(GL_DRAW_INDIRECT_BUFFER, COMMANDS_BUFFER_SIZE, GL_STREAM_DRAW);
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
	}
	
	private void loadShaders() {
		blocksProgram = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream(GLUtils.RESOURCES_ROOT_PATH + "shaders/blocks.vert")),
				Utils.readFully(getClass().getResourceAsStream(GLUtils.RESOURCES_ROOT_PATH + "shaders/blocks.frag")));
		
		projectionMatrixUniform = blocksProgram.getUniformLocation("projectionMatrix");
		viewMatrixUniform = blocksProgram.getUniformLocation("viewMatrix");
		normalMatrixUniform = blocksProgram.getUniformLocation("normalMatrix");
		
		blocksProgram.begin();
		glUniform1f(blocksProgram.getUniformLocation("cubeSize"), Chunk.CUBE_SIZE);
		blocksProgram.end();
		
		deferredProgram = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream(GLUtils.RESOURCES_ROOT_PATH + "shaders/deferred.vert")),
				Utils.readFully(getClass().getResourceAsStream(GLUtils.RESOURCES_ROOT_PATH + "shaders/deferred.frag")));
		
		lightSystem = new UniformBufferLightSystem();
		lightSystem.setupLights(deferredProgram);
		
		resolutionUniform = deferredProgram.getUniformLocation("resolution");
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
		
		final Vector2 texCoords[] = {
				new Vector2(0, 1),
				new Vector2(1, 1),
				new Vector2(1, 0),
				new Vector2(0, 0)
		};
		
		final Vector3 unitCube[] = {
				// front
				new Vector3(-0.5f, 0.5f, 0.5f),
				new Vector3(0.5f, 0.5f, 0.5f),
				new Vector3(0.5f, -0.5f, 0.5f),
				new Vector3(-0.5f, -0.5f, 0.5f),
				
				// back
				new Vector3(0.5f, 0.5f, -0.5f),
				new Vector3(-0.5f, 0.5f, -0.5f),
				new Vector3(-0.5f, -0.5f, -0.5f),
				new Vector3(0.5f, -0.5f, -0.5f),
				
				// top
				new Vector3(-0.5f, 0.5f, -0.5f),
				new Vector3(0.5f, 0.5f, -0.5f),
				new Vector3(0.5f, 0.5f, 0.5f),
				new Vector3(-0.5f, 0.5f, 0.5f),
				
				// bottom
				new Vector3(-0.5f, -0.5f, 0.5f),
				new Vector3(0.5f, -0.5f, 0.5f),
				new Vector3(0.5f, -0.5f, -0.5f),
				new Vector3(-0.5f, -0.5f, -0.5f),
				
				// right
				new Vector3(0.5f, 0.5f, 0.5f),
				new Vector3(0.5f, 0.5f, -0.5f),
				new Vector3(0.5f, -0.5f, -0.5f),
				new Vector3(0.5f, -0.5f, 0.5f),
				
				// left
				new Vector3(-0.5f, 0.5f, -0.5f),
				new Vector3(-0.5f, 0.5f, 0.5f),
				new Vector3(-0.5f, -0.5f, 0.5f),
				new Vector3(-0.5f, -0.5f, -0.5f)
		};
		
		// 2 vec3s and 1 vec2
		FloatBuffer cubeBuffer = BufferUtils.createFloatBuffer(unitCube.length * (2 * 3 + 2));
		for(int a = 0; a < unitCube.length; a++) {
			cubeBuffer.put(unitCube[a].toBuffer());
			cubeBuffer.put(normals[a / 4].toBuffer());
			cubeBuffer.put(texCoords[a % 4].toBuffer());
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
	
	private int loadCubeTexture(String texName) {
		try(InputStream in = getClass().getResourceAsStream(GLUtils.RESOURCES_ROOT_PATH + "textures/" + texName)) {
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
		} catch(Exception exc) {
			throw new RuntimeException("Failed to load " + texName, exc);
		}
	}
	
	private void setupBlockVAO() {
		final int DATA_VBO_SIZE = world.getChunkManager().getChunks().length * ChunkRenderer.CHUNK_DATA_SIZE;
		
		chunkVAO = glGenVertexArrays();
		glBindVertexArray(chunkVAO);
		
		glBindBuffer(GL_ARRAY_BUFFER, cubeVBO);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesVBO);
		
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, (2 * 3 + 2) * 4, 0);
		
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 3, GL_FLOAT, false, (2 * 3 + 2) * 4, 3 * 4);
		
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(2, 2, GL_FLOAT, false, (2 * 3 + 2) * 4, 2 * 3 * 4);
		
		GLBuffer chunkBuffer = new BufferStorage(GL_ARRAY_BUFFER, DATA_VBO_SIZE, true, 3);
		
		glEnableVertexAttribArray(3);
		glVertexAttribPointer(3, 3, GL_UNSIGNED_INT, false, 4 * 4, 0);
		GLUtils.glVertexAttribDivisor(3, 1);
		
		glEnableVertexAttribArray(4);
		glVertexAttribIPointer(4, 1, GL_UNSIGNED_INT, 4 * 4, 3 * 4);
		GLUtils.glVertexAttribDivisor(4, 1);
		
		GLUtils.glBindVertexArray(0);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		
		Chunk[] chunks = world.getChunkManager().getChunks();
		chunkRenderers = new ChunkRenderer[chunks.length];
		for(int i = 0; i < chunkRenderers.length; i++) {
			chunkRenderers[i] = new ChunkRenderer(chunks[i], chunkBuffer, i);
		}
	}
	
	private void setupDeferredFBO() {
		deferredFBO = glGenFramebuffers();
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, deferredFBO);
		
		int cameraPositionsTexture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, cameraPositionsTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB32F, GLUtils.getWidth(), GLUtils.getHeight(), 0, GL_RGB, GL_FLOAT, (ByteBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, cameraPositionsTexture, 0);
		
		int normalsTexture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, normalsTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB32F, GLUtils.getWidth(), GLUtils.getHeight(), 0, GL_RGB, GL_FLOAT, (ByteBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, normalsTexture, 0);
		
		int texCoordsTexture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, texCoordsTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB32F, GLUtils.getWidth(), GLUtils.getHeight(), 0, GL_RGB, GL_FLOAT, (ByteBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, texCoordsTexture, 0);
		
		int depthTexture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, depthTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, GLUtils.getWidth(), GLUtils.getHeight(), 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer)null);
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
		
		final int CUBE_TEXTURE_BINDING = 0;
		final int CAMERA_POSITIONS_TEXTURE_BINDING = 1;
		final int NORMALS_TEXTURE_BINDING = 2;
		final int TEX_COORDS_TEXTURE_BINDING = 3;
		final int DEPTH_TEXTURE_BINDING = 4;
		
		glActiveTexture(GL_TEXTURE0 + CUBE_TEXTURE_BINDING);
		glBindTexture(GL_TEXTURE_2D, loadCubeTexture("crate.png"));
		
		glActiveTexture(GL_TEXTURE0 + CAMERA_POSITIONS_TEXTURE_BINDING);
		glBindTexture(GL_TEXTURE_2D, cameraPositionsTexture);
		
		glActiveTexture(GL_TEXTURE0 + NORMALS_TEXTURE_BINDING);
		glBindTexture(GL_TEXTURE_2D, normalsTexture);
		
		glActiveTexture(GL_TEXTURE0 + TEX_COORDS_TEXTURE_BINDING);
		glBindTexture(GL_TEXTURE_2D, texCoordsTexture);
		
		glActiveTexture(GL_TEXTURE0 + DEPTH_TEXTURE_BINDING);
		glBindTexture(GL_TEXTURE_2D, depthTexture);
		
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
		
		deferredVAO = glGenVertexArrays();
		glBindVertexArray(deferredVAO);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
		glBindVertexArray(0);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}

	public void resized() {
		setupDeferredFBO();
	}
	
	private long timePassed;
	private int lastChunksRendered;
	
	public void update(long deltaTime) {
		timePassed += deltaTime;
		
		while(timePassed >= 1e9) {
			timePassed -= 1e9;
			
			if(timePassed >= 1e9) {
				continue;
			}
			
			long cubes = 0;
			for(ChunkRenderer chunkRenderer : chunkRenderers) {
				cubes += chunkRenderer.getLastCubeRenderCount();
			}
			
			System.out.printf("Rendering %d chunks, %d cubes\n", lastChunksRendered, cubes);
		}
	}
	
	private final Vector3 mainDiffuseColor = new Vector3(0.5f, 0.5f, 0.5f);
	private final Vector3 mainAmbientColor = new Vector3(0.01f, 0.01f, 0.01f);
	
	private final Bullet aim = new Bullet(new Vector3(), new Vector3(), 4, 0, Long.MAX_VALUE, false, new Vector3(1));
	
	private final MatrixStack tempStack = new MatrixStack();
	
	private final Matrix4 viewMatrix = new Matrix4(), cullingProjectionMatrix = new Matrix4();
	private final Matrix3 normalMatrix = new Matrix3();
	
	private final Vector3 cameraPosTemp = new Vector3();
	private final Vector3 renderTemp = new Vector3();
	
	private final DrawElementsIndirectCommand command = new DrawElementsIndirectCommand();
	
	{
		command.count = 6 * 6; // elements count
		command.firstIndex = 0;
		command.baseVertex = 0;
	}
	
	public void render() {
		Stopwatch.start("WorldRender Setup");
		
		Camera camera = world.getCamera();
		
		// Convert Camera's Quaternion to a Matrix4 and translate it by the camera's position
		camera.getOrientation().toMatrix(viewMatrix).translate(cameraPosTemp.set(camera.getPosition()).mult(-1));
		
		// Setting up the 6 planes that define the edges of the frustum
		culling.setupPlanes(cullingProjectionMatrix.set(camera.getProjectionMatrix()).mult(viewMatrix));
		
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, commandsVBO);
		ByteBuffer commandsMappedBuffer = glMapBufferRange(GL_DRAW_INDIRECT_BUFFER, 0, COMMANDS_BUFFER_SIZE,
				GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT | GL_MAP_UNSYNCHRONIZED_BIT, null);
		if(commandsMappedBuffer == null) {
			Utils.checkGLError("commands mapped buffer");
			throw new OpenGLException("commandsMappedBuffer == null ... no GL Error?");
		}
		
		IntBuffer commandsBuffer = commandsMappedBuffer.asIntBuffer();
		
		Stopwatch.stop();
		
		Stopwatch.start("ChunkRenderers");
		
		int chunksRendered = 0;
		float halfSpacing = Chunk.SPACING * 0.5f;
		
		for(ChunkRenderer chunkRenderer : chunkRenderers) {
			Chunk chunk = chunkRenderer.getChunk();
			
			if(culling.isRectPrismInsideFrustum(renderTemp.set(chunk.getCornerX(), chunk.getCornerY(), -chunk.getCornerZ())
					.mult(Chunk.SPACING).sub(halfSpacing, halfSpacing, -halfSpacing),
					Chunk.CHUNK_CUBE_WIDTH * Chunk.SPACING,
					Chunk.CHUNK_CUBE_HEIGHT * Chunk.SPACING,
					-Chunk.CHUNK_CUBE_DEPTH * Chunk.SPACING)) {
				if(chunkRenderer.render(command, chunkRenderFence)) {
					commandsBuffer.put(command.toBuffer());
					
					chunksRendered++;
				}
			}
		}
		
		lastChunksRendered = chunksRendered;
		
		glUnmapBuffer(GL_DRAW_INDIRECT_BUFFER);
		
		{
			glBindFramebuffer(GL_DRAW_FRAMEBUFFER, deferredFBO);
			
			glClearColor(0, 0, 0, 0);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			
			glDisable(GL_BLEND);

			blocksProgram.begin();
			
			glUniformMatrix4(projectionMatrixUniform, false, camera.getProjectionMatrix().toBuffer());
			glUniformMatrix4(viewMatrixUniform, false, viewMatrix.toBuffer());
			
			normalMatrix.set(viewMatrix).inverse().transpose();
			glUniformMatrix3(normalMatrixUniform, false, normalMatrix.toBuffer());
			
			GLUtils.glBindVertexArray(chunkVAO);
			glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_SHORT, 0, chunksRendered, 0);
			
			chunkRenderFence = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
		}
		
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
		
		{
			glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
			
			glClearColor(0.4f, 0.6f, 0.9f, 0f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			
			glEnable(GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			
			deferredProgram.begin();
			glUniform2f(resolutionUniform, GLUtils.getWidth(), GLUtils.getHeight());
			
			final float mainK = 0.00001f;
			lightSystem.renderLights(mainDiffuseColor, mainK, mainAmbientColor, viewMatrix, bulletRenderer);
			
			GLUtils.glBindVertexArray(deferredVAO);
			glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		}
		
		Stopwatch.stop();
		
		Stopwatch.start("BulletRenderer");
		
		bulletRenderer.render(camera.getProjectionMatrix(), tempStack.clear().setTop(viewMatrix), culling);
		
		glDisable(GL_DEPTH_TEST);
		bulletRenderer.render(new Matrix4().clearToOrtho(-GLUtils.getWidth() / 2, GLUtils.getWidth() / 2, -GLUtils.getHeight() / 2, GLUtils.getHeight() / 2, -1, 1), new MatrixStack(), null, aim);
		glEnable(GL_DEPTH_TEST);
		
		Stopwatch.stop();
	}
	
	public static class DrawElementsIndirectCommand {
		public int count;
		public int instanceCount;
		public int firstIndex;
		public int baseVertex;
		public int baseInstance;
		
		private static final IntBuffer commandBuffer = BufferUtils.createIntBuffer(5);
		
		public IntBuffer toBuffer() {
			commandBuffer.clear();
			commandBuffer.put(count).put(instanceCount).put(firstIndex).put(baseVertex).put(baseInstance).flip();
			return commandBuffer;
		}
	}
	
	private static interface LightSystem {
		void setupLights(ShaderProgram program);
		
		void renderLights(Vector3 diffuseColor, float mainK, Vector3 ambientColor, Matrix4 viewMatrix, BulletRenderer bulletRenderer);
	}
	
	private static class UniformBufferLightSystem implements LightSystem {
		private static final int LIGHTS_UNIFORM_BUFFER_SIZE = 4 * (MAX_NUM_LIGHTS * 2 * 4 + 4);
		
		private FloatBuffer bulletsBuffer;
		private int lightsUniformBufferVBO;
		
		private int lastBulletCount = -1;
		
		@Override
		public void setupLights(ShaderProgram program) {
			bulletsBuffer = BufferUtils.createFloatBuffer(MAX_NUM_LIGHTS * 2 * 4);
			
			final int BUFFER_BLOCK_BINDING = 1;
			
			int lightsBlockIndex = program.getUniformBlockIndex("Lights");
			
			if(lightsBlockIndex == -1) {
				throw new IllegalArgumentException("Uniform Block 'Lights' not found.");
			}
			
			glUniformBlockBinding(program.getProgram(), lightsBlockIndex, BUFFER_BLOCK_BINDING);
			
			lightsUniformBufferVBO = glGenBuffers();
			glBindBuffer(GL_UNIFORM_BUFFER, lightsUniformBufferVBO);
			glBufferData(GL_UNIFORM_BUFFER, LIGHTS_UNIFORM_BUFFER_SIZE, GL_STREAM_DRAW);
			glBindBuffer(GL_UNIFORM_BUFFER, 0);
			
			glBindBufferBase(GL_UNIFORM_BUFFER, BUFFER_BLOCK_BINDING, lightsUniformBufferVBO);
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
				
				glBindBuffer(GL_UNIFORM_BUFFER, lightsUniformBufferVBO);
				
				ByteBuffer lightsMappedBuffer = glMapBufferRange(GL_UNIFORM_BUFFER, 0, LIGHTS_UNIFORM_BUFFER_SIZE,
						GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT | GL_MAP_UNSYNCHRONIZED_BIT, null);
				
				if(lightsMappedBuffer == null) {
					Utils.checkGLError("lights map buffer");
					throw new OpenGLException("lightsMappedBuffer == null ... no GL error?!");
				}
				
				FloatBuffer lightsBuffer = lightsMappedBuffer.asFloatBuffer();
				
				lightsBuffer.put(ambientColor.toBuffer());
				lightsBuffer.put(bulletCount + 1);
				
				bulletsBuffer.flip();
				lightsBuffer.put(bulletsBuffer);
				
				lightsBuffer.put(0).put(0).put(0); // camera position
				lightsBuffer.put(5000);
				lightsBuffer.put(diffuseColor.toBuffer());
				lightsBuffer.put(mainK);
				
				glUnmapBuffer(GL_UNIFORM_BUFFER);
				
				glBindBuffer(GL_UNIFORM_BUFFER, 0);
			} finally {
				Stopwatch.stop();
			}
		}
	}
}
