package com.ra4king.fps.renderers;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.util.Arrays;

import org.lwjgl.BufferUtils;

import com.ra4king.fps.GLUtils;
import com.ra4king.fps.actors.Portal;
import com.ra4king.opengl.util.ShaderProgram;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.Vector2;
import com.ra4king.opengl.util.math.Vector3;

/**
 * @author Roi Atalla
 */
public class PortalRenderer {
	private Portal portal;
	private WorldRenderer worldRenderer;
	
	private ShaderProgram portalProgram;
	private int projectionMatrixUniform, viewMatrixUniform;
	private int vao;
	
	public PortalRenderer(Portal portal, WorldRenderer worldRenderer) {
		this.portal = portal;
		this.worldRenderer = worldRenderer;
		
		init();
	}
	
	private void init() {
		Vector3 v = portal.getPosition();
		Vector2 s = portal.getSize();
		
		final float[] portalData = {
				v.x(), v.y(), v.z(),
				v.x() + s.x(), v.y(), v.z(),
				v.x(), v.y() - s.y(), v.z(),
				
				v.x() + s.x(), v.y() - s.y(), v.z(),
				v.x(), v.y() - s.y(), v.z(),
				v.x() + s.x(), v.y(), v.z(),
		};
		
		System.out.println(Arrays.toString(portalData));
		
		int vbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, (FloatBuffer)BufferUtils.createFloatBuffer(portalData.length).put(portalData).flip(), GL_STATIC_DRAW);
		
		vao = GLUtils.glGenVertexArrays();
		GLUtils.glBindVertexArray(vao);
		
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		
		GLUtils.glBindVertexArray(0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		portalProgram = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream(GLUtils.RESOURCES_ROOT_PATH + "shaders/portal.vert")),
				Utils.readFully(getClass().getResourceAsStream(GLUtils.RESOURCES_ROOT_PATH + "shaders/portal.frag")));
		projectionMatrixUniform = portalProgram.getUniformLocation("projectionMatrix");
		viewMatrixUniform = portalProgram.getUniformLocation("viewMatrix");
		
		// makeDummy();
	}
	
	//
	// private ShaderProgram dummy;
	// private int dummyVAO;
	//
	// private void makeDummy() {
	// dummy = new ShaderProgram("#version 440\nlayout(location = 0) in vec4 position; void main() { gl_Position = position; }\n",
	// "#version 440\nout vec4 fragColor; void main() { fragColor = vec4(1, 1, 0, 1); }\n");
	//
	// FloatBuffer verts = BufferUtils.createFloatBuffer(8).put(new float[] {
	// 1, 1,
	// 1, -1,
	// -1, 1,
	// -1, -1
	// });
	// verts.flip();
	//
	// int vbo = glGenBuffers();
	// glBindBuffer(GL_ARRAY_BUFFER, vbo);
	// glBufferData(GL_ARRAY_BUFFER, verts, GL_STATIC_DRAW);
	//
	// dummyVAO = glGenVertexArrays();
	// glBindVertexArray(dummyVAO);
	// glEnableVertexAttribArray(0);
	// glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
	// glBindVertexArray(0);
	//
	// glBindBuffer(GL_ARRAY_BUFFER, 0);
	// }
	
	public void update(long deltaTime) {
		worldRenderer.update(deltaTime);
	}
	
	public void render(Matrix4 projectionMatrix, Matrix4 viewMatrix) {
		glEnable(GL_STENCIL_TEST);
		glStencilFunc(GL_ALWAYS, 1, 0xFF);
		glStencilOp(GL_ZERO, GL_ZERO, GL_REPLACE);
		
		portalProgram.begin();
		
		glUniformMatrix4(projectionMatrixUniform, false, projectionMatrix.toBuffer());
		glUniformMatrix4(viewMatrixUniform, false, viewMatrix.toBuffer());
		
		GLUtils.glBindVertexArray(vao);
		glDrawArrays(GL_TRIANGLES, 0, 6);
		
		glStencilFunc(GL_EQUAL, 1, 0xFF);
		
		glClear(GL_DEPTH_BUFFER_BIT);
		
		worldRenderer.render();
		
		glDisable(GL_STENCIL_TEST);
	}
}