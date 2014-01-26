#version 120

const vec2 maps[] = vec2[6](vec2(-0.5f, 0.5f), vec2(0.5f, 0.5f), vec2(0.5f, -0.5f),
							vec2(0.5f, -0.5f), vec2(-0.5f, -0.5f), vec2(-0.5f, 0.5f));

varying vec2 mapping;
varying vec4 bulletColor;

uniform mat4 projectionMatrix, modelViewMatrix;

#define BULLET_COUNT 2000

struct Bullet {
	vec4 position;
	vec4 color;
};

uniform Bullet bulletData[BULLET_COUNT];

void main() {
	int mapIndex = gl_VertexID % 6;
	int dataIndex = gl_VertexID / 6;
	
	mapping = maps[mapIndex] * 2;
	
	vec4 position = bulletData[dataIndex].position;
	vec4 color = bulletData[dataIndex].color;
	
	bulletColor = color;
	
	gl_Position = projectionMatrix * (modelViewMatrix * vec4(position.xyz, 1) + vec4(position.w * maps[mapIndex], 0, 0));
}
