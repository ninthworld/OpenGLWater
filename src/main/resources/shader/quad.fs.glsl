#version 420

in vec2 vs_position;

out vec4 fs_color;

layout(binding=0) uniform sampler2D colorTexture;

void main(){
    vec3 color = texture(colorTexture, vs_position * 2.0).rgb;

    fs_color = vec4(color, 1.0);
}