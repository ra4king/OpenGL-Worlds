#version 330

layout(location = 0) in vec4 position;

uniform mat4 projectionMatrix, viewMatrix;

void main() {
	gl_Position = projectionMatrix * viewMatrix * position;
}
