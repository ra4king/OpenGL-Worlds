#version 440

in vec3 cameraSpacePosition;
in vec3 norm;
in vec2 texCoord;

layout(location = 0) out vec3 fragCameraPos;
layout(location = 1) out vec3 fragNormal;
layout(location = 2) out vec3 fragTexCoord;

void main() {
	fragCameraPos = cameraSpacePosition;
	fragNormal = norm;
	fragTexCoord = vec3(texCoord, 0.0);
}
