#version 420

#define SPEED 2.0

layout(location=0) in vec3 in_position;

out vec2 vs_texCoord;
out vec3 vs_position;
out vec4 vs_glPosition;

layout(std140, binding=0) uniform Camera {
    mat4 projMatrix;
    mat4 viewMatrix;
} camera;

const float waterLevel = 8.0;

uniform float time;

uniform sampler2D noiseTextures[4];

//float getHeight(vec2 pos) {
//    float dt = time * 0.05 * SPEED;
//    float val = 0.0;
//    val += texture(noiseTextures[0], vec2(0.5, 1.0) * pos * 0.05 + vec2(dt, 0.0)).r * 0.8;
//    val += texture(noiseTextures[1], vec2(1.0, 0.5) * pos * 0.1 + vec2(0.0, dt)).r * 0.14;
//    val += (1.0 - abs(texture(noiseTextures[2], pos * 0.1 + vec2(dt, dt)).r * 2.0 - 1.0)) * 0.04;
//    val += (1.0 - abs(texture(noiseTextures[3], pos * 0.1 + vec2(-dt, -dt)).r * 2.0 - 1.0)) * 0.02;
//    return val;
//}

// Octave transform matrix from Alexander Alekseev aka TDM
mat2 octave_m = mat2(1.6,1.2,-1.2,1.6);

float noise(vec2 pos, int oct) {
    return texture(noiseTextures[oct], pos * pow(0.5, oct)).r;
}

// Modified from FractalNoise(in vec2 xy) at http://www.kevs3d.co.uk/dev/shaders/waves2.html
float getHeight(vec2 pos) {
    pos /= 16.0;
    float dt = time * SPEED;
    float m = 1.5;
    float w = 0.5;
    float f = 0.0;
    for (int i = 0; i < 4; i++){
        f += noise(pos + dt*0.0511, i) * m * 0.15;
        f += noise(pos.yx - dt*0.0333, i) * w * 0.25;
        w *= 0.5;
        m *= 0.25;
        pos *= octave_m;
    }
    return f;
}

void main() {

    vs_texCoord = in_position.xz;

    vec3 position = in_position;
    position.x -= 0.5;
    position.z -= 0.5;
    position *= 128.0;
    position.y += waterLevel;

    position.y += getHeight(position.xz);

    vs_position = position;

    gl_Position = camera.projMatrix * camera.viewMatrix * vec4(position, 1.0);
    vs_glPosition = gl_Position;
}