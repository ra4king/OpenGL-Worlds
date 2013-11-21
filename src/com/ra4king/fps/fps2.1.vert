#version 120

attribute vec4 position;
attribute vec3 normal;

varying vec3 cameraSpacePosition;
varying vec3 norm;

uniform mat4 projectionMatrix, viewMatrix, modelMatrix;

void main() {
	mat4 modelView = viewMatrix * modelMatrix;
	
	cameraSpacePosition = vec3(modelView * position);
    gl_Position = projectionMatrix * vec4(cameraSpacePosition, 1);
    norm = mat3(modelView) * normal;
}
