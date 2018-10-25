#version 420

in vec4 vs_glPosition;
in vec3 vs_position;
in vec3 vs_normal;

out vec4 fs_color;

uniform vec3 u_cameraPosition;
uniform vec3 u_sunLightDirection;
uniform sampler2D u_refractTexture;
uniform sampler2D u_reflectTexture;

const vec3 waterColor = vec3(0.0);//, 0.6, 0.9);
const vec3 specularColor = vec3(0.5);
const float specularShininess = 128.0;

void main() {
    vec2 screenSpace = (vs_glPosition.xy / vs_glPosition.w) / 2.0 + 0.5;
    vec3 view = normalize(u_cameraPosition - vs_position);

    vec3 refractColor = texture(u_refractTexture, screenSpace).rgb;
    vec3 reflectColor = texture(u_reflectTexture, screenSpace * vec2(1.0, -1.0)).rgb;

    vec3 color = waterColor;

    float fresnel = pow(dot(view, -vs_normal), 8.0);
    color += mix(refractColor, reflectColor, fresnel);

    float cosTheta = max(0.0, dot(vs_normal, u_sunLightDirection));
    vec3 finalColor = color * cosTheta;

    float specular = dot(view, reflect(-u_sunLightDirection, vs_normal));
    if(specular > 0.0) {
        specular = pow(specular, specularShininess);
        finalColor += vec3(specular) * specularColor;
    }

    fs_color = vec4(finalColor, 1.0);
}