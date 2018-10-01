#version 420

in vec2 vs_position;

out vec4 fs_color;

layout(std140, binding=0) uniform InvCamera {
    mat4 projMatrix;
    mat4 viewMatrix;
} invCamera;

const float waterLevel = 8.0;

uniform vec3 cameraPos;
uniform float time;

uniform sampler2D colorTexture;
uniform sampler2D depthTexture;

float normalDepth(float depthSample, float near, float far) {
    float zNear = 0.1;
    float zFar = 1000.0;

    depthSample = 2.0 * depthSample - 1.0;
    float zLinear = 2.0 * zNear * zFar / (zFar + zNear - depthSample * (zFar - zNear));
    return clamp((zLinear - near) / (far - near), 0.0, 1.0);
}

vec3 getPositionFromDepth(float depth, vec2 texCoord) {
	vec4 ssPos = invCamera.projMatrix * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
	vec4 wsPos = invCamera.viewMatrix * ssPos;
	return wsPos.xyz / wsPos.w;
}

void main(){

    vec3 color = texture(colorTexture, vs_position).rgb;

    if(cameraPos.y < waterLevel) {
        float depth = texture(depthTexture, vs_position).r;
        vec3 position = getPositionFromDepth(depth, vs_position);

        vec3 fogColor = vec3(0.2, 0.6, 0.8);
        vec3 overlayMul = vec3(0.4, 0.5, 0.6);

        color = mix(color * overlayMul, fogColor, normalDepth(depth, 10.0, 60.0));
    }

    fs_color = vec4(color, 1.0);
}