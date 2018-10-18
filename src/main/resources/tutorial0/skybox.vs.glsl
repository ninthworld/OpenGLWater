#version 420

layout(location=0) in vec3 in_position;

out vec3 vs_position;

uniform mat4 u_projMatrix;
uniform mat4 u_modelViewMatrix;

void main() {
    vs_position = in_position;
    vs_position /= 1.0;

    mat4 staticView = u_modelViewMatrix;
    staticView[3] = vec4(0.0, 0.0, 0.0, 1.0);

    gl_Position = u_projMatrix * staticView * vec4(vs_position, 1.0);
}
