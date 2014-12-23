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
public class OpenGLWorlds extends GLProgram {
	public static void main(String[] args) throws Exception {
		// System.setProperty("org.lwjgl.util.Debug", "true");
		
		// -Dnet.indiespot.struct.transform.StructEnv.PRINT_LOG=true
		
		// PrintStream logs = new PrintStream(new FileOutputStream("libstruct-log.txt"));
		// System.setOut(logs);
		// System.setErr(logs);
		
		new OpenGLWorlds().run(true, new PixelFormat(16, 0, 8, 0, 4));// , new ContextAttribs(4, 4).withDebug(true).withProfileCore(true));
	}
	
	private World world;
	private WorldRenderer worldRenderer;
	
	// private Fractal fractal;
	
	public OpenGLWorlds() {
		super("OpenGLWorlds", 800, 600, true);
	}
	
	@Override
	public void init() {
		System.out.println(glGetString(GL_VERSION));
		System.out.println(glGetString(GL_VENDOR));
		System.out.println(glGetString(GL_RENDERER));
		
		setPrintDebug(true);
		setFPS(200);
		
		// glEnable(GL_DEBUG_OUTPUT);
		//
		// glDebugMessageCallback(new KHRDebugCallback((int source, int type, int id, int severity, String message) ->
		// System.out.println("GL debug: " + message)));
		
		GLUtils.init();
		
		if(GLUtils.GL_VERSION < 21) {
			System.out.println("Your OpenGL version is too old.");
			System.exit(1);
		}
		
		// Mouse.setGrabbed(true);
		
		world = new World();
		worldRenderer = new WorldRenderer(this, world);
		
		world.generateRandomBlocks();
		// world.fillAll();
	}
	
	@Override
	public void resized() {
		super.resized();
		
		world.resized();
		worldRenderer.resized();
	}
	
	@Override
	public boolean shouldStop() {
		return false;
	}
	
	@Override
	public void keyPressed(int key, char c) {
		if(key == Keyboard.KEY_ESCAPE)
			Mouse.setGrabbed(!Mouse.isGrabbed());
		
		if(key == Keyboard.KEY_G) {
			world.clearAll();
			world.generateRandomBlocks();
		}
		
		if(key == Keyboard.KEY_C)
			world.clearAll();
		
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
		Stopwatch.start("World Render");
		worldRenderer.render();
		Stopwatch.stop();
	}
}
