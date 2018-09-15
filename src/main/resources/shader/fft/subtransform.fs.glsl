#version 420

// Inspired by David Li ( www.david.li/waves )

precision highp float;

in vec2 vs_texCoord;

out vec4 fs_color;

const float PI = 3.14159265359;

uniform sampler2D inputTexture;

uniform float subtransformSize;
uniform float direction;
uniform float resolution;

vec2 mulComplex(vec2 a, vec2 b);

void main() {
    vec2 fragCoord = vs_texCoord * resolution;

    float transformSize = resolution;

    float index = 0.0;
    if(direction == 0.0) {
        index = vs_texCoord.x * transformSize - 0.5;
    }
    else {
        index = vs_texCoord.y * transformSize - 0.5;
    }

    float evenIndex = floor(index / subtransformSize) * (subtransformSize * 0.5) + mod(index, subtransformSize * 0.5);

    vec4 even = vec4(0.0);
    vec4 odd = vec4(0.0);
    if(direction == 0.0) {
        even = texture(inputTexture, vec2(evenIndex + 0.5, fragCoord.y) / transformSize);
        odd = texture(inputTexture, vec2(evenIndex + transformSize * 0.5 + 0.5, fragCoord.y) / transformSize);
    }
    else {
        even = texture(inputTexture, vec2(fragCoord.x, evenIndex + 0.5) / transformSize);
        odd = texture(inputTexture, vec2(fragCoord.x, evenIndex + transformSize * 0.5 + 0.5) / transformSize);
    }

    float twiddleArg = -2.0 * PI * (index / subtransformSize);
    vec2 twiddle = vec2(cos(twiddleArg), sin(twiddleArg));

    vec2 outputA = even.xy + mulComplex(twiddle, odd.xy);
    vec2 outputB = even.zw + mulComplex(twiddle, odd.zw);

    fs_color = vec4(outputA, outputB);
}

vec2 mulComplex(vec2 a, vec2 b) {
    return vec2(a.x * b.x - a.y * b.y, a.y * b.x + a.x * b.y);
}