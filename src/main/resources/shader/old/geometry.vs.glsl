#version 420

layout(location=0) in vec3 in_position;

out vec3 vs_normal;
out vec2 vs_texCoord;

layout(std140, binding=1) uniform Water {
    vec4 value;
} water;

float getHeight(vec2 pos, float time) {
    return 0.0;//cos(50.0 * pos.x + time * 500.0) * 0.01;
}

vec3 getNormal(vec2 pos, float time) {
    const float o = 0.1;
    float h01 = getHeight(pos + vec2(-o, 0), time);
    float h21 = getHeight(pos + vec2(o, 0), time);
    float h10 = getHeight(pos + vec2(0, -o), time);
    float h12 = getHeight(pos + vec2(0, o), time);
    vec3 a = normalize(vec3(2.0 * o, 0.0, h21 - h01));
    vec3 b = normalize(vec3(0.0, 2.0 * 0, h12 - h10));
    return cross(a, b);
}

void main() {
    vs_texCoord = in_position.xz;
    vs_normal = getNormal(vs_texCoord, water.value.x);

    float height = getHeight(vs_texCoord, water.value.x);
    gl_Position = vec4((in_position - vec3(0.5, -height, 0.5)) * 64.0, 1.0);
}
