package com.ra4king.fps;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_CW;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_RENDERER;
import static org.lwjgl.opengl.GL11.GL_VENDOR;
import static org.lwjgl.opengl.GL11.GL_VERSION;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glFrontFace;
import static org.lwjgl.opengl.GL11.glGetString;
import static org.lwjgl.opengl.GL32.GL_DEPTH_CLAMP;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.HashMap;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.PixelFormat;

import com.ra4king.fps.actors.Portal;
import com.ra4king.fps.renderers.Resources;
import com.ra4king.fps.renderers.WorldRenderer;
import com.ra4king.fps.world.Chunk;
import com.ra4king.fps.world.World;
import com.ra4king.opengl.util.GLProgram;
import com.ra4king.opengl.util.PNGDecoder;
import com.ra4king.opengl.util.PNGDecoder.Format;
import com.ra4king.opengl.util.Stopwatch;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.math.Quaternion;
import com.ra4king.opengl.util.math.Vector2;
import com.ra4king.opengl.util.math.Vector3;
import com.ra4king.opengl.util.math.Vector4;
import com.ra4king.opengl.util.render.MonospaceFont;
import com.ra4king.opengl.util.render.PerformanceGraph;
import com.ra4king.opengl.util.render.RenderUtils;

/**
 * @author Roi Atalla
 */
public class OpenGLWorlds extends GLProgram {
	public static void main(String[] args) {
		// System.setProperty("org.lwjgl.util.Debug", "true");

		// -Dnet.indiespot.struct.transform.StructEnv.PRINT_LOG=true

		// PrintStream logs = new PrintStream(new FileOutputStream("libstruct-log.txt"));
		// System.setOut(logs);
		// System.setErr(logs);

		new OpenGLWorlds().run(4, 3, true, new PixelFormat(24, 0, 24, 8, 4));// , new ContextAttribs(4, 4).withDebug
		// (true).withProfileCore(true));
	}

	private final int WORLD_COUNT = 2;

	private Camera camera;

	private HashMap<World, WorldRenderer> worldsMap;

	private World[] worlds;
	private WorldRenderer[] worldRenderers;
	private int currentWorld;

	private MonospaceFont font;

	private boolean showPerformanceGraphs = true;
	private PerformanceGraph performanceGraphUpdate;
	private PerformanceGraph performanceGraphRender;
	private PerformanceGraph performanceGraphChunkRenderers;
	private PerformanceGraph performanceGraphUpdateCompactArray;
	private PerformanceGraph performanceGraphLightSystemRender;
	private PerformanceGraph performanceGraphBulletRender;
	private PerformanceGraph performanceGraphDisplayUpdate;
	private PerformanceGraph performanceGraphFPS;

	// private Fractal fractal;

	public OpenGLWorlds() {
		super("OpenGLWorlds", 800, 600, true);
	}

	public Camera getCamera() {
		return camera;
	}

