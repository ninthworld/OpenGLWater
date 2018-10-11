#version 420

layout(location=0) in vec3 in_position;
layout(location=1) in vec3 in_normal;
layout(location=2) in vec2 in_texCoord;

out vec3 vs_normal;
out vec2 vs_texCoord;

uniform mat4 u_projMatrix;
uniform mat4 u_modelViewMatrix;

void main() {
    vs_normal = in_normal;
    vs_texCoord = in_texCoord;

    gl_Position = u_projMatrix * u_modelViewMatrix * vec4(in_position, 1.0);
}
