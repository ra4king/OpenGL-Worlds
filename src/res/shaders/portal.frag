#version 440

in vec2 mapping;

uniform sampler2D portalTex;

uniform vec2 resolution;

out vec4 fragColor;

void main() {
	float d = dot(mapping, mapping);
    
    if(d > 1.0)
        discard;
    
    float a = 1.0 - d * d;
    
    float a2 = pow(d * d, 4.0);
    
	vec2 coord = gl_FragCoord.xy / resolution;
	fragColor = vec4(texture(portalTex, coord).xyz * a, 1.0) + vec4(a2, 0.0, 0.0, 0.0);
}
