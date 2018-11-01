#version 420

in vec3 vs_position;

out vec4 fs_color;

uniform samplerCube u_skyboxTexture;

void main() {
    vec3 color = texture(u_skyboxTexture, vs_position).rgb;
    fs_color = vec4(color, 1.0);
}