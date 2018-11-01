#version 420

in vec3 vs_position;
in vec2 vs_texCoord;
in vec3 vs_normal;

out vec4 fs_color;

uniform float u_time;
uniform float u_waterLevel;
uniform sampler2D u_noiseTexture[4];
uniform sampler2D u_colorTexture;
uniform vec3 u_sunLightDirection;

float noise(vec2 pos, int oct) {
    return texture(u_noiseTexture[oct], pos * pow(0.5, oct)).r;
}

// Modified from FractalNoise(in vec2 xy) at http://www.kevs3d.co.uk/dev/shaders/waves2.html
float getHeight(vec2 pos) {
    pos /= 16.0;
    float dt = u_time * 0.01;
    float m = 1.5;
    float w = 0.5;
    float f = 0.0;
    for (int i = 0; i < 4; i++){
        f += noise(pos + dt * 0.0511, i) * m * 0.15;
        f += noise(pos.yx - dt * 0.0333, i) * w * 0.25;
        w *= 0.5;
        m *= 0.25;
        pos *= mat2(1.6, 1.2, -1.2, 1.6); // Octave transform matrix from Alexander Alekseev aka TDM
    }
    return f;
}

void main() {
    vec3 color = texture(u_colorTexture, vs_texCoord).rgb;
    float cosTheta = max(0.0, dot(vs_normal, u_sunLightDirection));
    color *= cosTheta;

    if(vs_position.y < u_waterLevel) {
        float height = clamp(getHeight(vs_position.xz) * 2.0, 0.0, 1.0);
        height = pow(1.0 - abs(height * 2.0 - 1.0), 24.0);
        color += vec3(height * 0.5);
    }

    fs_color = vec4(color, 1.0);
}