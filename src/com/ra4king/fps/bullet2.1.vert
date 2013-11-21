#version 120

attribute vec4 position;

varying vec2 mapping;

uniform float scale;
uniform mat4 projectionMatrix, modelViewMatrix;

void main() {
	mapping = position.xy * 2;
	
	mat4 modelView = modelViewMatrix;
	
	modelView[0].x = scale;
	modelView[0].y = 0;
	modelView[0].z = 0;
	
	modelView[1].x = 0;
    modelView[1].y = scale;
    modelView[1].z = 0;
    
    modelView[2].x = 0;
    modelView[2].y = 0;
    modelView[2].z = scale;
	
    gl_Position = projectionMatrix * modelView * position;
}
