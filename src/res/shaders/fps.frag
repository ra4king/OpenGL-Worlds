#version 330

in vec3 cameraSpacePosition;
in vec3 normal;

struct Light {
	vec3 color;
	float k;
};

struct PointLight {
	vec3 position;
	float range;
	Light light;
};

#define MAX_NUM_LIGHTS 500

layout(std140) uniform Lights {
	Light mainDiffuseLight;
	vec3 mainAmbientColor;
	
	float numberOfLights;
	
	PointLight lightPositions[MAX_NUM_LIGHTS];
};

out vec4 fragColor;

void main() {
	vec3 normal = normalize(normal);
	
	fragColor = vec4(mainAmbientColor, 1);
	
	{
		vec3 lightDistance = -cameraSpacePosition;
		
		float cos = max(0, dot(normal, normalize(lightDistance)));
		float atten = 1.0 / (1.0 + mainDiffuseLight.k * dot(lightDistance, lightDistance));
		
		fragColor.xyz += vec3(1, 0.5, 0.5) * mainDiffuseLight.color * atten * cos;
	}
	
	for(int a = int(numberOfLights) - 1; a >= 0; a--) {
		PointLight pointLight = lightPositions[a];
		
		vec3 lightDistance = pointLight.position - cameraSpacePosition;
		
		float lightDistanceSqr = dot(lightDistance, lightDistance);
		if(lightDistanceSqr > pointLight.range * pointLight.range)
			continue;
		
		float cos = max(0, dot(normal, normalize(lightDistance)));
		float atten = 1.0 / (1.0 + pointLight.light.k * lightDistanceSqr);
		
		fragColor.xyz += (pointLight.light.color * atten * cos) * 0.2;
	}
	
	vec4 gamma = vec4(1.0 / 2.2);
	gamma.w = 1;
	fragColor = pow(fragColor, gamma);
}
