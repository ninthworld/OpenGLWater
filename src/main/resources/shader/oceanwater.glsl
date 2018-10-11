#version 420

#define SPEED 2.0

in vec2 vs_texCoord;
in vec3 vs_position;
in vec4 vs_glPosition;

out vec4 fs_diffuse;

layout(std140, binding=1) uniform Light {
    vec4 direction;
} light;

uniform sampler2D refractTexture;
uniform sampler2D reflectTexture;
uniform sampler2D heightMap;

uniform vec3 cameraPos;
uniform float time;

uniform sampler2D noiseTextures[4];

// Octave transform matrix from Alexander Alekseev aka TDM
mat2 octave_m = mat2(1.6,1.2,-1.2,1.6);

float noise(vec2 pos, int oct) {
    return texture(noiseTextures[oct], pos * pow(0.5, oct)).r;
}

// Modified from FractalNoise(in vec2 xy) at http://www.kevs3d.co.uk/dev/shaders/waves2.html
float getHeight(vec2 pos) {
    pos /= 16.0;
    float dt = time * SPEED;
    float m = 1.5;
    float w = 0.5;
    float f = 0.0;
    for (int i = 0; i < 4; i++){
        f += noise(pos + dt*0.0511, i) * m * 0.15;
        f += noise(pos.yx - dt*0.0333, i) * w * 0.25;
        w *= 0.5;
        m *= 0.25;
        pos *= octave_m;
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

const vec3 skyColor = vec3(0.0, 0.8, 1.0);
const vec3 diffuse = vec3(0.1, 0.2, 0.5);
const float specular = 20.0;
const vec3 lightColor = vec3(1.0, 1.0, 1.0);
const float specularHardness = 512.0;

void main() {

    vec3 lightDir = normalize(light.direction.xyz);

    vec3 normal = getNormal(vs_position);
    vec3 view = normalize(cameraPos - vs_position);

    vec2 screenSpace = (vs_glPosition.xy / vs_glPosition.w) / 2.0 + 0.5;
    vec2 distort = vec2(0.05, 0.0) * normal.xz + screenSpace;
    distort.x = clamp(distort.x, 0.001, 0.999);
    distort.y = clamp(distort.y, 0.001, 0.999);

    vec3 refractColor = texture(refractTexture, distort).rgb;
    vec3 reflectColor = texture(reflectTexture, distort * vec2(1.0, -1.0)).rgb;

    vec3 color = vec3(0.0);

    vec3 diffuse = vec3(0.1) * max(0.0, dot(normal, lightDir));
    color += diffuse;

    float spec = dot(view, reflect(-lightDir * vec3(1.0, -1.0, 1.0), normal));
    if(spec > 0.0) {
        spec = pow(spec, 128.0);
        color += vec3(spec);
    }

    float waterDepth = 8.0 + getHeight(vs_position.xz) - texture(heightMap, vs_texCoord).r * 0.1 * 128.0;

    vec3 oceanColor = mix(vec3(0.0, 0.06, 0.1), refractColor * vec3(0.8, 0.95, 1.0), pow( 1.0 - waterDepth / 9.0, 5.0));

    float fresnel = pow(1.0 - dot(view, -normal), 8.0);
    fresnel = mix(0.0, 1.0, min(1.0, fresnel));
    color += mix(oceanColor, reflectColor * vec3(0.8, 0.9, 1.0), fresnel);

    fs_diffuse = vec4(color, 1.0);
}