package com.ra4king.fps.renderers;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;

import java.nio.FloatBuffer;

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
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.MatrixStack;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author Roi Atalla
 */
public class WorldRenderer {
	private World world;
	
	private BulletRenderer bulletRenderer;
	
	private int vao;
	
	private ShaderProgram worldProgram;
	private int projectionMatrixUniform;
	private int modelViewMatrixUniform;
	
	private static final int MAX_NUM_LIGHTS = 500;
	
	private FloatBuffer lightsBuffer;
	private int lightsUniformBufferVBO;
	
	private FrustumCulling culling;
	
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
		
		if(GLUtils.get().VERSION >= 32)
			glEnable(GL_DEPTH_CLAMP);
		
		// private int mainLightPositionUniform, mainDiffuseColorUniform, mainAmbientColorUniform;
		// private int numberOfLightsUniform;
		// private int lightPositionsUniform, lightColorsUniform;
		
		// private FloatBuffer lightsColorBuffer;
		
		// if(GLUtils.get().VERSION >= 33)
		// worldProgram = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream(GLUtils.get().SHADERS_ROOT_PATH + "fps.vert")),
		// Utils.readFully(getClass().getResourceAsStream(GLUtils.get().SHADERS_ROOT_PATH + "fps.frag")));
		// else if(GLUtils.get().VERSION >= 30)
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
		float[] unitCube = {
				-0.5f, 0.5f, 0.5f,
				0, 0, 1,
				0.5f, 0.5f, 0.5f,
				0, 0, 1,
				0.5f, -0.5f, 0.5f,
				0, 0, 1,
				0.5f, -0.5f, 0.5f,
				0, 0, 1,
				-0.5f, -0.5f, 0.5f,
				0, 0, 1,
				-0.5f, 0.5f, 0.5f,
				0, 0, 1,
				
				0.5f, 0.5f, -0.5f,
				0, 0, -1,
				-0.5f, 0.5f, -0.5f,
				0, 0, -1,
				-0.5f, -0.5f, -0.5f,
				0, 0, -1,
				-0.5f, -0.5f, -0.5f,
				0, 0, -1,
				0.5f, -0.5f, -0.5f,
				0, 0, -1,
				0.5f, 0.5f, -0.5f,
				0, 0, -1,
				
				-0.5f, 0.5f, -0.5f,
				0, 1, 0,
				0.5f, 0.5f, -0.5f,
				0, 1, 0,
				0.5f, 0.5f, 0.5f,
				0, 1, 0,
				0.5f, 0.5f, 0.5f,
				0, 1, 0,
				-0.5f, 0.5f, 0.5f,
				0, 1, 0,
				-0.5f, 0.5f, -0.5f,
				0, 1, 0,
				
				-0.5f, -0.5f, 0.5f,
				0, -1, 0,
				0.5f, -0.5f, 0.5f,
				0, -1, 0,
				0.5f, -0.5f, -0.5f,
				0, -1, 0,
				0.5f, -0.5f, -0.5f,
				0, -1, 0,
				-0.5f, -0.5f, -0.5f,
				0, -1, 0,
				-0.5f, -0.5f, 0.5f,
				0, -1, 0,
				
				0.5f, 0.5f, 0.5f,
				1, 0, 0,
				0.5f, 0.5f, -0.5f,
				1, 0, 0,
				0.5f, -0.5f, -0.5f,
				1, 0, 0,
				0.5f, -0.5f, -0.5f,
				1, 0, 0,
				0.5f, -0.5f, 0.5f,
				1, 0, 0,
				0.5f, 0.5f, 0.5f,
				1, 0, 0,
				
				-0.5f, 0.5f, -0.5f,
				-1, 0, 0,
				-0.5f, 0.5f, 0.5f,
				-1, 0, 0,
				-0.5f, -0.5f, 0.5f,
				-1, 0, 0,
				-0.5f, -0.5f, 0.5f,
				-1, 0, 0,
				-0.5f, -0.5f, -0.5f,
				-1, 0, 0,
				-0.5f, 0.5f, -0.5f,
				-1, 0, 0,
		};
		
		FloatBuffer cubeBuffer = BufferUtils.createFloatBuffer(unitCube.length);
		cubeBuffer.put(unitCube).flip();
		
		glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
		glBufferData(GL_ARRAY_BUFFER, cubeBuffer, GL_STATIC_DRAW);
		
		worldProgram = new ShaderProgram(
				Utils.readFully(getClass().getResourceAsStream(GLUtils.get().SHADERS_ROOT_PATH + "fps.vert")),
				Utils.readFully(getClass().getResourceAsStream(GLUtils.get().SHADERS_ROOT_PATH + "fps.frag")));
		
