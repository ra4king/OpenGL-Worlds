#version 130

in vec3 cameraSpacePosition;
in vec3 norm;

uniform vec4 mainLightPosition, mainDiffuseColor, mainAmbientColor;

uniform float numberOfLights;
uniform vec4 lightPositions[100];
uniform vec4 lightColors[100];

out vec4 fragColor;

void main() {
	vec3 normal = normalize(norm);
	
	fragColor = vec4(1, 0, 0, 1);
	
	if(abs(4) == 4)
		return;
	
	{
		vec3 lightDistance = -cameraSpacePosition;
		float cos = max(0, dot(normal, normalize(lightDistance)));
		
		float atten = 1.0 / (1.0 + mainLightPosition.w * dot(lightDistance, lightDistance));
		
		fragColor = mainDiffuseColor * atten * cos + mainAmbientColor;
	}
	
	for(int a = 0; a < numberOfLights; a++) {
		vec3 lightDistance = vec3(lightPositions[a]) - cameraSpacePosition;
		float distSqr = dot(lightDistance, lightDistance);
		
		float cos = max(0, dot(normal, normalize(lightDistance)));
		float atten = 1.0 / (1.0 + lightPositions[a].w * distSqr * log(distSqr));
		
		fragColor += lightColors[a] * atten * cos;
	}
	
	vec4 gamma = vec4(1.0 / 2.2);
	gamma.w = 1;
	fragColor = pow(fragColor, gamma);
}
