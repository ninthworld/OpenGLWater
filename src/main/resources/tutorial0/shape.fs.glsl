#version 420

in vec3 vs_normal;
in vec2 vs_texCoord;

out vec4 fs_color;

uniform vec3 u_lightDirection;

void main() {
    fs_color = vec4(vs_normal * 0.5 + 0.5, 1.0);
}