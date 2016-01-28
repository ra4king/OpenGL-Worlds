#version 420

flat in vec3 position;
flat in float range;
flat in vec3 color;
flat in float k;

uniform sampler2D cubeTexture;

uniform sampler2D cameraPositions;
uniform sampler2D normals;
uniform sampler2D texCoords;
uniform sampler2D depth;

out vec4 fragColor;

const float fogRange = -1.0 / 200.0;
const float specularExponent = 0.01;

vec3 calculateLight(vec3 color, float k, vec3 normal, vec3 lightDistance) {
	vec3 lightDirection = normalize(lightDistance);
	float cos = clamp(dot(normal, lightDirection), 0, 1);
	float lightDot = dot(lightDistance, lightDistance);
	float atten = 1.0 / (1.0 + k * lightDot);
	
	return color * atten * cos;
}

void main() {
	vec2 tex = gl_FragCoord.xy;
	
//	tex.x += cos(tex.y * 30.0) / 50;
//	tex.y += sin(tex.x * 30.0) / 50;
	
	ivec2 texi = ivec2(tex);
	
	vec3 cameraSpacePosition = texelFetch(cameraPositions, texi, 0).xyz;
	
	if(cameraSpacePosition == vec3(0.0))
		discard;
	
	vec3 normal = normalize(texelFetch(normals, texi, 0).xyz);
	vec2 texCoord = texelFetch(texCoords, texi, 0).st;
	gl_FragDepth = texelFetch(depth, texi, 0).x;
	
	vec3 lightDistance = position - cameraSpacePosition;
	if(range > 0.0 && dot(lightDistance, lightDistance) > range * range)
		discard;
	
	vec3 totalLight = calculateLight(color, k, normal, lightDistance);
	
	//float fog = clamp(1.0 - cameraSpacePosition.z * fogRange, 0.1, 1.0);
	
	vec4 gamma = vec4(1.0 / 2.2);
	gamma.w = 1;
	fragColor = pow(vec4(mix(color, texture(cubeTexture, texCoord).rgb * totalLight, 1), 1), gamma);
}
