#version 420

layout(location=0) in vec3 in_position;

out vec2 vs_texCoord;
out vec3 vs_position;
out vec4 vs_glPosition;

layout(std140, binding=0) uniform Camera {
    mat4 projMatrix;
    mat4 viewMatrix;
} camera;

const float waterLevel = 8.0;

uniform float time;

uniform sampler2D colorTextures[4];

float getHeight(vec2 pos) {
    float dt = time * 0.05;
    float val = 0.0;
    val += texture(colorTextures[0], vec2(0.5, 1.0) * pos * 0.05 + vec2(dt, 0.0)).r * 0.8;
    val += texture(colorTextures[1], vec2(1.0, 0.5) * pos * 0.1 + vec2(0.0, dt)).r * 0.1;
    val += texture(colorTextures[2], pos * 0.2 + vec2(dt, dt)).r * 0.05;
    val += texture(colorTextures[3], pos * 0.4 + vec2(-dt, -dt)).r * 0.05;
    return val;
}

void main() {

    vs_texCoord = in_position.xz;

    vec3 position = in_position;
    position.x -= 0.5;
    position.z -= 0.5;
    position *= 128.0;
    position.y += waterLevel;

    position.y += getHeight(position.xz) * 0.5;

    vs_position = position;

    gl_Position = camera.projMatrix * camera.viewMatrix * vec4(position, 1.0);
    vs_glPosition = gl_Position;
}