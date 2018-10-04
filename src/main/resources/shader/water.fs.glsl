#version 420

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

float getHeight(vec2 pos) {
    float dt = time * 0.05 * 4.0;
    float val = 0.0;
    val += texture(noiseTextures[0], vec2(0.5, 1.0) * pos * 0.05 + vec2(dt, 0.0)).r * 0.8;
    val += texture(noiseTextures[1], vec2(1.0, 0.5) * pos * 0.1 + vec2(0.0, dt)).r * 0.14;
//    val += texture(noiseTextures[2], pos * 0.2 + vec2(dt, dt)).r * 0.04;
//    val += texture(noiseTextures[3], pos * 0.4 + vec2(-dt, -dt)).r * 0.02;
    val += (1.0 - abs(texture(noiseTextures[2], pos * 0.2 + vec2(dt, dt)).r * 2.0 - 1.0)) * 0.04;
    val += (1.0 - abs(texture(noiseTextures[3], pos * 0.4 + vec2(-dt, -dt)).r * 2.0 - 1.0)) * 0.02;
    return val;
}

vec3 getNormal(vec2 pos) {
    const float o = 0.08;
    float h01 = getHeight(pos + vec2(-o, 0));
    float h21 = getHeight(pos + vec2(o, 0));
    float h10 = getHeight(pos + vec2(0, -o));
    float h12 = getHeight(pos + vec2(0, o));
    vec3 a = normalize(vec3(2.0 * o, 0.0, h01 - h21));
    vec3 b = normalize(vec3(0.0, 2.0 * o, h10 - h12));
    return normalize(cross(a, b));
}

vec3 hdr(vec3 color, float exposure) {
    return 1.0 - exp(-color * exposure);
}

void main() {

    vec2 screenSpace = (vs_glPosition.xy / vs_glPosition.w) / 2.0 + 0.5;

    vec3 normal = getNormal(vs_position.xz);
    vec3 view = normalize(cameraPos.xyz - vs_position);

    // Terrain
    float terrainHeight = texture(heightMap, vs_texCoord).r * 0.1 * 128.0;
    float waterLevel = 8.0;// + getHeight(vs_position.xz) * 0.5;
    float normalDepth = clamp(terrainHeight / waterLevel, 0.0, 1.0);

    // Fresnel
    vec2 ssDistort = vec2(0.05, 0.0) * normal.xz + screenSpace;
    ssDistort.x = clamp(ssDistort.x, 0.001, 0.999);
    ssDistort.y = clamp(ssDistort.y, 0.001, 0.999);

    vec3 refractColor = texture(refractTexture, ssDistort).rgb;
    vec3 reflectColor = texture(reflectTexture, ssDistort * vec2(1.0, -1.0)).rgb;

    if(cameraPos.y < waterLevel) {
        vec3 col = reflectColor;
        reflectColor = refractColor;
        refractColor = col;
    }

    vec3 skyColor = reflectColor;
    vec3 oceanColor = mix(refractColor, vec3(0.0056, 0.0224, 0.056), 1.0 - normalDepth);

    float fresnel = 0.02 + 0.98 * pow(1.0 - dot(normalize(vec3(0.0, 1.0, 0.0) + normal * 0.2), view), 5.0);

    // Diffuse
    float diffuse = clamp(dot(normal, normalize(light.direction.xyz)), 0.0, 1.0);

    vec3 color = fresnel * skyColor + (1.0 - fresnel) * oceanColor * diffuse * skyColor;

    // Specular
    if(diffuse > 0.0) {
        color += pow(max(0.0, dot(-view, reflect(-normalize(light.direction.xyz), normal))), 32.0) * 0.25;
    }

    vec3 finalColor = hdr(color, 1.75);

    // Shore blending
    finalColor = mix(finalColor, texture(refractTexture, screenSpace).rgb, clamp(pow(normalDepth * 2.0 - 1.0, 16.0), 0.0, 1.0));

    fs_diffuse = vec4(finalColor, 1.0);
}