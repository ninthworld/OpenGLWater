#version 420

layout(location=0) in vec3 in_position;

out vec4 vs_glPosition;
out vec3 vs_position;

uniform mat4 u_projMatrix;
uniform mat4 u_viewMatrix;
uniform mat4 u_modelMatrix;

void main() {
    vs_position = (u_modelMatrix * vec4(in_position, 1.0)).xyz;
    gl_Position = u_projMatrix * u_viewMatrix * vec4(vs_position, 1.0);
    vs_glPosition = gl_Position;
}
