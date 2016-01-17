#version 450

layout(points) in;
layout(triangle_strip, max_vertices=24) out;

out vec3 cameraSpacePosition;
out vec3 norm;
out vec2 texCoord;

uniform mat4 projectionMatrix, viewMatrix;
uniform mat3 normalMatrix;

uniform float cubeSize;

uniform vec4 clipPlane;

const vec3 normals[] = {
	vec3(0, 0, 1), // front
	vec3(0, 0, -1), // back
	vec3(0, 1, 0), // top
	vec3(0, -1, 0), // bottom
	vec3(1, 0, 0), // right
	vec3(-1, 0, 0) // left
};

const vec2 texCoords[] = {
	vec2(0, 1),
	vec2(1, 1),
	vec2(0, 0),
	vec2(1, 0),
};

const vec3 unitCube[] = {
	// front
	vec3(0.0f, 1.0f, 0.0f),
	vec3(1.0f, 1.0f, 0.0f),
	vec3(0.0f, 0.0f, 0.0f),
	vec3(1.0f, 0.0f, 0.0f),
	
	// back
	vec3(1.0f, 1.0f, -1.0f),
	vec3(0.0f, 1.0f, -1.0f),
	vec3(1.0f, 0.0f, -1.0f),
	vec3(0.0f, 0.0f, -1.0f),
	
	// top
	vec3(0.0f, 1.0f, -1.0f),
	vec3(1.0f, 1.0f, -1.0f),
	vec3(0.0f, 1.0f, 0.0f),
	vec3(1.0f, 1.0f, 0.0f),
	
	// bottom
	vec3(0.0f, 0.0f, 0.0f),
	vec3(1.0f, 0.0f, 0.0f),
	vec3(0.0f, 0.0f, -1.0f),
	vec3(1.0f, 0.0f, -1.0f),
	
	// right
	vec3(1.0f, 1.0f, 0.0f),
	vec3(1.0f, 1.0f, -1.0f),
	vec3(1.0f, 0.0f, 0.0f),
	vec3(1.0f, 0.0f, -1.0f),
	
	// left
	vec3(0.0f, 1.0f, -1.0f),
	vec3(0.0f, 1.0f, 0.0f),
	vec3(0.0f, 0.0f, -1.0f),
	vec3(0.0f, 0.0f, 0.0f),
};

void createVertex(in vec3 cubePos, in vec3 position, in vec3 normal, in vec2 tex) {
	vec4 worldPosition = vec4(cubeSize * (vec3(cubePos.xy, -cubePos.z) + position), 1);
	
	vec4 cameraPos = viewMatrix * worldPosition;
	
	cameraSpacePosition = vec3(cameraPos);
	gl_Position = projectionMatrix * cameraPos;
	
	norm = normalMatrix * normal;
	
	texCoord = tex;
	
	gl_ClipDistance[0] = dot(clipPlane.xyz, worldPosition.xyz) + clipPlane.w;
	
	EmitVertex();
}

void main() {
	for(int a = 0; a < unitCube.length(); a += 4) {
		for(int b = 0; b < 4; b++) {
			vec3 position = unitCube[a + b];
			vec3 normal = normals[a / 4];
			
			int c = a + b;
			c %= 4;
			vec2 tex = texCoords[c];
			
			createVertex(gl_in[0].gl_Position.xyz, position, normal, tex);
		}
		
		EndPrimitive();
	}
}
