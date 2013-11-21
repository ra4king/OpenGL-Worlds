#version 120

varying vec3 cameraSpacePosition;
varying vec3 norm;

uniform vec4 mainLightPosition, mainDiffuseColor, mainAmbientColor;

uniform float numberOfLights;
uniform vec4 lightPositions[50];
uniform vec4 lightColors[50];

void main() {
	vec3 normal = normalize(norm);
	
	vec4 fragColor;
	
	{
		vec3 lightDistance = -cameraSpacePosition;
		float cos = max(0, dot(normal, normalize(lightDistance)));
		
		float atten = 1.0 / (1.0 + mainLightPosition.w * dot(lightDistance, lightDistance));
		
		fragColor = mainDiffuseColor * atten * cos + mainAmbientColor;
	}
	
	for(int a = 0; a < numberOfLights; a++) {
		vec3 lightDistance = vec3(lightPositions[a]) - cameraSpacePosition;
		float cos = max(0, dot(normal, normalize(lightDistance)));
		
		float atten = 1.0 / (1.0 + lightPositions[a].w * dot(lightDistance, lightDistance));
		
		fragColor += lightColors[a] * atten * cos;
	}
	
	vec4 gamma = vec4(1.0 / 2.2);
	gamma.w = 1;
	gl_FragColor = pow(fragColor, gamma);
}
