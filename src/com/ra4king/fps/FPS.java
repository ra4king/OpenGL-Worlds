package com.ra4king.fps;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.PixelFormat;

import com.ra4king.fps.renderers.WorldRenderer;
import com.ra4king.fps.world.World;
import com.ra4king.opengl.util.GLProgram;

/**
 * @author ra4king
 */
public class FPS extends GLProgram {
	public static void main(String[] args) throws Exception {
		if(args.length > 0 && args[0].equals("ide")) {
			String os = System.getProperty("os.name").toLowerCase();
			
			String libraryPath;
			if(os.contains("win"))
				libraryPath = "E:/Roi Atalla/";
			else if(os.contains("linux"))
				libraryPath = "/home/ra4king/Dropbox/";
			else {
				System.out.println("System not supported!");
				return;
			}
			
			libraryPath += "Documents/Programming Files/Java Files/Personal Projects/Libraries/lwjgl/natives/";
			
			System.setProperty("org.lwjgl.librarypath", libraryPath);
		}
		
		new FPS().run(new PixelFormat(16, 0, 8, 0, 4));
	}
	
	private World world;
	private WorldRenderer worldRenderer;
	
	// private Fractal fractal;
	
	public FPS() {
		super("FPS", 1280, 800, true);
	}
	
	@Override
	public void init() {
		GLUtils.get();
		
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
	public void update(long deltaTime) {
		world.update(deltaTime);
		worldRenderer.update(deltaTime);
	}
	
	@Override
	public void keyPressed(int key, char c) {
		if(key == Keyboard.KEY_ESCAPE)
			Mouse.setGrabbed(!Mouse.isGrabbed());
		
		world.keyPressed(key, c);
	}
	
	@Override
	public boolean shouldStop() {
		return false;
	}
	
	@Override
	public void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		worldRenderer.render();
	}
}
