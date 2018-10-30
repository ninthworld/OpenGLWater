#version 420

in vec4 vs_glPosition;
in vec3 vs_position;

out vec4 fs_color;

uniform vec3 u_cameraPosition;
uniform vec3 u_sunLightDirection;
uniform float u_time;
uniform sampler2D u_refractTexture;
uniform sampler2D u_reflectTexture;
uniform sampler2D u_noiseTexture[4];

const vec3 waterColor = vec3(0.0, 0.1, 0.3);
const vec3 specularColor = vec3(0.5);
const float specularShininess = 128.0;

float noise(vec2 pos, int oct) {
    return texture(u_noiseTexture[oct], pos * pow(0.5, oct)).r;
}

// Modified from FractalNoise(in vec2 xy) at http://www.kevs3d.co.uk/dev/shaders/waves2.html
float getHeight(vec2 pos) {
    pos /= 16.0;
    float dt = u_time * 0.01;
    float m = 1.5;
    float w = 0.5;
    float f = 0.0;
    for (int i = 0; i < 4; i++){
        f += noise(pos + dt * 0.0511, i) * m * 0.15;
        f += noise(pos.yx - dt * 0.0333, i) * w * 0.25;
        w *= 0.5;
        m *= 0.25;
        pos *= mat2(1.6, 1.2, -1.2, 1.6); // Octave transform matrix from Alexander Alekseev aka TDM
    }
    return f;
}

float getDist(vec3 pos) {
   return dot(pos - vec3(0.0, -getHeight(pos.xz), 0.0), vec3(0.0, 1.0, 0.0));
}

vec3 getNormal(vec3 pos) {
    const vec2 delta = vec2(0.05, 0.0);
    vec3 n;
    n.x = getDist( pos + delta.xyy ) - getDist( pos - delta.xyy );
    n.y = getDist( pos + delta.yxy ) - getDist( pos - delta.yxy );
    n.z = getDist( pos + delta.yyx ) - getDist( pos - delta.yyx );

    return normalize(-n);
}

void main() {
    vec2 screenSpace = (vs_glPosition.xy / vs_glPosition.w) / 2.0 + 0.5;
    vec3 view = normalize(u_cameraPosition - vs_position);
    vec3 normal = getNormal(vs_position);

    vec2 distort = vec2(0.05, 0.0) * normal.xz + screenSpace;
    distort.x = clamp(distort.x, 0.001, 0.999);
    distort.y = clamp(distort.y, 0.001, 0.999);

    vec3 color = waterColor;
    vec3 refractColor = texture(u_refractTexture, distort).rgb;
    vec3 reflectColor = texture(u_reflectTexture, distort * vec2(1.0, -1.0)).rgb;

    float fresnel = dot(view, normal);
    color += mix(reflectColor, refractColor, fresnel);

    float cosTheta = max(0.0, dot(normal, -u_sunLightDirection));
    vec3 finalColor = color * cosTheta;

    float specular = dot(view, reflect(-u_sunLightDirection, normal));
    if(specular > 0.0) {
        specular = pow(specular, specularShininess);
        finalColor += vec3(specular) * specularColor;
    }

    fs_color = vec4(finalColor, 1.0);
}