#version 400

layout(location=0) in vec3 in_position;

layout(std140, binding=0) uniform Camera {
    mat4 projMatrix;
    mat4 viewMatrix;
} camera;

void main() {
    gl_Position = camera.projMatrix * camera.viewMatrix * vec4(in_position, 1.0);
}
