package com.ra4king.fps.renderers;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.util.Arrays;

import org.lwjgl.BufferUtils;

import com.ra4king.fps.Camera;
import com.ra4king.fps.actors.Portal;
import com.ra4king.opengl.util.ShaderProgram;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.Quaternion;
import com.ra4king.opengl.util.math.Vector2;
import com.ra4king.opengl.util.math.Vector3;
import com.ra4king.opengl.util.render.RenderUtils;
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
		
		vao = RenderUtils.glGenVertexArrays();
		RenderUtils.glBindVertexArray(vao);
		
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		
		RenderUtils.glBindVertexArray(0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		portalProgram = new ShaderProgram(Utils.readFully(Resources.getInputStream("shaders/portal.vert")),
				                                 Utils.readFully(Resources.getInputStream("shaders/portal.frag")));
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
	
	public void render(Camera camera, Matrix4 viewMatrix, FrustumCulling culling) {
		if(rendered) {
			return;
		}
//		
//		Matrix4 finalMatrix = new Matrix4(projectionMatrix).mult(viewMatrix);
//		Vector3 portalPos = finalMatrix.mult3(portal.getPosition(), 1.0f, new Vector3());
//		Vector3 portalPos2 = finalMatrix.mult3(new Vector3(portal.getPosition()).add(portal.getSize().x(), portal.getSize().y(), 0), 1.0f, new Vector3());
//		if(!culling.isPointInsideFrustum(portalPos) && !culling.isPointInsideFrustum(portalPos2))
//			return;
//		
		rendered = true;
		updated = false;
		
		glEnable(GL_STENCIL_TEST);
		glClear(GL_STENCIL_BUFFER_BIT);
		
		glStencilFunc(GL_ALWAYS, 1, 0xFF);
		glStencilOp(GL_ZERO, GL_ZERO, GL_REPLACE);
		
		portalProgram.begin();
		
		glUniformMatrix4(projectionMatrixUniform, false, camera.getProjectionMatrix().toBuffer());
		glUniformMatrix4(viewMatrixUniform, false, new Matrix4(viewMatrix).translate(portal.getPosition()).mult(portal.getOrientation().toMatrix(new Matrix4())).toBuffer());
		
		glDisable(GL_CULL_FACE);
		
		RenderUtils.glBindVertexArray(vao);
		glDrawArrays(GL_TRIANGLES, 0, 6);
		
		glEnable(GL_CULL_FACE);
		
		glStencilFunc(GL_EQUAL, 1, 0xFF);
		
		glClear(GL_DEPTH_BUFFER_BIT);
		
		portalCamera.setCamera(camera);
		
		//Quaternion destUp = new Quaternion((float)Math.PI * 0.25f, portal.getDestPortal().getOrientation().mult3(Vector3.UP, new Vector3()));
		
		// Calculate the difference orientation between the portal's orientation and the camera's orientation
		Quaternion diff = new Quaternion(camera.getOrientation()).mult(new Quaternion(portal.getOrientation()).inverse()).normalize();
		// Multiply this difference with the destination portal to get the correct effect
		portalCamera.getOrientation().set(diff).mult(portal.getDestPortal().getOrientation()).normalize();
		
		// Get the difference orientation between the origin portal and the destination portal
		diff.set(portal.getOrientation())/*.mult(destUp)*/.mult(new Quaternion(portal.getDestPortal().getOrientation()).inverse()).normalize();
		
		// Convert the position difference using the difference orientation
		Vector3 diffPosition = new Vector3(camera.getPosition()).sub(portal.getPosition());
		portalCamera.getPosition().set(portal.getDestPortal().getPosition()).add(diff.mult3(diffPosition, new Vector3()));
		
		worldRenderer.render(portalCamera);
		
		glDisable(GL_STENCIL_TEST);
	}
}
