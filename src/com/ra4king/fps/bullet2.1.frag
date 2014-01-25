#version 120

varying vec2 mapping;
varying vec4 bulletColor;

void main() {
	float d = dot(mapping, mapping);
	
	if(d > 1.0)
		discard;
	
	gl_FragColor = vec4(bulletColor.rgb, bulletColor.a * (1.0 - d*d));
}
