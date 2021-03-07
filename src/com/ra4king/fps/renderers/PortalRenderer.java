package com.ra4king.fps.renderers;

import static org.lwjgl.opengl.GL11.GL_ALWAYS;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_EQUAL;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_REPLACE;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_ZERO;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glStencilFunc;
import static org.lwjgl.opengl.GL11.glStencilOp;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DEPTH24_STENCIL8;
import static org.lwjgl.opengl.GL30.GL_DEPTH_STENCIL_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30.GL_RGB32F;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindRenderbuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glDeleteRenderbuffers;
import static org.lwjgl.opengl.GL30.glFramebufferRenderbuffer;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenRenderbuffers;
import static org.lwjgl.opengl.GL30.glRenderbufferStorage;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.OpenGLException;

import com.ra4king.fps.Camera;
import com.ra4king.fps.actors.Portal;
import com.ra4king.opengl.util.ShaderProgram;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.Vector2;
import com.ra4king.opengl.util.math.Vector3;
import com.ra4king.opengl.util.math.Vector4;
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

	private int portalFbo, portalTexture, depthStencilBuf;

	public PortalRenderer(Portal portal, WorldRenderer worldRenderer) {
		this.portal = portal;
		this.worldRenderer = worldRenderer;

		portalCamera = new Camera();

		init();
	}

	public Portal getPortal() {
		return portal;
	}

	private void init() {
		Vector2 s = portal.getSize();

		final float[] portalData = {
			0, 0, 0, s.x(), 0, 0, 0, s.y(), 0,

			s.x(), s.y(), 0, 0, s.y(), 0, s.x(), 0, 0,
			};

		System.out.println(Arrays.toString(portalData));

		int vbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER,
		             BufferUtils.createFloatBuffer(portalData.length).put(portalData).flip(),
		             GL_STATIC_DRAW);

		vao = RenderUtils.glGenVertexArrays();
		RenderUtils.glBindVertexArray(vao);

		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

		RenderUtils.glBindVertexArray(0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);

		portalProgram = new ShaderProgram(Utils.readFully(Resources.getInputStream("shaders/portal.vert")),
		                                  Utils.readFully(Resources.getInputStream("shaders/portal.frag")));

		portalProgram.begin();
		glUniform1i(portalProgram.getUniformLocation("portalTex"), 0);
		glUniform2f(portalProgram.getUniformLocation("size"), s.x(), s.y());
	}

	private void setupFramebuffer() {
		if (portalFbo != 0) {
			glDeleteFramebuffers(portalFbo);
			glDeleteTextures(portalTexture);
			glDeleteRenderbuffers(depthStencilBuf);
		}

		portalFbo = glGenFramebuffers();
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, portalFbo);

		portalTexture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, portalTexture);
		glTexImage2D(GL_TEXTURE_2D,
		             0,
		             GL_RGB32F,
		             RenderUtils.getWidth(),
		             RenderUtils.getHeight(),
		             0,
		             GL_RGB,
		             GL_FLOAT,
		             (ByteBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, portalTexture, 0);
		glBindTexture(GL_TEXTURE_2D, 0);

		depthStencilBuf = glGenRenderbuffers();
		glBindRenderbuffer(GL_RENDERBUFFER, depthStencilBuf);
		glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, RenderUtils.getWidth(), RenderUtils.getHeight());
		glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depthStencilBuf);
		glBindRenderbuffer(GL_RENDERBUFFER, 0);

		IntBuffer drawBuffers = BufferUtils.createIntBuffer(1).put(GL_COLOR_ATTACHMENT0);
		drawBuffers.flip();
		glDrawBuffers(drawBuffers);

		int fboStatus = glCheckFramebufferStatus(GL_FRAMEBUFFER);
		if (fboStatus != GL_FRAMEBUFFER_COMPLETE) {
			throw new OpenGLException("FBO not complete, status: " + fboStatus);
		}

		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
	}

	private boolean updated = false;
	private boolean rendered = false;

	public void update(long deltaTime) {
		rendered = false;

		if (updated) {
			return;
		}

		updated = true;

		worldRenderer.update(deltaTime);
	}

	public void resized() {
		setupFramebuffer();
	}

	public void render(int currentFbo, Camera camera, Matrix4 viewMatrix, FrustumCulling culling) {
		updated = false;

		if (rendered) {
			return;
		}

		rendered = true;

		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, portalFbo);

		glEnable(GL_STENCIL_TEST);
		glClear(GL_STENCIL_BUFFER_BIT);

		// Setup stencil test: all pixels that pass depth test mark 1 in stencil buffer
		glStencilFunc(GL_ALWAYS, 1, 0xFF);
		glStencilOp(GL_ZERO, GL_ZERO, GL_REPLACE);

		portalProgram.begin();

		glUniformMatrix4(portalProgram.getUniformLocation("projectionMatrix"),
		                 false,
		                 camera.getProjectionMatrix().toBuffer());
		glUniformMatrix4(portalProgram.getUniformLocation("viewMatrix"),
		                 false,
		                 new Matrix4(viewMatrix).translate(portal.getPosition())
		                                        .mult(portal.getOrientation().toMatrix(new Matrix4()))
		                                        .toBuffer());
		glUniform2f(portalProgram.getUniformLocation("resolution"), RenderUtils.getWidth(), RenderUtils.getHeight());

		// Now render the portal quad, with each pixel marking the stencil buffer
		glDisable(GL_CULL_FACE);
		RenderUtils.glBindVertexArray(vao);
		glDrawArrays(GL_TRIANGLES, 0, 6);
		glEnable(GL_CULL_FACE);

		glStencilFunc(GL_EQUAL, 1, 0xFF);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		portalCamera.setCamera(camera);
		portal.transform(portalCamera.getPosition(), portalCamera.getOrientation());

		Vector3 destNormal = portal.getDestPortal().getOrientation().mult3(Vector3.FORWARD, new Vector3()).normalize();
		Vector4 portalPlane = new Vector4(destNormal, -destNormal.dot(portal.getDestPortal().getPosition()));

		Vector3 normal = portal.getOrientation().mult3(Vector3.FORWARD, new Vector3()).normalize();
		if (camera.getPosition().dot(normal) - normal.dot(portal.getPosition()) > 0) {
			portalPlane.mult(-1f);
		}

		worldRenderer.render(portalPlane, portal, portalFbo, portalCamera);

		glDisable(GL_STENCIL_TEST);
		glDisable(GL_CULL_FACE);

		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, currentFbo);

		portalProgram.begin();
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, portalTexture);

		RenderUtils.glBindVertexArray(vao);
		glDrawArrays(GL_TRIANGLES, 0, 6);

		glEnable(GL_CULL_FACE);
	}
}
