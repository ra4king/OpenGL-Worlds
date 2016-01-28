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
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.*;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
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
import com.ra4king.opengl.util.buffers.GLBuffer;
import com.ra4king.opengl.util.buffers.MappedBuffer;
import com.ra4king.opengl.util.math.Matrix3;
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.MatrixStack;
import com.ra4king.opengl.util.math.Vector2;
import com.ra4king.opengl.util.math.Vector3;
import com.ra4king.opengl.util.math.Vector4;
import com.ra4king.opengl.util.render.RenderUtils;
import com.ra4king.opengl.util.render.RenderUtils.FrustumCulling;

/**
 * @author Roi Atalla
 */
public class WorldRenderer {
	private static final int MAX_NUM_LIGHTS = 500;
	
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
	
	private int chunkVAO, cubeVBO, indicesVBO, commandsVBO;
	private ChunkRenderer[] chunkRenderers;
	private BufferStorage chunkRendererStorage;
	
	private int chunksRendered, blocksRendered;
	
	private GLBuffer lightsBufferObject;
	private BulletRenderer bulletRenderer;
	
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
		
		loadCube();
		setupBlockVAO();
		
		setupDeferredFBO();
		setupDeferredVAO();
		
		final int COMMANDS_BUFFER_SIZE = chunkRenderers.length * 5 * 4;
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
				                                 Utils.readFully(Resources.getInputStream("shaders/blocks.frag")));
		
		projectionMatrixUniform = blocksProgram.getUniformLocation("projectionMatrix");
		viewMatrixUniform = blocksProgram.getUniformLocation("viewMatrix");
		normalMatrixUniform = blocksProgram.getUniformLocation("normalMatrix");
		
		blocksProgram.begin();
		glUniform1f(blocksProgram.getUniformLocation("cubeSize"), Chunk.BLOCK_SIZE);
		blocksProgram.end();
		
		deferredProgram = new ShaderProgram(Utils.readFully(Resources.getInputStream("shaders/deferred.vert")),
		                                     Utils.readFully(Resources.getInputStream("shaders/deferred.geom")),
				                                   Utils.readFully(Resources.getInputStream("shaders/deferred.frag")));
	}
	
	private void loadCube() {
		final short[] indices = { 0, 1, 2, 2, 3, 0 };
		
		final Vector3[] normals = {
		  new Vector3(0, 0, 1), // front
		  new Vector3(0, 0, -1), // back
		  new Vector3(0, 1, 0), // top
		  new Vector3(0, -1, 0), // bottom
		  new Vector3(1, 0, 0), // right
		  new Vector3(-1, 0, 0) // left
		};
		
		final Vector2[] texCoords = {
				new Vector2(0, 1),
				new Vector2(1, 1),
				new Vector2(1, 0),
				new Vector2(0, 0)
		};
		
		final Vector3[] unitCube = {
				// front
				new Vector3(0.0f, 1.0f, 0.0f),
				new Vector3(1.0f, 1.0f, 0.0f),
				new Vector3(1.0f, 0.0f, 0.0f),
				new Vector3(0.0f, 0.0f, 0.0f),
				
				// back
				new Vector3(1.0f, 1.0f, -1.0f),
				new Vector3(0.0f, 1.0f, -1.0f),
				new Vector3(0.0f, 0.0f, -1.0f),
				new Vector3(1.0f, 0.0f, -1.0f),
				
				// top
				new Vector3(0.0f, 1.0f, -1.0f),
				new Vector3(1.0f, 1.0f, -1.0f),
				new Vector3(1.0f, 1.0f, 0.0f),
				new Vector3(0.0f, 1.0f, 0.0f),
				
				// bottom
				new Vector3(0.0f, 0.0f, 0.0f),
				new Vector3(1.0f, 0.0f, 0.0f),
				new Vector3(1.0f, 0.0f, -1.0f),
				new Vector3(0.0f, 0.0f, -1.0f),
				
				// right
				new Vector3(1.0f, 1.0f, 0.0f),
				new Vector3(1.0f, 1.0f, -1.0f),
				new Vector3(1.0f, 0.0f, -1.0f),
				new Vector3(1.0f, 0.0f, 0.0f),
				
				// left
				new Vector3(0.0f, 1.0f, -1.0f),
				new Vector3(0.0f, 1.0f, 0.0f),
				new Vector3(0.0f, 0.0f, 0.0f),
				new Vector3(0.0f, 0.0f, -1.0f)
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
		
		glBindBuffer(GL_ARRAY_BUFFER, cubeVBO);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesVBO);
		
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, (2 * 3 + 2) * 4, 0);
		
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 3, GL_FLOAT, false, (2 * 3 + 2) * 4, 3 * 4);
		
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(2, 2, GL_FLOAT, false, (2 * 3 + 2) * 4, 2 * 3 * 4);
		
		chunkRendererStorage = new BufferStorage(GL_ARRAY_BUFFER, DATA_VBO_SIZE, true, 3);
		
		glEnableVertexAttribArray(3);
		glVertexAttribPointer(3, 3, GL_UNSIGNED_INT, false, 4 * 4, 0);
		RenderUtils.glVertexAttribDivisor(3, 1);
		
		glEnableVertexAttribArray(4);
		glVertexAttribIPointer(4, 1, GL_UNSIGNED_INT, 4 * 4, 3 * 4);
		RenderUtils.glVertexAttribDivisor(4, 1);
		
		RenderUtils.glBindVertexArray(0);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		
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
		lightsBufferObject = new MappedBuffer(GL_ARRAY_BUFFER, MAX_NUM_LIGHTS * 8 * 4, true);
		
		deferredVAO = RenderUtils.glGenVertexArrays();
		RenderUtils.glBindVertexArray(deferredVAO);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * 4, 0);
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 1, GL_FLOAT, false, 8 * 4, 3 * 4);
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(2, 3, GL_FLOAT, false, 8 * 4, 4 * 4);
		glEnableVertexAttribArray(3);
		glVertexAttribPointer(3, 1, GL_FLOAT, false, 8 * 4, 7 * 4);
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
	
	private final DrawElementsIndirectCommand command = new DrawElementsIndirectCommand();
	
	{
		command.count = 6 * 6; // elements count
		command.firstIndex = 0;
		command.baseVertex = 0;
	}
	
	public void render(Vector4 clipPlane, Portal surroundingPortal, int currentFbo, Camera camera) {
//		glClearColor(0.4f, 0.6f, 0.9f, 0f);
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
		
		for(ChunkRenderer chunkRenderer : chunkRenderers) {
			chunkRenderer.update();
		}
		
		commandsBuffer.clear();
		
		int currentOffset = chunkRendererStorage.getBufferIndex() * chunkRenderers.length * Chunk.TOTAL_BLOCKS;
		
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
		
		chunkRendererStorage.nextBuffer();
		
		commandsBuffer.flip();
		
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, commandsVBO);
		glBufferData(GL_DRAW_INDIRECT_BUFFER, commandsBuffer, GL_STREAM_DRAW);
		
		{
			glBindFramebuffer(GL_DRAW_FRAMEBUFFER, deferredFBO);
			
			glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			
			glDisable(GL_BLEND);
			glEnable(GL_DEPTH_TEST);
			
			blocksProgram.begin();
			
			glUniformMatrix4(projectionMatrixUniform, false, camera.getProjectionMatrix().toBuffer());
			glUniformMatrix4(viewMatrixUniform, false, viewMatrix.toBuffer());
			
			glUniformMatrix3(normalMatrixUniform, false, new Matrix3().set4x4(viewMatrix).inverse().transpose().toBuffer());
			
			if(clipPlane != null) {
				glEnable(GL_CLIP_DISTANCE0);
				glUniform4(blocksProgram.getUniformLocation("clipPlane"), clipPlane.toBuffer());
			}
			
			RenderUtils.glBindVertexArray(chunkVAO);
			glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_SHORT, 0, chunksRendered, 0);
			
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
			glBlendFunc(GL_ONE, GL_ONE);
			
			deferredProgram.begin();
			
			glUniformMatrix4(deferredProgram.getUniformLocation("projectionMatrix"), false, camera.getProjectionMatrix().toBuffer());
			
			Vector3 ambientColor = new Vector3(0.01f, 0.01f, 0.01f);
			Vector3 diffuseColor = new Vector3(0.5f, 0.1f, 0.1f);
			
			FloatBuffer lightsBuffer = lightsBufferObject.bind().asFloatBuffer();
			
			// ambient
			lightsBuffer.put(0).put(0).put(0).put(0);
			lightsBuffer.put(ambientColor.toBuffer());
			lightsBuffer.put(0);
			
			// camera is light source
			lightsBuffer.put(0).put(0).put(0); // camera position
			lightsBuffer.put(1000);
			lightsBuffer.put(diffuseColor.toBuffer());
			lightsBuffer.put(0.1f);

