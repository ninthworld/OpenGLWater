#version 420

in vec2 vs_texCoord;

out vec4 fs_color;

uniform sampler2D colorTexture;

void main() {
    fs_color = vec4(texture(colorTexture, vs_texCoord).rgb, 1.0);
}