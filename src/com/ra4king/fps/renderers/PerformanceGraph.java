package com.ra4king.fps.renderers;

import static com.ra4king.fps.GLUtils.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import com.ra4king.fps.GLUtils;
import com.ra4king.opengl.util.ShaderProgram;
import com.ra4king.opengl.util.Stopwatch;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.Vector4;

/**
 * @author Roi Atalla
 */
public class PerformanceGraph {
	private String stopWatchName;
	private float maxValue;
	
	private int x, y;
	private int width, height;
	private int maxSteps, stepWidth;
	
	private Vector4 color;
	
	private ShaderProgram uiProgram;
	private int vbo, vao;
	
	private FloatBuffer graphData;
	private int graphOffset;
	private int stepCount;
	
	public PerformanceGraph(String stopWatchName, float maxValue, int x, int y, int maxSteps, int stepWidth, int graphHeight, Vector4 color) {
		this.stopWatchName = stopWatchName;
		this.maxValue = maxValue;
		
		this.x = x;
		this.y = y;
		this.width = maxSteps * stepWidth;
		this.height = graphHeight;
		this.maxSteps = maxSteps;
		this.stepWidth = stepWidth;
		
		this.color = color.copy();
		
		init();
	}
	
	private void init() {
		uiProgram = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream(RESOURCES_ROOT_PATH + "shaders/perf_graph.vert")),
				Utils.readFully(getClass().getResourceAsStream(RESOURCES_ROOT_PATH + "shaders/perf_graph.frag")));
		
		vao = glGenVertexArrays();
		glBindVertexArray(vao);
		
		float[] graph = {
				x, y,
				x, y + height,
				x, y,
				x + width, y
		};
		
		graphOffset = graph.length;
		
		graphData = BufferUtils.createFloatBuffer(maxSteps * 2);
		stepCount = 0;
		
		vbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, (graphOffset + graphData.capacity()) * Float.BYTES, GL_STREAM_DRAW);
		
		glBufferSubData(GL_ARRAY_BUFFER, 0, (FloatBuffer)BufferUtils.createFloatBuffer(graph.length).put(graph).flip());
		
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
		
		Matrix4 projectionMatrix = new Matrix4().clearToOrtho(0, GLUtils.getWidth(), 0, GLUtils.getHeight(), 0, 1);
		
		uiProgram.begin();
		glUniformMatrix4(uiProgram.getUniformLocation("projectionMatrix"), false, projectionMatrix.toBuffer());
		glUniform4(uiProgram.getUniformLocation("color"), color.toBuffer());
		uiProgram.end();
	}
	
	private long elapsedTime;
	
	public void update(long deltaTime) {
		elapsedTime += deltaTime;
		
		while(elapsedTime >= 1e9) {
			elapsedTime -= 1e9;
			
			graphData.clear();
			
			if(stepCount < maxSteps) {
				stepCount++;
			}
			
			for(int a = stepCount * 2 - 2; a >= 2; a -= 2) {
				graphData.put(a, graphData.get(a - 2) - stepWidth);
				graphData.put(a + 1, graphData.get(a - 1));
			}
			
			float stepHeight = (float)(Stopwatch.getTimePerFrame(stopWatchName) / maxValue * height);
			
			if(Float.isNaN(stepHeight))
				stepHeight = 0;
			
			graphData.put(0, x + width);
			graphData.put(1, y + stepHeight);
			
			System.out.println("Max Value: " + maxValue + ", Step height: " + stepHeight);
			
			glBindBuffer(GL_ARRAY_BUFFER, vbo);
			glBufferSubData(GL_ARRAY_BUFFER, graphOffset * Float.BYTES, graphData);
			glBindBuffer(GL_ARRAY_BUFFER, 0);
		}
	}
	
	public void render() {
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_CULL_FACE);
		glDisable(GL_BLEND);
		
		uiProgram.begin();
		
		glBindVertexArray(vao);
		glDrawArrays(GL_LINES, 0, graphOffset / 2);
		glDrawArrays(GL_LINE_STRIP, graphOffset / 2, stepCount);
		glBindVertexArray(0);
		
		uiProgram.end();
		
		glEnable(GL_BLEND);
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
	}
}