	@Override
	public void init() {
		System.out.println(glGetString(GL_VERSION));
		System.out.println(glGetString(GL_VENDOR));
		System.out.println(glGetString(GL_RENDERER));

		printDebug(true);
		checkError(false);
		setFPS(0);

		RenderUtils.init();

		if (RenderUtils.GL_VERSION < 21) {
			System.out.println("Your OpenGL version is too old.");
			System.exit(1);
		}

		glEnable(GL_DEPTH_TEST);

		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glFrontFace(GL_CW);

		glEnable(GL_DEPTH_CLAMP);

		worldsMap = new HashMap<>();

		// Mouse.setGrabbed(true);

		worlds = new World[WORLD_COUNT];
		worldRenderers = new WorldRenderer[WORLD_COUNT];

		camera = new Camera(60, 1, 5000);
		resetCamera();

		for (int a = 0; a < WORLD_COUNT; a++) {
			worlds[a] = new World(3, 2, 2);
			worldRenderers[a] = new WorldRenderer(this, worlds[a]);
			worlds[a].generateAllBlocks();//generateRandomBlocks();
			worldsMap.put(worlds[a], worldRenderers[a]);
		}

		Portal portal1 =
			new Portal(this, worlds[0], new Vector3(0, 0, 10), new Vector2(10, 20), new Quaternion(), worlds[1]);
		Portal portal2 = new Portal(this,
		                            worlds[1],
		                            new Vector3(10, 0, 20),
		                            new Vector2(10, 20),
		                            new Quaternion((float)Math.PI * 0.25f, Vector3.UP).mult(new Quaternion(
			                            (float)Math.PI * 0.25f,
			                            Vector3.RIGHT)),
		                            worlds[0]);
		portal1.setDestPortal(portal2);
		portal2.setDestPortal(portal1);

		worlds[0].addActor(portal1);
		worlds[1].addActor(portal2);

		for (int a = 0; a < WORLD_COUNT; a++) {
			worldRenderers[a].loadActors();
		}

		currentWorld = 0;
		camera.setCameraUpdate(worlds[currentWorld]);

		loadFont();

		final float maxValue = 10.0f;
		final int graphX = 100, graphY = 100, maxSteps = 100, stepSize = 5, graphHeight = 300;
		performanceGraphUpdate = new PerformanceGraph(maxValue,
		                                              graphX,
		                                              graphY,
		                                              maxSteps,
		                                              stepSize,
		                                              graphHeight,
		                                              new Vector4(0, 0, 1, 1),
		                                              () -> Stopwatch.getTimePerFrame("Update")); // Blue
		performanceGraphRender = new PerformanceGraph(maxValue,
		                                              graphX,
		                                              graphY,
		                                              maxSteps,
		                                              stepSize,
		                                              graphHeight,
		                                              new Vector4(0, 1, 1, 1),
		                                              () -> Stopwatch.getTimePerFrame("Render")); // Cyan
		performanceGraphChunkRenderers = new PerformanceGraph(maxValue,
		                                                      graphX,
		                                                      graphY,
		                                                      maxSteps,
		                                                      stepSize,
		                                                      graphHeight,
		                                                      new Vector4(0.5f, 0.5f, 0.5f, 1),
		                                                      () -> Stopwatch.getTimePerFrame("ChunkRenderers")); // 
		// gray
		performanceGraphUpdateCompactArray = new PerformanceGraph(maxValue,
		                                                          graphX,
		                                                          graphY,
		                                                          maxSteps,
		                                                          stepSize,
		                                                          graphHeight,
		                                                          new Vector4(1, 0, 0, 1),
		                                                          () -> Stopwatch.getTimePerFrame(
			                                                          "Update Compact " + "Array")); // Red
		performanceGraphLightSystemRender = new PerformanceGraph(maxValue,
		                                                         graphX,
		                                                         graphY,
		                                                         maxSteps,
		                                                         stepSize,
		                                                         graphHeight,
		                                                         new Vector4(1, 1, 0, 1),
		                                                         () -> Stopwatch.getTimePerFrame(
			                                                         "LightSystem render UBO")); // Orange
		performanceGraphBulletRender = new PerformanceGraph(maxValue,
		                                                    graphX,
		                                                    graphY,
		                                                    maxSteps,
		                                                    stepSize,
		                                                    graphHeight,
		                                                    new Vector4(1, 1, 1, 1),
		                                                    () -> Stopwatch.getTimePerFrame("BulletRenderer")); // 
		// White
		performanceGraphDisplayUpdate = new PerformanceGraph(maxValue,
		                                                     graphX,
		                                                     graphY,
		                                                     maxSteps,
		                                                     stepSize,
		                                                     graphHeight,
		                                                     new Vector4(1, 0, 1, 1),
		                                                     () -> Stopwatch.getTimePerFrame("Display.update()")); // 
		// Magenta
		performanceGraphFPS = new PerformanceGraph(200,
		                                           graphX,
		                                           graphY,
		                                           maxSteps,
		                                           stepSize,
		                                           graphHeight,
		                                           new Vector4(0, 1, 0, 1),
		                                           this::getLastFps); // Green
	}

