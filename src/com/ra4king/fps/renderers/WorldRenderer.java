package com.ra4king.fps.renderers;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;

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
	private World world;
	
	private BulletRenderer bulletRenderer;
	
	private ShaderProgram worldProgram;
	private int projectionMatrixUniform;
	private int viewMatrixUniform;
	private int normalMatrixUniform;
	
	private LightSystem lightSystem;
	
	private static final int MAX_NUM_LIGHTS;
	
	private FrustumCulling culling;
	
	static {
		switch(GLUtils.get().GL_VERSION) {
			case 31:
				MAX_NUM_LIGHTS = 100;
				break;
			case 30:
				MAX_NUM_LIGHTS = 100;
				break;
			case 21:
				MAX_NUM_LIGHTS = 50;
				break;
			default:
				MAX_NUM_LIGHTS = 10;
				break;
		}
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
		
		if(GLUtils.get().GL_VERSION >= 32)
			glEnable(GL_DEPTH_CLAMP);
		
		// private int mainLightPositionUniform, mainDiffuseColorUniform, mainAmbientColorUniform;
		// private int numberOfLightsUniform;
		// private int lightPositionsUniform, lightColorsUniform;
		
		// private FloatBuffer lightsColorBuffer;
		
		// if(GLUtils.get().GL_VERSION >= 33)
		// worldProgram = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream(GLUtils.get().SHADERS_ROOT_PATH + "fps.vert")),
		// Utils.readFully(getClass().getResourceAsStream(GLUtils.get().SHADERS_ROOT_PATH + "fps.frag")));
		// else if(GLUtils.get().GL_VERSION >= 30)
		// worldProgram = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream(GLUtils.get().SHADERS_ROOT_PATH + "fps3.0.vert")),
		// Utils.readFully(getClass().getResourceAsStream(GLUtils.get().SHADERS_ROOT_PATH + "fps3.0.frag")));
		// else {
		// throw new RuntimeException("2.1 and below not supported.");
		//
		// // worldProgram = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream(SHADERS_ROOT_PATH + "fps2.1.vert")), Utils.readFully(getClass().getResourceAsStream(SHADERS_ROOT_PATH +
		// // "fps2.1.frag")));
		// }
		
		loadShaders();
		
		culling = new FrustumCulling();
		
		bulletRenderer = new BulletRenderer(world.getBulletManager());
	}
	
	private void loadShaders() {
		String shaderName = "fps" + GLUtils.get().GL_VERSION;
		
		HashMap<Integer,String> attributes = new HashMap<Integer,String>();
		attributes.put(0, "position");
		attributes.put(1, "normal");
		
		worldProgram = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream(GLUtils.get().SHADERS_ROOT_PATH + shaderName + ".vert")),
				Utils.readFully(getClass().getResourceAsStream(GLUtils.get().SHADERS_ROOT_PATH + shaderName + ".frag")),
				attributes);
		
		if(GLUtils.get().GL_VERSION >= 31)
			lightSystem = new UniformBufferLightSystem();
		else
			lightSystem = new UniformArrayLightSystem();
		
		lightSystem.setupLights(worldProgram);
		
		projectionMatrixUniform = worldProgram.getUniformLocation("projectionMatrix");
		viewMatrixUniform = worldProgram.getUniformLocation("viewMatrix");
		normalMatrixUniform = worldProgram.getUniformLocation("normalMatrix");
	}
	
	private long timePassed;
	private int frameCount, chunksRendered;
	
	private boolean isKeyDown;
	
	public void update(long deltaTime) {
		timePassed += deltaTime;
		
		if(Keyboard.isKeyDown(Keyboard.KEY_R) && !isKeyDown) {
			loadShaders();
			System.out.println("Shaders reloaded.");
			isKeyDown = true;
		}
		else
			isKeyDown = false;
		
		while(timePassed >= 1e9) {
			timePassed -= 1e9;
			
			if(frameCount == 0)
				continue;
			
			System.out.println("Average of " + (chunksRendered / frameCount) + " chunks rendered.");
			
			// long cubesRendered = 0;
			// for(Chunk c : world.getChunkManager().getChunks())
			// cubesRendered += c.getCubesRendered();
			
			// System.out.println("Average of " + (cubesRendered / frameCount) + " cubes renderered.\n");
			
			frameCount = chunksRendered = 0;
		}
	}
	
	private final Vector3 mainDiffuseColor = new Vector3(1f, 1f, 1f);
	private final Vector3 mainAmbientColor = new Vector3(0.1f, 0.1f, 0.1f);
	
	private final Bullet aim = new Bullet(new Vector3(), new Vector3(), 4, 0, Long.MAX_VALUE, false, new Vector3(1));
	
	private final Matrix4 viewMatrix = new Matrix4(), cullingProjectionMatrix = new Matrix4();
	private final Matrix3 normalMatrix = new Matrix3();
	
	private final MatrixStack tempStack = new MatrixStack();
	
	private final Vector3 renderTemp = new Vector3();
	
	public void render() {
		Camera camera = world.getCamera();
		
		// Convert Camera's Quaternion to a Matrix4 and translate it by the camera's position
		world.getCamera().getOrientation().toMatrix(viewMatrix).translate(camera.getPosition());
		
		worldProgram.begin();
		
		glUniformMatrix4(projectionMatrixUniform, false, camera.getProjectionMatrix().toBuffer());
		glUniformMatrix4(viewMatrixUniform, false, viewMatrix.toBuffer());
		
		normalMatrix.set(viewMatrix).inverse().transpose();
		glUniformMatrix3(normalMatrixUniform, false, normalMatrix.toBuffer());
		
		final float mainK = 0.001f;
		lightSystem.renderLights(mainDiffuseColor, mainK, mainAmbientColor, viewMatrix, bulletRenderer);
		
		// Setting up the 6 planes that define the edges of the frustum
		culling.setupPlanes(cullingProjectionMatrix.set(camera.getProjectionMatrix()).mult(viewMatrix));
		
		for(Chunk chunk : world.getChunkManager().getChunks()) {
			if(culling.isRectPrismInsideFrustum(renderTemp.set(chunk.getChunkInfo().chunkCornerX, chunk.getChunkInfo().chunkCornerY, -chunk.getChunkInfo().chunkCornerZ).mult(Chunk.SPACING),
					Chunk.CUBES_WIDTH * Chunk.SPACING * 2,
					Chunk.CUBES_HEIGHT * Chunk.SPACING * 2,
					Chunk.CUBES_DEPTH * Chunk.SPACING * 2)) {
				chunk.render(viewMatrix, normalMatrix);
				chunksRendered++;
			}
		}
		
		worldProgram.end();
		
		bulletRenderer.render(camera.getProjectionMatrix(), tempStack.clear().setTop(viewMatrix), culling);
		
		glDepthMask(false);
		
		glDisable(GL_DEPTH_TEST);
		bulletRenderer.render(new Matrix4().clearToOrtho(-GLUtils.get().getWidth() / 2, GLUtils.get().getWidth() / 2, -GLUtils.get().getHeight() / 2, GLUtils.get().getHeight() / 2, -1, 1), new
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
			glBindBuffer(GL_UNIFORM_BUFFER, lightsUniformBufferVBO);
			
			ByteBuffer mapBuffer = glMapBuffer(GL_UNIFORM_BUFFER, GL_WRITE_ONLY, null);
			
			FloatBuffer lightsBuffer = mapBuffer.asFloatBuffer();
			
			bulletsBuffer.clear();
			
			// Fill buffer with each bullet's position as each bullet is a light source
			int bulletCount = bulletRenderer.getBulletLightData(viewMatrix, bulletsBuffer, MAX_NUM_LIGHTS - 1);
			
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
