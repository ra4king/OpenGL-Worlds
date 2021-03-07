#version 420

flat in vec3 position;
flat in float range;
flat in vec3 color;
flat in float k;

uniform float showBoxes = 1.0;

uniform sampler2D cubeTexture;

uniform sampler2D cameraPositions;
uniform sampler2D normals;
uniform sampler2D texCoords;
uniform sampler2D depth;

out vec4 fragColor;

const float fogRange = -1.0 / 200.0;
const float specularExponent = 0.01;

const float edgeSize = 1.0 - 0.02;

float calculateAttenuation(float k, vec3 normal, vec3 lightDistance) {
	vec3 lightDirection = normalize(lightDistance);
	float cos = clamp(dot(normal, lightDirection), 0, 1);
	float lightDot = dot(lightDistance, lightDistance);
	float atten = 1.0 / (1.0 + k * lightDot);

	return atten * cos;
}

void main() {
	vec2 tex = gl_FragCoord.xy;

	//	tex.x += cos(tex.y * 30.0) / 50;
	//	tex.y += sin(tex.x * 30.0) / 50;

	ivec2 texi = ivec2(tex);

	vec3 cameraSpacePosition = texelFetch(cameraPositions, texi, 0).xyz;

	if (cameraSpacePosition == vec3(0.0))
	discard;

	vec3 normal = normalize(texelFetch(normals, texi, 0).xyz);
	vec2 texCoord = texelFetch(texCoords, texi, 0).st;
	gl_FragDepth = texelFetch(depth, texi, 0).x;

	vec3 lightDistance = position - cameraSpacePosition;
	if (range > 0.0 && dot(lightDistance, lightDistance) > range * range)
	discard;

	float atten = calculateAttenuation(k, normal, lightDistance);

	vec3 totalLight = color * atten;
	
	vec3 viewDirection = normalize(-cameraSpacePosition);
	vec3 halfAngle = normalize(lightDistance + viewDirection);
	float angleNormalHalf = acos(dot(halfAngle, normal));
	float exponent = angleNormalHalf / 0.05;
	exponent = -(exponent * exponent);
	float gaussianTerm = exp(exponent);
	float cosAngIncidence = dot(normal, lightDistance);
	cosAngIncidence = clamp(cosAngIncidence, 0.0, 1.0);
	gaussianTerm = cosAngIncidence != 0.0 ? gaussianTerm : 0.0;

	totalLight += vec3(0.15) * gaussianTerm * atten;

	vec2 edgeC = abs((texCoord - vec2(0.5)) / vec2(0.5));
	vec2 edgeU = abs((texCoord - vec2(0.5, 0.495)) / vec2(0.5));
	vec2 edgeD = abs((texCoord - vec2(0.5, 0.505)) / vec2(0.5));
	vec2 edgeL = abs((texCoord - vec2(0.495, 0.5)) / vec2(0.5));
	vec2 edgeR = abs((texCoord - vec2(0.505, 0.5)) / vec2(0.5));
	vec3 edge = 0.2 * ((edgeC.x > edgeSize || edgeC.y > edgeSize || (edgeC.x * edgeC.y > edgeSize * edgeSize - 0.01 * edgeSize)) ? 1 : 0) * vec3(0.5);
	edge += 0.2 * ((edgeU.x > edgeSize || edgeU.y > edgeSize || (edgeU.x * edgeU.y > edgeSize * edgeSize - 0.01 * edgeSize)) ? 1 : 0) * vec3(0.5);
	edge += 0.2 * ((edgeD.x > edgeSize || edgeD.y > edgeSize || (edgeD.x * edgeD.y > edgeSize * edgeSize - 0.01 * edgeSize)) ? 1 : 0) * vec3(0.5);
	edge += 0.2 * ((edgeL.x > edgeSize || edgeL.y > edgeSize || (edgeL.x * edgeL.y > edgeSize * edgeSize - 0.01 * edgeSize)) ? 1 : 0) * vec3(0.5);
	edge += 0.2 * ((edgeR.x > edgeSize || edgeR.y > edgeSize || (edgeR.x * edgeR.y > edgeSize * edgeSize - 0.01 * edgeSize)) ? 1 : 0) * vec3(0.5);
	totalLight.rgb -= totalLight.rgb * edge;

	//float fog = clamp(1.0 - cameraSpacePosition.z * fogRange, 0.1, 1.0);

	vec4 gamma = vec4(1.0 / 2.2);
	gamma.w = 1;

	//	vec3 cubeColor = texture(cubeTexture, texCoord).rgb * totalLight;
	fragColor = pow(vec4(mix(color, totalLight, showBoxes), 1), gamma);
}
