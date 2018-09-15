#version 420

in vec2 vs_texCoord;
in vec3 vs_normal;
in vec3 vs_position;

out vec4 fs_diffuse;

layout(std140, binding=1) uniform Light {
    vec4 direction;
} light;

uniform sampler2D colorTexture;

void main() {
    vec3 normal = vs_normal;
    normal.y *= 0.2;
    normal = normalize(normal);

    vec3 lightDir = normalize(light.direction.xyz);
    float diff = max(dot(normal, lightDir), 0.0) * 1.2;

    vec3 color = texture(colorTexture, vs_position.xz * 0.5).rgb;

    fs_diffuse = vec4(color * diff, 1.0);
}