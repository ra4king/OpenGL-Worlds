#version 330

in vec3 cameraSpacePosition;
in vec3 normal;

struct Light {
	vec4 position;
	vec4 color;
};

layout(std140) uniform Lights {
	vec4 mainLightPosition, mainDiffuseColor, mainAmbientColor;
	
	float numberOfLights;
	Light lightPositions[1000];
};

out vec4 fragColor;

void main() {
	vec3 normal = normalize(normal);
	
	{
		vec3 lightDistance = -cameraSpacePosition;
		float cos = max(0, dot(normal, normalize(lightDistance)));
		
		float atten = 1.0 / (1.0 + mainLightPosition.w * dot(lightDistance, lightDistance));
		
		fragColor = mainDiffuseColor * atten * cos + mainAmbientColor;
	}
	
	for(int a = 0; a < numberOfLights; a++) {
		Light light = lightPositions[a];
		
		vec3 lightDistance = vec3(light.position) - cameraSpacePosition;
		float distSqr = dot(lightDistance, lightDistance);
		
		float cos = max(0, dot(normal, normalize(lightDistance)));
		float atten = 1.0 / (1.0 + light.position.w * distSqr * log(distSqr));
		
		fragColor += light.color * atten * cos;
	}
	
	vec4 gamma = vec4(1.0 / 2.2);
	gamma.w = 1;
	fragColor = pow(fragColor, gamma);
}
