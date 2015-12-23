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
import com.ra4king.opengl.util.math.Vector2;
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

//		Does not work, as portal could occupy entire screen with its corners outside the frustum and it would be culled, which is incorrect behavior.
//		Vector3 portalPos1 = new Vector3(portal.getSize().x(), 0, 0);
//		Vector3 portalPos2 = new Vector3(portal.getSize().x(), -portal.getSize().y(), 0);
//		Vector3 portalPos3 = new Vector3(0, -portal.getSize().y(), 0);
//		
//		portal.getOrientation().mult3(portalPos1, portalPos1).add(portal.getPosition());
//		portal.getOrientation().mult3(portalPos2, portalPos2).add(portal.getPosition());
//		portal.getOrientation().mult3(portalPos3, portalPos3).add(portal.getPosition());
//		
//		if(!culling.isPointInsideFrustum(portal.getPosition()) && !culling.isPointInsideFrustum(portalPos1) &&
//				   !culling.isPointInsideFrustum(portalPos2) && !culling.isPointInsideFrustum(portalPos3)) {
//			return;
//		}
		
		rendered = true;
		updated = false;
		
		glEnable(GL_STENCIL_TEST);
		glClear(GL_STENCIL_BUFFER_BIT);
		
		glStencilFunc(GL_ALWAYS, 1, 0xFF);
		glStencilOp(GL_ZERO, GL_ZERO, GL_REPLACE);
		
		portalProgram.begin();
		
		glUniformMatrix4(portalProgram.getUniformLocation("projectionMatrix"), false, camera.getProjectionMatrix().toBuffer());
		glUniformMatrix4(portalProgram.getUniformLocation("viewMatrix"), false, new Matrix4(viewMatrix).translate(portal.getPosition()).mult(portal.getOrientation().toMatrix(new Matrix4())).toBuffer());
		glUniform1f(portalProgram.getUniformLocation("a"), 1.0f);
		
		glDisable(GL_CULL_FACE);
		
		RenderUtils.glBindVertexArray(vao);
		glDrawArrays(GL_TRIANGLES, 0, 6);
		
		glEnable(GL_CULL_FACE);
		
		glStencilFunc(GL_EQUAL, 1, 0xFF);
		glClear(GL_DEPTH_BUFFER_BIT);
		
		portalCamera.setCamera(camera);
		portal.transform(portalCamera.getPosition(), portalCamera.getOrientation());
		
		worldRenderer.render(portalCamera);
		
		glDisable(GL_STENCIL_TEST);
		
		portalProgram.begin();
		glUniform1f(portalProgram.getUniformLocation("a"), 0.0f);
		
		glDisable(GL_CULL_FACE);
		
		RenderUtils.glBindVertexArray(vao);
		glDrawArrays(GL_TRIANGLES, 0, 6);
		
		glEnable(GL_CULL_FACE);
	}
}
