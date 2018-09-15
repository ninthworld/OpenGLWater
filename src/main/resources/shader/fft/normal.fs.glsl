#version 420

// Inspired by David Li ( www.david.li/waves )

precision highp float;

in vec2 vs_texCoord;

out vec4 fs_color;

uniform sampler2D displacementMap;

uniform float resolution;
uniform float size;

void main() {
    float texel = 1.0 / resolution;
    float texelSize = size / resolution;

    vec3 center = texture(displacementMap, vs_texCoord).rgb;
    vec3 right = vec3(texelSize, 0.0, 0.0) + texture(displacementMap, vs_texCoord + vec2(texel, 0.0)).rgb - center;
    vec3 left = vec3(-texelSize, 0.0, 0.0) + texture(displacementMap, vs_texCoord + vec2(-texel, 0.0)).rgb - center;
    vec3 top = vec3(0.0, 0.0, -texelSize) + texture(displacementMap, vs_texCoord + vec2(0.0, -texel)).rgb - center;
    vec3 bottom = vec3(0.0, 0.0, texelSize) + texture(displacementMap, vs_texCoord + vec2(0.0, texel)).rgb - center;

    vec3 topRight = cross(right, top);
    vec3 topLeft = cross(top, left);
    vec3 bottomLeft = cross(left, bottom);
    vec3 bottomRight = cross(bottom, right);

    fs_color = vec4(normalize(topRight + topLeft + bottomLeft + bottomRight), 1.0);
}