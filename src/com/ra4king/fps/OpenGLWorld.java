package com.ra4king.fps;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.PixelFormat;

import com.ra4king.fps.renderers.WorldRenderer;
import com.ra4king.fps.world.World;
import com.ra4king.opengl.util.GLProgram;
import com.ra4king.opengl.util.Stopwatch;

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
		setFPS(0);
		
		GLUtils.init();
		
		if(GLUtils.GL_VERSION < 21) {
			System.out.println("Your OpenGL version is too old.");
			System.exit(1);
		}
		
		// Mouse.setGrabbed(true);
		
		world = new World();
		worldRenderer = new WorldRenderer(world);
		
		world.getChunkManager().setupBlocks(true);
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
	
	@Override
	public void update(long deltaTime) {
		Stopwatch.start("World Update");
		world.update(deltaTime);
		Stopwatch.stop();
		
		Stopwatch.start("WorldRenderer Update");
		worldRenderer.update(deltaTime);
		Stopwatch.stop();
	}
	
	@Override
	public void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		Stopwatch.start("World Render");
		worldRenderer.render();
		Stopwatch.stop();
	}
}
