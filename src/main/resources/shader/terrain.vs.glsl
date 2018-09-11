#version 430

layout(location=0) in vec3 in_position;

out vec2 vs_texCoord;
out vec3 vs_normal;
out vec3 vs_position;

layout(std140, binding=0) uniform Camera {
    mat4 projMatrix;
    mat4 viewMatrix;
} camera;

layout(binding=0) uniform sampler2D heightMap;
layout(binding=1) uniform sampler2D normalMap;

layout(location=1) uniform vec4 clippingPlane;

void main() {
    vs_texCoord = in_position.xz;

    vs_normal = normalize(texture(normalMap, vs_texCoord).rgb * 2.0 - 1.0);

    vs_position = in_position;
    vs_position.y += texture(heightMap, vs_texCoord).r * 0.1;
    vs_position.x -= 0.5;
    vs_position.z -= 0.5;
    vs_position *= 128.0;

    gl_ClipDistance[0] = dot(clippingPlane, vec4(vs_position, 1.0));

    gl_Position = camera.projMatrix * camera.viewMatrix * vec4(vs_position, 1.0);
}
