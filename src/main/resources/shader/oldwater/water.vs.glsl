#version 430

layout(location=0) in vec3 in_position;

out vec2 vs_texCoord;
out vec3 vs_normal;

layout(std140, binding=0) uniform Camera {
    mat4 projMatrix;
    mat4 viewMatrix;
} camera;

layout(location=2) uniform float time;

void main() {
    const float waterLevel = 8.0;

    vs_texCoord = in_position.xz;

    vec3 position = in_position;
    position.x -= 0.5;
    position.z -= 0.5;
    position *= 128.0;
    position += waterLevel;

    vs_normal =  vec3(0.0, 1.0, 0.0);

    gl_Position = vec4(position, 1.0);
}
