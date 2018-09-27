#version 420

#define OCTAVES 4

in vec2 vs_position;

out vec4 fs_color;

uniform float time;
uniform sampler2D colorTextures[OCTAVES];

void main(){

    float dt = time * 0.001;

    float val = 0.0;

    val += texture(colorTextures[0], vs_position * 2.0 + vec2(dt, 0.0)).r;
    val += texture(colorTextures[1], vs_position * 2.0 + vec2(0.0, dt)).r;
    val += texture(colorTextures[2], vs_position * 2.0 + vec2(0.0, -dt)).r;
    val += texture(colorTextures[3], vs_position * 2.0 + vec2(-dt, dt)).r;
    val /= OCTAVES;

    vec3 color = vec3(val);

    fs_color = vec4(color, 1.0);
}