		vao = GLUtils.get().glGenVertexArrays();
		GLUtils.get().glBindVertexArray(vao);
		
		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);
		glVertexAttribDivisor(0, 1);
		glVertexAttribDivisor(1, 1);
		
		glEnableVertexAttribArray(2);
		glEnableVertexAttribArray(3);
		glVertexAttribPointer(2, 3, GL_FLOAT, false, 6 * 4, 0);
		glVertexAttribPointer(3, 3, GL_FLOAT, false, 6 * 4, 3 * 4);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		GLUtils.get().glBindVertexArray(0);
		
		lightsBuffer = BufferUtils.createFloatBuffer(MAX_NUM_LIGHTS * 2 * 4 + 2 * 4);
		
		int lightsBlockIndex = worldProgram.getUniformBlockIndex("Lights");
		glUniformBlockBinding(worldProgram.getProgram(), lightsBlockIndex, 1);
		
		lightsUniformBufferVBO = glGenBuffers();
		glBindBuffer(GL_UNIFORM_BUFFER, lightsUniformBufferVBO);
		glBufferData(GL_UNIFORM_BUFFER, lightsBuffer.capacity() * 4, GL_STREAM_DRAW);
		glBindBufferBase(GL_UNIFORM_BUFFER, 1, lightsUniformBufferVBO);
		glBindBuffer(GL_UNIFORM_BUFFER, 0);
		
		projectionMatrixUniform = worldProgram.getUniformLocation("projectionMatrix");
		modelViewMatrixUniform = worldProgram.getUniformLocation("modelViewMatrix");
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
			
			long cubesRendered = 0;
			for(Chunk c : world.getChunkManager().getChunks())
				cubesRendered += c.getCubesRendered();
			
			System.out.println("Average of " + (cubesRendered / frameCount) + " cubes renderered.\n");
			
			frameCount = chunksRendered = 0;
		}
	}
	
	private final Vector3 mainDiffuseColor = new Vector3(1f, 1f, 1f);
	private final Vector3 mainAmbientColor = new Vector3(0.01f, 0.01f, 0.01f);
	
	private final Bullet aim = new Bullet(new Vector3(), new Vector3(), 4, 0, Long.MAX_VALUE, false, new Vector3(1));
	
	private final Matrix4 viewMatrix = new Matrix4(), cullingProjectionMatrix = new Matrix4();
	private final MatrixStack tempStack = new MatrixStack();
	
	private final Vector3 renderTemp = new Vector3();
	
	public void render() {
		worldProgram.begin();
		
		Camera camera = world.getCamera();
		
		world.getCamera().getOrientation().toMatrix(viewMatrix).translate(camera.getPosition());
		
		glUniformMatrix4(projectionMatrixUniform, false, camera.getProjectionMatrix().toBuffer());
		glUniformMatrix4(modelViewMatrixUniform, false, viewMatrix.toBuffer());
		
		culling.setupPlanes(cullingProjectionMatrix.set(camera.getProjectionMatrix()).mult(viewMatrix));
		
		final float mainK = 0.001f;
		
		lightsBuffer.clear();
		
		lightsBuffer.put(mainDiffuseColor.toBuffer());
		lightsBuffer.put(mainK);
		
		lightsBuffer.put(mainAmbientColor.toBuffer());
		
		bulletRenderer.getBulletLightData(viewMatrix, lightsBuffer, MAX_NUM_LIGHTS);
		
		lightsBuffer.flip();
		
		glBindBuffer(GL_UNIFORM_BUFFER, lightsUniformBufferVBO);
		glBufferSubData(GL_UNIFORM_BUFFER, 0, lightsBuffer);
		glBindBuffer(GL_UNIFORM_BUFFER, 0);
		
		GLUtils.get().glBindVertexArray(vao);
		
		for(Chunk chunk : world.getChunkManager().getChunks())
			if(culling.isCubeInsideFrustum(renderTemp.set(chunk.getChunkInfo().chunkCornerX, chunk.getChunkInfo().chunkCornerY, -chunk.getChunkInfo().chunkCornerZ).mult(Chunk.SPACING), Chunk.CUBES_SIDE * Chunk.SPACING * 2)) {
				chunk.render();
				chunksRendered++;
			}
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		frameCount++;
		
		GLUtils.get().glBindVertexArray(0);
		
		worldProgram.end();
		
		bulletRenderer.render(camera.getProjectionMatrix(), tempStack.clear().setTop(viewMatrix), culling);
		
		glDepthMask(false);
		
		glDisable(GL_DEPTH_TEST);
		bulletRenderer.render(new Matrix4().clearToOrtho(-GLUtils.get().getWidth() / 2, GLUtils.get().getWidth() / 2, -GLUtils.get().getHeight() / 2, GLUtils.get().getHeight() / 2, -1, 1), new MatrixStack(), null, aim);
		glEnable(GL_DEPTH_TEST);
		
		glDepthMask(true);
	}
}
