#version 420

in vec2 vs_texCoord;

out vec4 fs_color;

uniform sampler2D u_colorTexture;
uniform vec3 u_lightDirection;

void main() {
    vec3 color = texture(u_colorTexture, vs_texCoord * 128.0).rgb;
    fs_color = vec4(color, 1.0);
}