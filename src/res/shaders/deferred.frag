#version 420

uniform vec2 resolution;

in vec3 cameraSpacePosition;
in vec3 norm;
in vec2 texCoord;

//uniform sampler2D cameraPositions;
//uniform sampler2D normals;
//uniform sampler2D texCoords;

uniform float cubeTextureK;
uniform sampler2D cubeTexture;

struct PointLight {
	vec3 position;
	float range;
	
	vec3 color;
	float k;
};

#define MAX_LIGHTS 100

layout(std140) uniform Lights {
	vec3 ambientLight;
	float numberOfLights;
	PointLight lights[MAX_LIGHTS];
};

out vec4 fragColor;

const float fogRange = -1f / 200f;

vec3 calculateLight(vec3 color, float k, vec3 normal, vec3 lightDistance) {
	vec3 lightDirection = normalize(lightDistance);
	float cos = clamp(dot(normal, lightDirection), 0, 1);
	float dot = dot(lightDistance, lightDistance);
	float atten = 1.0 / (1.0 + k * dot);
	
	//vec3 viewDirection = normalize(-cameraSpacePosition);
	//vec3 halfAngle = normalize(lightDirection + viewDirection);
	//float angleNormalHalf = acos(dot(halfAngle, normal));
	//float exponent = angleNormalHalf / specularExponent;
	//exponent = -(exponent * exponent);
	//float guassianTerm = exp(exponent);
	
	//guassianTerm = cos != 0.0 ? gaussianTerm : 0.0;
	
	return color * atten * cos;// * gaussianTerm;
}

void main() {
	//vec2 tex = gl_FragCoord.xy / resolution;
	
	//vec3 cameraSpacePosition = texture(cameraPositions, tex).xyz;
	
	//if(cameraSpacePosition == vec3(0.0)) {
	//	fragColor = vec4(tex, 0, 1);
	//	return;
	//}
	//else {
	//	fragColor = vec4(cameraSpacePosition, 1);
	//	return;
	//}
	
	//vec3 normal = normalize(texture(normals, tex)).xyz;
	//vec2 texCoord = texture(texCoords, tex).st;
	
	vec3 totalLight = ambientLight;
	
	for(int a = 0; a < numberOfLights; a++) {
		PointLight light = lights[a];
		
		vec3 lightDistance = light.position - cameraSpacePosition;
		
		if(dot(lightDistance, lightDistance) <= light.range * light.range) {
			totalLight += calculateLight(light.color, light.k, norm, lightDistance) * 0.55f;
		}
	}
	
	float fog = clamp(1f - cameraSpacePosition.z * fogRange, 0.1f, 1.0f);
	
	
	
	#ifdef RGB_WAVE
	float redWave = 2 * sin(cameraSpacePosition.x + cameraSpacePosition.y) - 1;
	float greenWave = 2 * sin(cameraSpacePosition.x + cameraSpacePosition.z) - 1;
	float blueWave = 2 * sin(cameraSpacePosition.y + cameraSpacePosition.z) - 1;
	#else
	float sineWave = 2 * sin(cameraSpacePosition.x + cameraSpacePosition.y) - 1;
	#endif
	
	vec4 gamma = vec4(1.0 / 2.2);
	gamma.w = 1;
	fragColor = pow(vec4(texture(cubeTexture, texCoord).rgb * totalLight * fog *
		#ifdef RGB_WAVE
		vec3(redWave, greenWave, blueWave)
		#else
		vec3(sineWave, sineWave, sineWave)
		#endif
		, 1), gamma);
}