#version 120

varying vec3 cameraSpacePosition;
varying vec3 norm;
//varying vec2 texCoord;

#define MAX_LIGHTS 50

uniform float cubeTextureK;

uniform vec3 ambientLight;
uniform int numberOfLights;
uniform vec4 lights[MAX_LIGHTS * 2]; // 2 vec4's per light. 1st: vec3 position, float range, 2nd: vec3 color, float k

vec3 calculateLight(vec3 color, float k, vec3 normal, vec3 lightDistance) {
	vec3 lightDirection = normalize(lightDistance);
	float cos = clamp(dot(normal, lightDirection), 0, 1);
	float atten = 1.0 / (1.0 + k * dot(lightDistance, lightDistance));
	
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
	vec3 normal = normalize(norm);
	
	vec4 fragColor = vec4(ambientLight, 1);
	
	for(int a = 0; a < numberOfLights; a++) {
		vec4 pos = lights[a * 2];
		vec4 col = lights[a * 2 + 1];
		
		vec3 position = pos.xyz;
		float range = pos.w;
		
		vec3 color = col.rgb;
		float k = col.a;
		
		vec3 lightDistance = position - cameraSpacePosition;
		
		if(dot(lightDistance, lightDistance) <= range * range)
			fragColor.rgb += calculateLight(color, k, normal, lightDistance);
	}
	
	vec4 gamma = vec4(1.0 / 2.2);
	gamma.w = 1;
	gl_FragColor = pow(fragColor, gamma);
}
