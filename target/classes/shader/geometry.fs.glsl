#version 420

in vec3 vs_position;

out vec4 fs_color;

void main(){
    vec3 color = (vs_position + 1.0) / 2.0;

    fs_color = vec4(color, 1.0);
}