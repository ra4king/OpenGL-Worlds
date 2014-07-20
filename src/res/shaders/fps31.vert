#version 330

in vec4 position;
in vec3 normal;
in vec2 tex;

out vec3 cameraSpacePosition;
out vec3 norm;
//varying vec2 texCoord;

uniform mat4 projectionMatrix, viewMatrix;
uniform mat3 normalMatrix;

void main() {
	vec4 cameraPos = viewMatrix * position;
	
	cameraSpacePosition = vec3(cameraPos);
	gl_Position = projectionMatrix * cameraPos;
	
	norm = normalMatrix * normal;
	
	//tex = texCoord;
}
