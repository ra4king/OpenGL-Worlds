#version 420

in vec3 position;
in vec3 normal;
in vec2 tex;

in vec3 cubePos; // XYZ: index
in float cubeType;

out vec3 originalCubePos;
out float originalCubeType;
out vec4 worldPosition;
out vec4 glPosition;

uniform mat4 projectionMatrix, viewMatrix;
uniform mat3 normalMatrix;

uniform float cubeSize;

layout(binding = 0) uniform atomic_uint fragmentCount;

const vec3 offset = vec3(0.5, 0.5, -0.5);

void main() {
	originalCubePos = cubePos;
	originalCubeType = cubeType;
	worldPosition = vec4(cubeSize * (vec3(cubePos.xy, -cubePos.z) + offset + position), 1);
	glPosition = projectionMatrix * viewMatrix * worldPosition;
	
    atomicCounterIncrement(fragmentCount);
}
