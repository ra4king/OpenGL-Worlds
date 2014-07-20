#version 120

attribute vec4 position;
attribute vec3 normal;
//attribute vec2 tex;

varying vec3 cameraSpacePosition;
varying vec3 norm;
//varying vec2 texCoord;

uniform mat4 projectionMatrix, viewMatrix;
uniform mat3 normalMatrix;

void main() {
	vec4 cameraPos = viewMatrix * position;
	
	cameraSpacePosition = vec3(cameraPos);
	gl_Position = projectionMatrix * cameraPos;
	
	norm = normalMatrix * normal;
	
	//tex = texCoord;
}
