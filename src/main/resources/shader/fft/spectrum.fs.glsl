#version 420

// Inspired by David Li ( www.david.li/waves )

precision highp float;

in vec2 vs_texCoord;

out vec4 fs_color;

const float PI = 3.14159265359;
const float G = 9.81;
const float KM = 370.0;

uniform sampler2D phases;
uniform sampler2D initialSpectrum;

uniform float choppiness;
uniform float resolution;
uniform float size;

vec2 mulComplex(vec2 a, vec2 b);
vec2 mulByI(vec2 a);
float sqr(float x);
float omega(float k);

void main() {
    vec2 fragCoord = vs_texCoord * resolution;
    vec2 coords = fragCoord - 0.5;
    float n = (coords.x < resolution * 0.5 ? coords.x : coords.x - resolution);
    float m = (coords.y < resolution * 0.5 ? coords.y : coords.y - resolution);
    vec2 waveVec = (2.0 * PI * vec2(n, m)) / size;

    float phase = texture(phases, vs_texCoord).r;
    vec2 phaseVec = vec2(cos(phase), sin(phase));

    vec2 h0 = texture(initialSpectrum, vs_texCoord).rg;
    vec2 h0Star = texture(initialSpectrum, vec2(1.0 - vs_texCoord + 1.0 / resolution)).rg;
    h0Star.y *= -1.0;

    vec2 h = mulComplex(h0, phaseVec) + mulComplex(h0Star, vec2(phaseVec.x, -phaseVec.y));

    vec2 hX = -mulByI(h * (waveVec.x / length(waveVec))) * choppiness;
    vec2 hZ = -mulByI(h * (waveVec.y / length(waveVec))) * choppiness;

    if(waveVec.x == 0.0 && waveVec.y == 0.0) {
        h = vec2(0.0);
        hX = vec2(0.0);
        hZ = vec2(0.0);
    }

    fs_color = vec4(hX + mulByI(h), hZ);
}

vec2 mulComplex(vec2 a, vec2 b) {
    return vec2(a.x * b.x - a.y * b.y, a.y * b.x + a.x * b.y);
}

vec2 mulByI(vec2 a) {
    return vec2(-a.y, a.x);
}

float sqr(float x) {
    return x * x;
}

float omega(float k) {
    return sqrt(G * k * (1.0 + sqr(k / KM)));
}