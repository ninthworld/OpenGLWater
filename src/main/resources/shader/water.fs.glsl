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
uniform sampler2D foamTexture;

uniform vec4 cameraPos;
uniform float time;

vec3 getNormal(vec2 pos);

void main() {

    const float ambientCoefficent = 0.1;
    const float materialShininess = 8.0;
    const vec3 materialSpecularColor = vec3(1.0, 1.0, 1.0);

    vec3 normal = getNormal(vs_position.xz);
    vec3 surfacePos = vs_position;
    vec3 surfaceColor = vec3(0.3, 0.5, 0.7);
    vec3 surfaceToLight = normalize(light.direction.xyz);
    vec3 surfaceToCamera = normalize(cameraPos.xyz - surfacePos);

    float waterLevel = surfacePos.y;

    float depth = 1.0 - texture(heightMap, vs_texCoord).r * 0.1 * 128.0 / waterLevel;

    // Water Surface
    vec2 fragCoord = (vs_glPosition.xy / vs_glPosition.w) / 2.0 + 0.5;
    vec2 normFragCoord = abs(0.1 * normal.y * normal.xz) + fragCoord;

    vec3 refractColor = texture(refractTexture, normFragCoord).rgb;
    vec3 reflectColor = texture(reflectTexture, normFragCoord * vec2(1.0, -1.0)).rgb;
    float refractValue = dot(surfaceToCamera, vec3(0.0, 1.0, 0.0));
    vec3 fresnelColor = mix(reflectColor, refractColor, refractValue);
    surfaceColor *= fresnelColor;

    // Depth Color
    surfaceColor *= mix(vec3(1.0, 1.0, 1.0), vec3(0.05, 0.3, 0.5), depth);

    // Ambient
    vec3 ambient = ambientCoefficent * surfaceColor;

    // Diffuse
    float diffuseCoefficent = max(0.0, dot(normal, surfaceToLight));
    vec3 diffuse = diffuseCoefficent * surfaceColor;

    // Specular
    float specularCoefficient = 0.0;
    if(diffuseCoefficent > 0.0) {
        specularCoefficient = pow(max(0.0, dot(surfaceToCamera, reflect(-surfaceToLight, normal))), materialShininess);
    }
    vec3 specular = specularCoefficient * materialSpecularColor;

    // Linear Color
    vec3 linearColor = ambient + diffuse + specular;

    // Final Color
    vec3 gamma = vec3(1.0 / 2.2);
    vec3 finalColor = vec3(pow(linearColor, gamma));

    // Foam
    vec3 foamColor = texture(foamTexture, surfacePos.xz / 2.0).rgb;
    //finalColor = mix(finalColor, foamColor, clamp(dot(vec3(0.0, 1.0, 0.0), normal), 0.0, 1.0));

    // Shore Blending
    // vec3 shoreColor = mix(texture(refractTexture, fragCoord).rgb + mix(vec3(0.0, 0.0, 0.0), texture(foamTexture, surfacePos.xz).rgb, clamp(depth * 16.0, 0.0, 1.0)), finalColor, clamp(depth * 8.0, 0.0, 1.0));
    vec3 shoreColor = mix(foamColor, finalColor, clamp(depth * 6.0, 0.0, 1.0));
    shoreColor = mix(texture(refractTexture, fragCoord).rgb, shoreColor, clamp(depth * 20.0, 0.0, 1.0));

    fs_diffuse = vec4(shoreColor, 1.0);
}

float noise(vec2 x);

// Inspired by Kevin Roast
// <https://github.com/kevinroast/webglshaders/blob/master/waves3.html>
float getFractal(vec2 pos) {
    const int octaves = 8;

    float amp1 = 1.8;
    float amp2 = 0.8;
    float sum = 0.0;
    for(int i=0; i<octaves; ++i) {
        sum += noise(pos + time * 0.5) * amp1 * 0.25;
        if(i < 2) {
            sum += noise(pos.yx - time * 0.25) * amp2 * 0.1;
        }
        else {
            sum += abs(noise(pos.yx - time * 0.8) * amp2 * 0.05) * 2.0;
        }
        amp1 *= 0.4;
        amp2 *= 0.5;
        pos.x = pos.x * 1.75 + pos.y;
        pos.y = pos.y * 1.75 - pos.x;
    }
    return sum;
}

vec3 getNormal(vec2 pos) {
    const float o = 0.1;
    float h01 = getFractal(pos + vec2(-o, 0));
    float h21 = getFractal(pos + vec2(o, 0));
    float h10 = getFractal(pos + vec2(0, -o));
    float h12 = getFractal(pos + vec2(0, o));
    vec3 a = normalize(vec3(2.0 * o, 0.0, h01 - h21));
    vec3 b = normalize(vec3(0.0, 2.0 * o, h10 - h12));
    return normalize(cross(a, b));
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