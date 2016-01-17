#version 440

layout(location = 0) in vec3 cubePos; // XYZ: index
layout(location = 1) in uint cubeType;

void main() {
	gl_Position = vec4(cubePos, 1.0);
}
