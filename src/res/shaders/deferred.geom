#version 440

layout(points) in;
layout(triangle_strip, max_vertices=4) out;

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
	
	gl_Position = _range[0] == 0.0 ? vec4(1.0, 1.0, 0.0, 1.0) : projectionMatrix * (gl_in[0].gl_Position + right + up);
	position = gl_in[0].gl_Position.xyz;
	range = _range[0];
	color = _color[0];
	k = _k[0];
	EmitVertex();
	
	gl_Position = _range[0] == 0.0 ? vec4(1.0, -1.0, 0.0, 1.0) : projectionMatrix * (gl_in[0].gl_Position + right - up);
	position = gl_in[0].gl_Position.xyz;
	range = _range[0];
	color = _color[0];
	k = _k[0];
	EmitVertex();
	
	gl_Position = _range[0] == 0.0 ? vec4(-1.0, 1.0, 0.0, 1.0) : projectionMatrix * (gl_in[0].gl_Position - right + up);
	position = gl_in[0].gl_Position.xyz;
	range = _range[0];
	color = _color[0];
	k = _k[0];
	EmitVertex();
	
	gl_Position = _range[0] == 0.0 ? vec4(-1.0, -1.0, 0.0, 1.0) : projectionMatrix * (gl_in[0].gl_Position - right - up);
	position = gl_in[0].gl_Position.xyz;
	range = _range[0];
	color = _color[0];
	k = _k[0];
	EmitVertex();
}
