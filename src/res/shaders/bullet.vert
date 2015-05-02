#version 330

layout(location = 0) in vec2 vertex;
layout(location = 1) in vec4 position; // 1 per instance
layout(location = 2) in vec4 color;    // 1 per instance

out vec2 mapping;
out vec4 bulletColor;

uniform mat4 projectionMatrix, modelViewMatrix;

void main() {
	mapping = vertex * 2;
	
	bulletColor = color;
	
	vec2 size = position.w * vertex;
	gl_Position = projectionMatrix * (modelViewMatrix * vec4(position.xyz, 1) + vec4(size, 0, 0));
}
