#version 420

in vec2 vs_texCoord;
in vec3 vs_normal;

out vec4 fs_color;

uniform sampler2D u_colorTexture;
uniform vec3 u_sunLightDirection;

void main() {
    vec3 color = texture(u_colorTexture, vs_texCoord).rgb;
    float cosTheta = max(0.0, dot(vs_normal, u_sunLightDirection));
    fs_color = vec4(color * cosTheta, 1.0);
}