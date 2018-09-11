#version 430

layout(location=0) in vec3 in_position;

out vec3 vs_position;

layout(std140, binding=0) uniform Camera {
    mat4 projMatrix;
    mat4 viewMatrix;
} camera;

void main() {
    vs_position = in_position;
    vs_position *= 10.0;

    mat4 staticView = camera.viewMatrix;
    staticView[3] = vec4(0.0, 0.0, 0.0, 1.0);

    gl_Position = camera.projMatrix * staticView * vec4(vs_position, 1.0);
}
