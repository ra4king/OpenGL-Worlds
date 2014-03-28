#version 330

struct Bullet {
	vec4 position;
	vec4 color;
};

layout(location = 0) in vec2 vertex;
layout(location = 1) in Bullet bullet; //1 per instance 

out vec2 mapping;
out vec4 bulletColor;

uniform mat4 projectionMatrix, modelViewMatrix;

void main() {
	mapping = vertex * 2;
	
	bulletColor = bullet.color;
	
	gl_Position = projectionMatrix * (modelViewMatrix * vec4(bullet.position.xyz, 1) + vec4(bullet.position.w * vertex, 0, 0));
}
