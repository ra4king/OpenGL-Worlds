#version 120

varying vec2 mapping;

uniform vec3 bulletColor;
uniform float alpha;

void main() {
	float d = dot(mapping, mapping);
	
	if(d > 1)
		discard;
	
	gl_FragColor = vec4(bulletColor, alpha * (1 - d*d));
}
