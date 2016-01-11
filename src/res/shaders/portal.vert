#version 330

layout(location = 0) in vec4 position;

out vec2 mapping;

uniform mat4 projectionMatrix, viewMatrix;
uniform vec2 size;

void main() {
	mapping = (2.0 * position.xy - size) / size;

	gl_Position = projectionMatrix * viewMatrix * position;
}
