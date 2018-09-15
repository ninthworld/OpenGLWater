#version 420

// Inspired by David Li ( www.david.li/waves )

precision highp float;

in vec2 vs_texCoord;

out vec4 fs_color;

const float PI = 3.14159265359;
const float G = 9.81;
const float KM = 370.0;
const float CM = 0.23;

uniform vec2 wind;
uniform float resolution;
uniform float size;

float sqr(float x);
float omega(float k);
float tanh(float x);

void main() {
    vec2 fragCoord = vs_texCoord * resolution;
    vec2 coords = fragCoord - 0.5;
    float n = (coords.x < resolution * 0.5 ? coords.x : coords.x - resolution);
    float m = (coords.y < resolution * 0.5 ? coords.y : coords.y - resolution);
    vec2 waveVec = (2.0 * PI * vec2(n, m)) / size;
    float k = length(waveVec);

    float U10 = length(wind);

    float Omega = 0.84;
    float kp = G * sqr(Omega / U10);

    float c = omega(k) / k;
    float cp = omega(kp) / kp;

    float Lpm = exp(-1.25 * sqr(kp / k));
    float gamma = 1.7;
    float sigma = 0.08 * (1.0 + 4.0 * pow(Omega, -3.0));
    float Gamma = exp(-sqr(sqrt(k / kp) - 1.0) / 2.0 * sqr(sigma));
    float Jp = pow(gamma, Gamma);
    float Fp = Lpm * Jp * exp(-Omega / sqrt(10.0) * (sqrt(k / kp) - 1.0));
    float alphap = 0.006 * sqrt(Omega);
    float B1 = 0.5 * alphap * cp / c * Fp;

    float z0 = 0.000037 * sqr(U10) / G * pow(U10 / cp, 0.9);
    float uStar = 0.41 * U10 / log(10.0 / z0);
    float alpham = 0.01 * ((uStar < CM ? 1.0 + log(uStar / CM) : 1.0 + 3.0 * log(uStar / CM)));
    float Fm = exp(-0.25 * sqr(k / KM - 1.0));
    float Bh = 0.5 * alpham * CM / c * Fm * Lpm;

    float a0 = log(2.0) / 4.0;
    float am = 0.13 * uStar / CM;
    float Delta = tanh(a0 + 4.0 * pow(c / cp, 2.5) + am * pow(CM / c, 2.5));

    float cosPhi = dot(normalize(wind), normalize(waveVec));

    float S = (1.0 / (2.0 * PI)) * pow(k, -4.0) * (B1 + Bh) * (1.0 + Delta * (2.0 * cosPhi * cosPhi - 1.0));

    float dk = 2.0 * PI / size;
    float h = sqrt(S / 2.0) * dk;

    if(waveVec.x == 0.0 && waveVec.y == 0.0) {
        h = 0.0;
    }

    fs_color = vec4(h, 0.0, 0.0, 0.0);
}

float sqr(float x) {
    return x * x;
}

float omega(float k) {
    return sqrt(G * k * (1.0 + sqr(k / KM)));
}

float tanh(float x) {
    return (1.0 - exp(-2.0 * x)) / (1.0 + exp(-2.0 * x));
}