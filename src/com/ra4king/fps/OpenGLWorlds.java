package com.ra4king.fps;

import static org.lwjgl.opengl.GL11.*;

import java.util.HashMap;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.PixelFormat;

import com.ra4king.fps.renderers.WorldRenderer;
import com.ra4king.fps.world.World;
import com.ra4king.opengl.util.GLProgram;
import com.ra4king.opengl.util.Stopwatch;
import com.ra4king.opengl.util.math.Vector2;
import com.ra4king.opengl.util.math.Vector3;

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
		
		new OpenGLWorlds().run(true, new PixelFormat(16, 0, 8, 8, 4));// , new ContextAttribs(4, 4).withDebug(true).withProfileCore(true));
	}
	
	private final int WORLD_COUNT = 2;
	
	private HashMap<World,WorldRenderer> worldsMap;
	
	private World[] worlds;
	private WorldRenderer[] worldRenderers;
	private int currentWorld;
	
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
		setFPS(0);
		
		GLUtils.init();
		
		if(GLUtils.GL_VERSION < 21) {
			System.out.println("Your OpenGL version is too old.");
			System.exit(1);
		}
		
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		glEnable(GL_DEPTH_TEST);
		
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glFrontFace(GL_CW);
		
		worldsMap = new HashMap<>();
		
		// Mouse.setGrabbed(true);
		
		worlds = new World[WORLD_COUNT];
		worldRenderers = new WorldRenderer[WORLD_COUNT];
		
		for(int a = 0; a < WORLD_COUNT; a++) {
			worlds[a] = new World(3, 3, 3);
			
			if(a == 1)
				worlds[a].addPortal(new Vector3(0, 0, 0), new Vector2(3, 5), worlds[0]);
			
			worldRenderers[a] = new WorldRenderer(this, worlds[a]);
			worlds[a].generateRandomBlocks();
			
			worldsMap.put(worlds[a], worldRenderers[a]);
		}
		
		currentWorld = 0;
		// worlds[currentWorld].setActive(true);
	}
	
	public WorldRenderer getRenderer(World world) {
		return worldsMap.get(world);
	}
	
	@Override
	public void resized() {
		super.resized();
		
		for(int a = 0; a < WORLD_COUNT; a++) {
			worlds[a].resized();
			worldRenderers[a].resized();
		}
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
			worlds[currentWorld].clearAll();
			worlds[currentWorld].generateRandomBlocks();
		}
		
		if(key == Keyboard.KEY_H)
			worlds[currentWorld].clearAll();
		
		worlds[currentWorld].keyPressed(key, c);
		worldRenderers[currentWorld].keyPressed(key, c);
		
		if(key == Keyboard.KEY_1) {
			// worlds[currentWorld].setActive(false);
			currentWorld = 0;
			// worlds[currentWorld].setActive(true);
		}
		if(key == Keyboard.KEY_2) {
			// worlds[currentWorld].setActive(false);
			currentWorld = 1;
			// worlds[currentWorld].setActive(true);
		}
		if(key == Keyboard.KEY_3) {
			// worlds[currentWorld].setActive(false);
			currentWorld = 2;
			// worlds[currentWorld].setActive(true);
		}
	}
	
	@Override
	public void update(long deltaTime) {
		Stopwatch.start("World Update");
		worlds[currentWorld].update(deltaTime);
		Stopwatch.stop();
		
		Stopwatch.start("WorldRenderer Update");
		worldRenderers[currentWorld].update(deltaTime);
		Stopwatch.stop();
	}
	
	@Override
	public void render() {
		glClearColor(0.4f, 0.6f, 0.9f, 0f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
		
		Stopwatch.start("World Render");
		worldRenderers[currentWorld].render();
		Stopwatch.stop();
	}
}
