#version 420

in vec3 cameraSpacePosition;
in vec3 norm;
in vec2 texCoord;

#define MAX_LIGHTS 50

uniform float cubeTextureK;
uniform sampler2D cubeTexture;

struct PointLight {
	vec3 position;
	float range;
	
	vec3 color;
	float k;
};

layout(std140) uniform Lights {
	vec3 ambientLight;
	float numberOfLights;
	PointLight lights[MAX_LIGHTS];
};

out vec4 fragColor;

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
	vec3 normal = normalize(norm);
	
	vec4 totalLight = vec4(ambientLight, 1);
	
	for(int a = 0; a < numberOfLights; a++) {
		PointLight light = lights[a];
		
		vec3 lightDistance = light.position - cameraSpacePosition;
		
		if(dot(lightDistance, lightDistance) <= light.range * light.range)
			totalLight.rgb += calculateLight(light.color, light.k, normal, lightDistance);
	}
	
	fragColor = texture(cubeTexture, texCoord) * totalLight;
	
	vec4 gamma = vec4(1.0 / 2.2);
	gamma.w = 1;
	fragColor = pow(fragColor, gamma);
}