//			Vector4 result = new Vector4();
//			System.out.println(camera.getProjectionMatrix().mult4(new Vector4(10, 10, -3, 1), result).divide(result.w()).toString());
			
			int lightCount = bulletRenderer.getBulletLightData(viewMatrix, lightsBuffer, MAX_NUM_LIGHTS - 2);
			
			lightsBufferObject.unbind();
			
			glDepthFunc(GL_ALWAYS);
			
			RenderUtils.glBindVertexArray(deferredVAO);
			glDrawArrays(GL_POINTS, 0, lightCount + 2);
			
			glDepthFunc(GL_LESS);
		}
		
		Stopwatch.stop();
		
		for(PortalRenderer portalRenderer : portalRenderers) {
			if(portalRenderer.getPortal().getDestPortal() == surroundingPortal) {
				continue;
			}
			
			portalRenderer.render(currentFbo, camera, viewMatrix, culling);
		}
		
		Stopwatch.start("BulletRenderer");
		
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		bulletRenderer.render(camera.getProjectionMatrix(), tempStack.setTop(viewMatrix), culling);
		
		glDisable(GL_DEPTH_TEST);
		bulletRenderer.render(new Matrix4().clearToOrtho(-RenderUtils.getWidth() / 2, RenderUtils.getWidth() / 2, -RenderUtils.getHeight() / 2, RenderUtils.getHeight() / 2, -1, 1), new MatrixStack(), null, aim);
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
}
