#version 420

layout(location=0) in vec2 in_position;

out vec2 vs_position;

void main() {
    vs_position = (in_position + 1.0) / 2.0;
    gl_Position = vec4(in_position, 0.0, 1.0);
}
