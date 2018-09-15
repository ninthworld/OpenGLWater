#version 420

// Inspired by David Li ( www.david.li/waves )

precision highp float;

in vec2 vs_texCoord;

out vec4 fs_color;

const float PI = 3.14159265359;
const float G = 9.81;
const float KM = 370.0;

uniform sampler2D phases;

uniform float deltaTime;
uniform float resolution;
uniform float size;

float sqr(float x);
float omega(float k);

void main() {
    float dTime = 1.0 / 60.0;
    vec2 fragCoord = vs_texCoord * resolution;
    vec2 coords = fragCoord - 0.5;
    float n = (coords.x < resolution * 0.5 ? coords.x : coords.x - resolution);
    float m = (coords.y < resolution * 0.5 ? coords.y : coords.y - resolution);
    vec2 waveVec = (2.0 * PI * vec2(n, m)) / size;

    float phase = texture(phases, vs_texCoord).r;
    float deltaPhase = omega(length(waveVec)) * deltaTime;
    phase = mod(phase + deltaPhase, 2.0 * PI);

    fs_color = vec4(phase, 0.0, 0.0, 0.0);
}

float sqr(float x) {
    return x * x;
}

float omega(float k) {
    return sqrt(G * k * (1.0 + sqr(k / KM)));
}