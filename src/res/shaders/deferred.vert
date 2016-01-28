#version 440

layout(location = 0) in vec3 position;
layout(location = 1) in float range; // range == 0, color = ambient
layout(location = 2) in vec3 color;
layout(location = 3) in float k;

out vec3 _position;
out float _range;
out vec3 _color;
out float _k;

void main() {
	_position = position;
	_range = range;
	_color = color;
	_k = k;
}
