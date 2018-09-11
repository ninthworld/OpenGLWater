#version 420

layout(location=0) in vec3 in_position;

out vec2 vs_texCoord;
out vec3 vs_position;
out vec4 vs_glPosition;

layout(std140, binding=0) uniform Camera {
    mat4 projMatrix;
    mat4 viewMatrix;
} camera;

uniform float time;

const float waterLevel = 8.0;

float getFractal(vec2 pos);

void main() {

    vs_texCoord = in_position.xz;

    vec3 position = in_position;
    position.x -= 0.5;
    position.z -= 0.5;
    position *= 128.0;
    position.y += waterLevel;

    position.y += getFractal(position.xz);

    vs_position = position;

    gl_Position = camera.projMatrix * camera.viewMatrix * vec4(position, 1.0);
    vs_glPosition = gl_Position;
}

float noise(vec2 x);

// Inspired by Kevin Roast
// <https://github.com/kevinroast/webglshaders/blob/master/waves3.html>
float getFractal(vec2 pos) {
    const int octaves = 8;

    float amp1 = 1.8;
    float amp2 = 0.8;
    float sum = 0.0;
    for(int i=0; i<octaves; ++i) {
        sum += noise(pos + time * 0.5) * amp1 * 0.25;
        if(i < 2) {
            sum += noise(pos.yx - time * 0.25) * amp2 * 0.1;
        }
        else {
            sum += abs(noise(pos.yx - time * 0.8) * amp2 * 0.05) * 2.0;
        }
        amp1 *= 0.4;
        amp2 *= 0.5;
        pos.x = pos.x * 1.75 + pos.y;
        pos.y = pos.y * 1.75 - pos.x;
    }
    return sum;
}

vec3 getNormal(vec2 pos) {
    const float o = 0.01;
    float h01 = getFractal(pos + vec2(-o, 0));
    float h21 = getFractal(pos + vec2(o, 0));
    float h10 = getFractal(pos + vec2(0, -o));
    float h12 = getFractal(pos + vec2(0, o));
    vec3 a = normalize(vec3(2.0 * o, 0.0, h01 - h21));
    vec3 b = normalize(vec3(0.0, 2.0 * o, h10 - h12));
    return normalize(cross(a, b));
}

//	<https://www.shadertoy.com/view/4dS3Wd>
//	By Morgan McGuire @morgan3d, http://graphicscodex.com
//
float hash(float n) { return fract(sin(n) * 1e4); }
float hash(vec2 p) { return fract(1e4 * sin(17.0 * p.x + p.y * 0.1) * (0.1 + abs(sin(p.y * 13.0 + p.x)))); }

float noise(float x) {
    float i = floor(x);
    float f = fract(x);
    float u = f * f * (3.0 - 2.0 * f);
    return mix(hash(i), hash(i + 1.0), u);
}

float noise(vec2 x) {
    vec2 i = floor(x);
    vec2 f = fract(x);

    // Four corners in 2D of a tile
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    // Simple 2D lerp using smoothstep envelope between the values.
    // return vec3(mix(mix(a, b, smoothstep(0.0, 1.0, f.x)),
    //			mix(c, d, smoothstep(0.0, 1.0, f.x)),
    //			smoothstep(0.0, 1.0, f.y)));

    // Same code, with the clamps in smoothstep and common subexpressions
    // optimized away.
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}