	private void loadFont() {
		String file;
		int charWidth;
		String characters;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(Resources.getInputStream(
			"textures/font" + ".fnt")))) {
			file = reader.readLine().trim();
			charWidth = Integer.parseInt(reader.readLine().trim());
			characters = reader.readLine().trim();
		} catch (Exception exc) {
			throw new RuntimeException(exc);
		}

		ByteBuffer data;
		int imageWidth, imageHeight;
		try {
			PNGDecoder imageDecoder = new PNGDecoder(Resources.getInputStream("textures/" + file));
			imageWidth = imageDecoder.getWidth();
			imageHeight = imageDecoder.getHeight();
			data = BufferUtils.createByteBuffer(imageWidth * imageHeight * 4);
			imageDecoder.decode(data, imageWidth * 4, Format.RGBA);
			data.flip();
		} catch (Exception exc) {
			throw new RuntimeException(exc);
		}

		font = MonospaceFont.init("DejaVu-Sans-Mono", charWidth, imageWidth, imageHeight, data, characters);
	}

	public WorldRenderer getRenderer(World world) {
		return worldsMap.get(world);
	}

	@Override
	public void resized() {
		super.resized();

		camera.setWindowSize(RenderUtils.getWidth(), RenderUtils.getHeight());

		for (int a = 0; a < WORLD_COUNT; a++) {
			worldRenderers[a].resized();
		}
	}

	@Override
	public boolean shouldStop() {
		return false;
	}

	@Override
	public void keyPressed(int key, char c) {
		if (key == Keyboard.KEY_ESCAPE) {
			Mouse.setGrabbed(!Mouse.isGrabbed());
		}

		if (key == Keyboard.KEY_G) {
			worlds[currentWorld].clearAll();
			worlds[currentWorld].generateRandomBlocks();
		}

		if (key == Keyboard.KEY_H) {
			worlds[currentWorld].clearAll();
		}

		worlds[currentWorld].keyPressed(key, c);

		if (key == Keyboard.KEY_1) {
			currentWorld = 0;
			camera.setCameraUpdate(worlds[currentWorld]);
		}
		if (key == Keyboard.KEY_2) {
			currentWorld = 1;
			camera.setCameraUpdate(worlds[currentWorld]);
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_R)) {
			resetCamera();
		}

		if (key == Keyboard.KEY_O) {
			showPerformanceGraphs = !showPerformanceGraphs;
		}
	}

	public void resetCamera() {
		camera.setPosition(new Vector3(-Chunk.BLOCK_SIZE, -Chunk.BLOCK_SIZE, Chunk.BLOCK_SIZE).mult(5));
		Utils.lookAt(camera.getPosition(), Vector3.ZERO, Vector3.UP).toQuaternion(camera.getOrientation()).normalize();
	}

	public World getWorld() {
		return worlds[currentWorld];
	}

	public void setWorld(World world) {
		for (int i = 0; i < worlds.length; i++) {
			if (world == worlds[i]) {
				currentWorld = i;
				camera.setCameraUpdate(worlds[currentWorld]);
				break;
			}
		}
	}

	@Override
	public void update(long deltaTime) {
		Stopwatch.start("Camera Update");
		camera.update(deltaTime);
		Stopwatch.stop();

		Stopwatch.start("World Update");
		for (World w : worlds) {
			w.update(deltaTime);
		}
		Stopwatch.stop();

		Stopwatch.start("WorldRenderer Update");
		worldRenderers[currentWorld].update(deltaTime);
		Stopwatch.stop();

		performanceGraphUpdate.update(deltaTime);
		performanceGraphRender.update(deltaTime);
		performanceGraphChunkRenderers.update(deltaTime);
		performanceGraphUpdateCompactArray.update(deltaTime);
		performanceGraphLightSystemRender.update(deltaTime);
		performanceGraphBulletRender.update(deltaTime);
		performanceGraphDisplayUpdate.update(deltaTime);
		performanceGraphFPS.update(deltaTime);
	}

	@Override
	public void render() {
		Stopwatch.start("World Render");
		worldRenderers[currentWorld].render(null, null, 0, camera);
		Stopwatch.stop();

		if (showPerformanceGraphs) {
			Stopwatch.start("Performance Graphs Render");
			performanceGraphUpdate.render();
			performanceGraphRender.render();
			performanceGraphChunkRenderers.render();
			performanceGraphUpdateCompactArray.render();
			performanceGraphLightSystemRender.render();
			performanceGraphBulletRender.render();
			performanceGraphDisplayUpdate.render();
			performanceGraphFPS.render();
			Stopwatch.stop();
		}

		font.render(getLastFps() + " FPS", 100, 75, 20, new Vector4(0, 1, 0, 1));
		font.render(
			String.format("Update: %.2f ms", Stopwatch.getTimePerFrame("Update")),
			100,
			55,
			20,
			new Vector4(1, 0.5f, 0, 1));
		font.render(
			String.format("Render: %.2f ms", Stopwatch.getTimePerFrame("Render")),
			100,
			35,
			20,
			new Vector4(0, 1, 1, 1));
		font.render(
			String.format("Display.update(): %.2f ms", Stopwatch.getTimePerFrame("Display.update()")),
			100,
			15,
			20,
			new Vector4(1, 0, 1, 1));

		font.render(
			String.format("Update Compact Array: %.2f ms", Stopwatch.getTimePerFrame("Update Compact Array")),
			360,
			75,
			20,
		            new Vector4(1, 0, 0, 1));
		font.render(String.format("Bullet Render: %.2f ms", Stopwatch.getTimePerFrame("BulletRenderer")),
		            360,
		            55,
		            20,
		            new Vector4(1, 1, 1, 1));
		font.render(String.format("Light System Render: %.2f ms", Stopwatch.getTimePerFrame("LightSystem render UBO")),
		            360,
		            35,
		            20,
		            new Vector4(1, 1, 0, 1));
		font.render(String.format("Chunk Render: %.2f ms", Stopwatch.getTimePerFrame("ChunkRenderers")),
		            360,
		            15,
		            20,
		            new Vector4(0.5f, 0.5f, 0.5f, 1));

		font.render("Position: " + camera.getPosition().toString(), 20, Display.getHeight() - 40, 20, new Vector4(1));

		int totalChunksRendered = 0, totalBlocksRendered = 0;
		for (WorldRenderer renderer : worldRenderers) {
			totalChunksRendered += renderer.getChunksRenderedCount();
			totalBlocksRendered += renderer.getBlocksRenderedCount();
		}

		font.render("Chunks visible: " + totalChunksRendered + ", Total cubes rendered: " + totalBlocksRendered,
		            20,
		            Display.getHeight() - 60,
		            20,
		            new Vector4(1));
	}
}
