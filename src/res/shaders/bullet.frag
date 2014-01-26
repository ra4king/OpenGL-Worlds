#version 330

in vec2 mapping;
in vec4 bulletColor;

out vec4 fragColor;

void main() {
	float d = dot(mapping, mapping);
	
	if(d > 1.0)
		discard;
	
	fragColor = vec4(bulletColor.rgb, bulletColor.a * (1.0 - d*d));
}
