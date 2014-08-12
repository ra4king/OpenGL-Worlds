#version 420

in vec3 position;
in vec3 normal;
in vec2 tex;

in vec3 cubePos; // XYZ: index
in uint cubeType;

out vec3 cameraSpacePosition;
out vec3 norm;
out vec2 texCoord;

uniform mat4 projectionMatrix, viewMatrix;
uniform mat3 normalMatrix;

uniform float cubeSize;

const vec3 offset = vec3(0.5, 0.5, -0.5);

void main() {
	vec4 worldPosition = vec4(cubeSize * (vec3(cubePos.xy, -cubePos.z) + offset + position), 1);
	
	vec4 cameraPos = viewMatrix * worldPosition;
	
	cameraSpacePosition = vec3(cameraPos);
	gl_Position = projectionMatrix * cameraPos;
	
	norm = normalMatrix * normal;
	
	texCoord = tex;
}
