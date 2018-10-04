#version 420

#define SPEED 0.5

in vec2 vs_texCoord;
in vec3 vs_normal;
in vec3 vs_position;

out vec4 fs_diffuse;

layout(std140, binding=1) uniform Light {
    vec4 direction;
} light;

uniform sampler2D colorTexture;

uniform float time;

uniform sampler2D noiseTextures[4];

void main() {
    vec3 normal = vs_normal;
    normal.y *= 0.2;
    normal = normalize(normal);

    vec3 lightDir = normalize(light.direction.xyz);
    float diff = max(dot(normal, lightDir), 0.0) * 1.2;

    vec3 color = texture(colorTexture, vs_position.xz * 0.5).rgb;

    const vec2[] offset = { vec2(0.002, 0.002), vec2(0.002, 0.0), vec2(0.0) };
    float dt = time * 0.05 * SPEED;
    vec3 caustics = vec3(0.0);
    if(vs_position.y < 8.0) {
        for(int i=0; i<3; ++i) {
            caustics[i] = texture(noiseTextures[0], vs_position.xz * 0.05 + vec2(dt, -dt) + offset[i]).r * 0.5;
            caustics[i] += texture(noiseTextures[1], vs_position.xz * 0.05 + vec2(-dt, dt) * 1.2 + offset[i]).r * 0.5;
            caustics[i] = pow(1.0 - abs(caustics[i] * 2.0 - 1.0), 32.0) * 0.1;
        }
    }

    fs_diffuse = vec4(caustics + color * diff, 1.0);
}