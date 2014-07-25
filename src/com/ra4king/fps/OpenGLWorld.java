package com.ra4king.fps;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.PixelFormat;

import com.ra4king.fps.renderers.WorldRenderer;
import com.ra4king.fps.world.World;
import com.ra4king.opengl.util.GLProgram;

/**
 * @author Roi Atalla
 */
public class OpenGLWorld extends GLProgram {
	public static void main(String[] args) throws Exception {
		new OpenGLWorld().run(true, new PixelFormat(16, 0, 8, 0, 4));
	}
	
	private World world;
	private WorldRenderer worldRenderer;
	
	// private Fractal fractal;

	public OpenGLWorld() {
		super("OpenGLWorld", 800, 600, true);
	}
	
	@Override
	public void init() {
		setPrintDebug(true);
		
		GLUtils.init();
		
		if(GLUtils.get().GL_VERSION < 21) {
			System.out.println("Your OpenGL version is too old.");
			System.exit(1);
		}

	setFPS(0);
		
		// Mouse.setGrabbed(true);
		
		world = new World();
		worldRenderer = new WorldRenderer(world);
	}
	
	@Override
	public void resized() {
		super.resized();
		
		world.resized();
	}
	
	@Override
	public boolean shouldStop() {
		return false;
	}
	
	@Override
	public void keyPressed(int key, char c) {
		if(key == Keyboard.KEY_ESCAPE)
			Mouse.setGrabbed(!Mouse.isGrabbed());
		
		world.keyPressed(key, c);
	}
	
	private long timePassed;
	private long worldUpdateTime, worldRenderUpdateTime, frames;
	
	@Override
	public void update(long deltaTime) {
		timePassed += deltaTime;
		
		long before = System.nanoTime();
		world.update(deltaTime);
		worldUpdateTime += System.nanoTime() - before;
		
		before = System.nanoTime();
		worldRenderer.update(deltaTime);
		worldRenderUpdateTime += System.nanoTime() - before;
		
		frames++;
		
		while(timePassed >= 1e9) {
			timePassed -= 1e9;
			
			if(frames == 0)
				continue;
			
			System.out.printf("World update: %.3f ms\t", worldUpdateTime / (frames * 1e6));
			System.out.printf("World Renderer update: %.3f ms\n", worldRenderUpdateTime / (frames * 1e6));
			
			worldUpdateTime = worldRenderUpdateTime = frames = 0;
		}
	}

	@Override
	public void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		worldRenderer.render();
	}
}
