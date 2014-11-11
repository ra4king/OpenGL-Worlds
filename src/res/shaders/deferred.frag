#version 420

uniform vec2 resolution;

uniform sampler2D cubeTexture;

uniform sampler2D cameraPositions;
uniform sampler2D normals;
uniform sampler2D texCoords;
uniform sampler2D depth;

struct PointLight {
	vec3 position;
	float range;
	
	vec3 color;
	float k;
};

#define MAX_LIGHTS 200

layout(std140) uniform Lights {
	vec3 ambientLight;
	float numberOfLights;
	PointLight lights[MAX_LIGHTS];
};

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
	vec2 tex = gl_FragCoord.xy / resolution;
	
	vec3 cameraSpacePosition = texture(cameraPositions, tex).xyz;
	vec3 normal = normalize(texture(normals, tex).xyz);
	vec2 texCoord = texture(texCoords, tex).st;
	gl_FragDepth = texture(depth, tex).x;
	
	if(cameraSpacePosition == vec3(0.0)) {
		discard;
	}
	
	vec3 totalLight = ambientLight;
	
	for(int a = 0; a < numberOfLights; a++) {
		PointLight light = lights[a];
		
		vec3 lightDistance = light.position - cameraSpacePosition;
		
		if(dot(lightDistance, lightDistance) <= light.range * light.range) {
			totalLight += calculateLight(light.color, light.k, normal, lightDistance) * 0.55;
		}
	}
	
	float fog = clamp(1.0 - cameraSpacePosition.z * fogRange, 0.1, 1.0);
	
	#ifdef RGB_WAVE
	float redWave = clamp(2.0 * sin(cameraSpacePosition.x + cameraSpacePosition.y) - 1.0, 0.0, 1.0);
	float greenWave = clamp(2.0 * sin(cameraSpacePosition.x + cameraSpacePosition.z) - 1.0, 0.0, 1.0);
	float blueWave = clamp(2.0 * sin(cameraSpacePosition.y + cameraSpacePosition.z) - 1.0, 0.0, 1.0);
	#endif
	#ifdef SINE_WAVE
	float sineWave = clamp(2.0 * sin(cameraSpacePosition.x + cameraSpacePosition.y) - 1.0, 0.0, 1.0);
	#endif
	
	vec4 gamma = vec4(1.0 / 2.2);
	gamma.w = 1;
	fragColor = pow(vec4(texture(cubeTexture, texCoord).rgb * totalLight * fog
		#ifdef RGB_WAVE
		* vec3(redWave, greenWave, blueWave)
		#endif
		#ifdef SINE_WAVE
		vec3(sineWave, sineWave, sineWave)
		#endif
		, 1), gamma);
}
