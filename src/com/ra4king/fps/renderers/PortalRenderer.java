package com.ra4king.fps.renderers;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.util.Arrays;

import org.lwjgl.BufferUtils;

import com.ra4king.fps.Camera;
import com.ra4king.fps.GLUtils;
import com.ra4king.fps.actors.Portal;
import com.ra4king.opengl.util.ShaderProgram;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.Quaternion;
import com.ra4king.opengl.util.math.Vector2;
import com.ra4king.opengl.util.math.Vector3;
import com.ra4king.opengl.util.render.RenderUtils.FrustumCulling;

/**
 * @author Roi Atalla
 */
public class PortalRenderer {
	private Portal portal;
	private Camera portalCamera;
	private WorldRenderer worldRenderer;
	
	private ShaderProgram portalProgram;
	private int projectionMatrixUniform, viewMatrixUniform;
	private int vao;
	
	public PortalRenderer(Portal portal, WorldRenderer worldRenderer) {
		this.portal = portal;
		this.worldRenderer = worldRenderer;
		
		portalCamera = new Camera();
		
		init();
	}
	
	private void init() {
		Vector2 s = portal.getSize();
		
		final float[] portalData = {
		                             0, 0, 0,
		                             s.x(), 0, 0,
		                             0, -s.y(), 0,
		  
		                             s.x(), -s.y(), 0,
		                             0, -s.y(), 0,
		                             s.x(), 0, 0,
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
	}
	
	private static boolean updated = false;
	private static boolean rendered = false;
	
	public void update(long deltaTime) {
		if(updated) {
			return;
		}
		
		updated = true;
		rendered = false;
		
		worldRenderer.update(deltaTime);
	}
	
	public void render(Matrix4 projectionMatrix, Matrix4 viewMatrix, FrustumCulling culling) {
//		if(!culling.isRectPrismInsideFrustum(portal.getPosition(), portal.getSize().x(), -portal.getSize().y(), 0))
//			return;
		
		if(!rendered) {
			glEnable(GL_STENCIL_TEST);
			glClear(GL_STENCIL_BUFFER_BIT);
			
			glStencilFunc(GL_ALWAYS, 1, 0xFF);
			glStencilOp(GL_ZERO, GL_ZERO, GL_REPLACE);
		}
		
		portalProgram.begin();
		
		glUniformMatrix4(projectionMatrixUniform, false, projectionMatrix.toBuffer());
		glUniformMatrix4(viewMatrixUniform, false, new Matrix4(viewMatrix).translate(portal.getPosition()).mult(portal.getOrientation().toMatrix(new Matrix4())).toBuffer());
		
		GLUtils.glBindVertexArray(vao);
		glDrawArrays(GL_TRIANGLES, 0, 6);
		
		if(!rendered) {
			rendered = true;
			updated = false;
			
			glStencilFunc(GL_EQUAL, 1, 0xFF);
			
			glClear(GL_DEPTH_BUFFER_BIT);
			
			portalCamera.setCamera(portal.getCamera());
			
			// Calculate the difference orientation between the portal's orientation and the camera's orientation
			Quaternion diff = new Quaternion(portal.getCamera().getOrientation()).mult(new Quaternion(portal.getOrientation()).inverse());
			// Multiply this difference with the destination portal to get the correct effect
			portalCamera.getOrientation().set(diff.normalize()).mult(portal.getDestPortal().getOrientation()).normalize();
			
			// Get the difference orientation between the origin portal and the destination portal
			diff.set(portal.getDestPortal().getOrientation()).mult(new Quaternion(portal.getOrientation()).inverse()).normalize();
			
			// Convert the position difference using the difference orientation
			Vector3 diffPosition = new Vector3(portalCamera.getPosition()).sub(portal.getPosition());
			portalCamera.getPosition().set(portal.getDestPortal().getPosition()).add(diff.inverse().mult3(diffPosition, new Vector3()));
			
			worldRenderer.render(portalCamera);
			
			glDisable(GL_STENCIL_TEST);
		}
	}
}
