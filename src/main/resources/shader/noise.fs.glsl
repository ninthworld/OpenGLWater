#version 420

#define OCTAVES 4

in vec2 vs_position;

out vec4 fs_color;

uniform float time;
uniform sampler2D colorTextures[OCTAVES];

float getHeight(vec2 pos) {
    float dt = time * 0.002; //time * 1.5;
    float amp1 = 1.8;
    float amp2 = 0.8;
    float sum = 0.0;
    for(int i=0; i<OCTAVES; ++i) {
        sum += texture(colorTextures[i], pos + dt * 0.5).r * amp1 * 0.25;
        if(i < 2) {
            sum += texture(colorTextures[i], pos.yx - dt * 0.25).r * amp2 * 0.1;
        }
        else {
            sum += abs(texture(colorTextures[i], pos.yx - dt * 0.8).r * amp2 * 0.05) * 2.0;
        }
        amp1 *= 0.4;
        amp2 *= 0.5;
        pos.x = pos.x * 1.5 + pos.y;
        pos.y = pos.y * 1.5 - pos.x;
    }
    return sum;
}

void main(){

    vec2 pos = vs_position * 32.0;
    float dt = time * 0.05;
    float val = 0.0;
    val += texture(colorTextures[0], vec2(0.5, 1.0) * pos * 0.05 + vec2(dt, 0.0)).r * 0.8;
    val += texture(colorTextures[1], vec2(1.0, 0.5) * pos * 0.1 + vec2(0.0, dt)).r * 0.1;
    val += texture(colorTextures[2], pos * 0.2 + vec2(dt, dt)).r * 0.05;
    val += texture(colorTextures[3], pos * 0.4 + vec2(-dt, -dt)).r * 0.05;

    vec3 color = vec3(val);

    fs_color = vec4(color, 1.0);
}