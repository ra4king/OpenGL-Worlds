#version 330

in vec2 mapping;
in vec4 bulletColor;

out vec4 fragColor;

void main() {
	float d = dot(mapping, mapping);
	
	if(d > 1.0)
		discard;
	
	float a = 0.8 - d * d;
	fragColor = vec4(bulletColor.rgb, bulletColor.a * a);
}
