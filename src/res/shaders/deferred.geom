#version 440

layout(points) in;
layout(triangle_strip, max_vertices=4) out;

in vec3 _position[];
in float _range[];
in vec3 _color[];
in float _k[];

flat out vec3 position;
flat out float range;
flat out vec3 color;
flat out float k;

uniform mat4 projectionMatrix;

void main() {
	vec4 right = vec4(_range[0], 0, 0, 0);
	vec4 up = vec4(0, _range[0], 0, 0);
	
	gl_Position = _range[0] == 0.0 ? vec4(1.0, 1.0, 0.0, 1.0) : projectionMatrix * (vec4(_position[0], 1) + right + up);
	gl_Position.z = 0.0;
	position = _position[0];
	range = _range[0];
	color = _color[0];
	k = _k[0];
	EmitVertex();
	
	gl_Position = _range[0] == 0.0 ? vec4(1.0, -1.0, 0.0, 1.0) : projectionMatrix * (vec4(_position[0], 1) + right - up);
	gl_Position.z = 0.0;
	position = _position[0];
	range = _range[0];
	color = _color[0];
	k = _k[0];
	EmitVertex();
	
	gl_Position = _range[0] == 0.0 ? vec4(-1.0, 1.0, 0.0, 1.0) : projectionMatrix * (vec4(_position[0], 1) - right + up);
	gl_Position.z = 0.0;
	position = _position[0];
	range = _range[0];
	color = _color[0];
	k = _k[0];
	EmitVertex();
	
	gl_Position = _range[0] == 0.0 ? vec4(-1.0, -1.0, 0.0, 1.0) : projectionMatrix * (vec4(_position[0], 1) - right - up);
	gl_Position.z = 0.0;
	position = _position[0];
	range = _range[0];
	color = _color[0];
	k = _k[0];
	EmitVertex();
}
