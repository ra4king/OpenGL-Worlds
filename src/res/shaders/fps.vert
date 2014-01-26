#version 330

const vec3 normals[] = vec3[6](vec3(0, 0, 1), vec3(0, 0, -1), vec3(0, 1, 0),
							   vec3(0, -1, 0), vec3(1, 0, 0), vec3(-1, 0, 0));

const vec3 unitCube[] = vec3[36](vec3(-0.5f, 0.5f, 0.5f),
								 vec3(0.5f, 0.5f, 0.5f),
								 vec3(0.5f, -0.5f, 0.5f),
								 vec3(0.5f, -0.5f, 0.5f),
								 vec3(-0.5f, -0.5f, 0.5f),
								 vec3(-0.5f, 0.5f, 0.5f),
								 
								 vec3(0.5f, 0.5f, -0.5f),
								 vec3(-0.5f, 0.5f, -0.5f),
								 vec3(-0.5f, -0.5f, -0.5f),
								 vec3(-0.5f, -0.5f, -0.5f),
								 vec3(0.5f, -0.5f, -0.5f),
								 vec3(0.5f, 0.5f, -0.5f),
								 
								 vec3(-0.5f, 0.5f, -0.5f),
								 vec3(0.5f, 0.5f, -0.5f),
								 vec3(0.5f, 0.5f, 0.5f),
								 vec3(0.5f, 0.5f, 0.5f),
								 vec3(-0.5f, 0.5f, 0.5f),
								 vec3(-0.5f, 0.5f, -0.5f),
								 
								 vec3(-0.5f, -0.5f, 0.5f),
								 vec3(0.5f, -0.5f, 0.5f),
								 vec3(0.5f, -0.5f, -0.5f),
								 vec3(0.5f, -0.5f, -0.5f),
								 vec3(-0.5f, -0.5f, -0.5f),
								 vec3(-0.5f, -0.5f, 0.5f),
								 
								 vec3(0.5f, 0.5f, 0.5f),
								 vec3(0.5f, 0.5f, -0.5f),
								 vec3(0.5f, -0.5f, -0.5f),
								 vec3(0.5f, -0.5f, -0.5f),
								 vec3(0.5f, -0.5f, 0.5f),
								 vec3(0.5f, 0.5f, 0.5f),
								 
								 vec3(-0.5f, 0.5f, -0.5f),
								 vec3(-0.5f, 0.5f, 0.5f),
								 vec3(-0.5f, -0.5f, 0.5f),
								 vec3(-0.5f, -0.5f, 0.5f),
								 vec3(-0.5f, -0.5f, -0.5f),
								 vec3(-0.5f, 0.5f, -0.5f));

out vec3 cameraSpacePosition;
out vec3 normal;

uniform mat4 projectionMatrix, modelViewMatrix;

#define CUBE_COUNT 16 * 16 * 16

struct Cube {
	vec3 position;
	float scale;
};

layout(std140) uniform CubeData {
	Cube cubes[CUBE_COUNT];
};

void main() {
	Cube cube = cubes[gl_VertexID / 36];
	
	vec4 vertexPosition = vec4(unitCube[gl_VertexID % 36] * cube.scale + cube.position, 1);
	
	cameraSpacePosition = vec3(modelViewMatrix * vertexPosition);
	gl_Position = projectionMatrix * vec4(cameraSpacePosition, 1);
	normal = mat3(modelViewMatrix) * normals[(gl_VertexID / 6) % 6];
}
