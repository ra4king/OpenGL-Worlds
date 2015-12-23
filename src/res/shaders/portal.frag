#version 440

uniform float a;

out vec4 fragColor;

void main() {
	fragColor = vec4(0.6, 0.8, 1.0, a);
}
