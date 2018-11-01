#version 420

in vec3 vs_position;
in vec3 vs_normal;

out vec4 fs_color;

uniform vec3 u_cameraPosition;
uniform vec3 u_sunLightDirection;

const vec3 waterColor = vec3(0.0, 0.6, 0.9);
const vec3 specularColor = vec3(0.5);
const float specularShininess = 128.0;

void main() {
    vec3 color = waterColor;

    float cosTheta = max(0.0, dot(vs_normal, u_sunLightDirection));
    vec3 finalColor = color * cosTheta;

    vec3 view = normalize(u_cameraPosition - vs_position);
    float specular = dot(view, reflect(-u_sunLightDirection, vs_normal));
    if(specular > 0.0) {
        specular = pow(specular, specularShininess);
        finalColor += vec3(specular) * specularColor;
    }

    fs_color = vec4(finalColor, 1.0);
}