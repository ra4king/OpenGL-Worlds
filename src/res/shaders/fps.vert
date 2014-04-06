#version 330

struct Cube {
	vec3 position;
	float scale;
};

layout(location = 0) in Cube cubeInfo; //1 Cube per instance
layout(location = 2) in vec3 cubePos;
layout(location = 3) in vec3 norm;

out vec3 cameraSpacePosition;
out vec3 normal;
out vec3 color;

uniform mat4 projectionMatrix, modelViewMatrix;

void main() {
	vec4 vertexPosition = vec4(cubePos * cubeInfo.scale + cubeInfo.position, 1);
	
	cameraSpacePosition = vec3(modelViewMatrix * vertexPosition);
	gl_Position = projectionMatrix * vec4(cameraSpacePosition, 1);
	normal = mat3(modelViewMatrix) * norm;
	
	if(cubeInfo.scale - 0.5f < 0.0001f)
		color = vec3(0.0f, 0.0f, 1.0f);
	else
		color = vec3(0.0f, 0.0f, 0.0f);
}
