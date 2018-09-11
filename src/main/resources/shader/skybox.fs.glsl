#version 430

in vec3 vs_position;

out vec4 fs_diffuse;

layout(binding=0) uniform samplerCube skyboxTexture;

void main() {
    vec3 color = texture(skyboxTexture, vs_position).rgb;

    fs_diffuse = vec4(color, 1.0);
}