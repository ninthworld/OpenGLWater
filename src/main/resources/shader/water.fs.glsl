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

uniform vec4 cameraPos;
uniform float time;

float noise(vec2 x);

// Inspired by Kevin Roast
// <https://github.com/kevinroast/webglshaders/blob/master/waves3.html>

float getHeight(vec2 pos) {
    const int octaves = 6;
    float dt = time * 1.5;
    float amp1 = 1.8;
    float amp2 = 0.8;
    float sum = 0.0;
    for(int i=0; i<octaves; ++i) {
        sum += noise(pos + dt * 0.5) * amp1 * 0.25;
        if(i < 2) {
            sum += noise(pos.yx - dt * 0.25) * amp2 * 0.1;
        }
        else {
            sum += abs(noise(pos.yx - dt * 0.8) * amp2 * 0.05) * 2.0;
        }
        amp1 *= 0.4;
        amp2 *= 0.5;
        pos.x = pos.x * 1.5 + pos.y;
        pos.y = pos.y * 1.5 - pos.x;
    }
    return sum;
}

vec3 getNormal(vec2 pos) {
    const float o = 0.01;
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
    float waterLevel = 8.0 + getHeight(vs_position.xz) * 0.5;
    float normalDepth = clamp(terrainHeight / waterLevel, 0.0, 1.0);

    // Fresnel
    vec2 ssDistort = vec2(0.05, 0.0) * normal.xz + screenSpace;
    ssDistort.x = clamp(ssDistort.x, 0.001, 0.999);
    ssDistort.y = clamp(ssDistort.y, 0.001, 0.999);

    vec3 refractColor = texture(refractTexture, ssDistort).rgb;
    vec3 reflectColor = texture(reflectTexture, ssDistort * vec2(1.0, -1.0)).rgb;

    vec3 skyColor = reflectColor;
    vec3 oceanColor = mix(refractColor, vec3(0.0056, 0.0224, 0.056), 1.0 - pow(normalDepth, 3.0));

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

//	<https://www.shadertoy.com/view/4dS3Wd>
//	By Morgan McGuire @morgan3d, http://graphicscodex.com
//
float hash(float n) { return fract(sin(n) * 1e4); }
float hash(vec2 p) { return fract(1e4 * sin(17.0 * p.x + p.y * 0.1) * (0.1 + abs(sin(p.y * 13.0 + p.x)))); }

float noise(float x) {
    float i = floor(x);
    float f = fract(x);
    float u = f * f * (3.0 - 2.0 * f);
    return mix(hash(i), hash(i + 1.0), u);
}

float noise(vec2 x) {
    vec2 i = floor(x);
    vec2 f = fract(x);

    // Four corners in 2D of a tile
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    // Simple 2D lerp using smoothstep envelope between the values.
    // return vec3(mix(mix(a, b, smoothstep(0.0, 1.0, f.x)),
    //			mix(c, d, smoothstep(0.0, 1.0, f.x)),
    //			smoothstep(0.0, 1.0, f.y)));

    // Same code, with the clamps in smoothstep and common subexpressions
    // optimized away.
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}