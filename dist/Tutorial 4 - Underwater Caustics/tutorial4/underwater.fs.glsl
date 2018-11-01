#version 420

in vec2 vs_texCoord;

out vec4 fs_color;

uniform float u_waterLevel;
uniform vec3 u_cameraPosition;
uniform sampler2D u_colorTexture;
uniform sampler2D u_depthTexture;

const float fogNear = 10.0;
const float fogFar = 60.0;
const vec3 overlayColor = vec3(0.5, 0.6, 0.85);
const vec3 fogColor = vec3(0.1, 0.3, 0.55);
//const vec3 overlayColor = vec3(0.7, 0.8, 0.95);
//const vec3 fogColor = vec3(0.3, 0.5, 0.75);

float normalDepth(float depthSample, float near, float far) {
    const float zNear = 0.1;
    const float zFar = 1000.0;
    depthSample = 2.0 * depthSample - 1.0;
    float zLinear = 2.0 * zNear * zFar / (zFar + zNear - depthSample * (zFar - zNear));
    return clamp((zLinear - near) / (far - near), 0.0, 1.0);
}

void main() {
    vec3 color = texture(u_colorTexture, vs_texCoord).rgb;

    if(u_cameraPosition.y < u_waterLevel) {
        float depth = texture(u_depthTexture, vs_texCoord).r;
        color = mix(color * overlayColor, fogColor, normalDepth(depth, 10.0, 60.0));
    }

    fs_color = vec4(color, 1.0);
}