#version 420

layout(location=0) in vec2 in_position;

out vec2 vs_texCoord;

void main() {
    vs_texCoord = in_position * 0.5 + 0.5;
    gl_Position = vec4(in_position, 0.0, 1.0);
}