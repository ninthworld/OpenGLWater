#version 430

in vec2 vs_texCoord;

out vec4 fs_diffuse;

layout(std140, binding=0) uniform Camera {
    mat4 projMatrix;
    mat4 viewMatrix;
} camera;

layout(std140, binding=1) uniform InvCamera {
    mat4 invProjMatrix;
    mat4 invViewMatrix;
} invCamera;

layout(std140, binding=2) uniform Light {
    vec4 direction;
} light;

layout(location=5) uniform vec4 cameraPos;
layout(location=6) uniform float time;

layout(binding=0) uniform sampler2D sceneTexture;
layout(binding=1) uniform sampler2D depthTexture;
layout(binding=2) uniform sampler2D reflectTexture;

const float waterLevel = 8.0;

float noise(vec2 x);

float getFractal(vec2 P);
float getHeight(vec2 P);
vec3 getNormal(vec2 P);

vec3 getPosition(vec2 texCoord);

void main() {

    vec3 color = texture(sceneTexture, vs_texCoord).rgb;
    vec3 position = getPosition(vs_texCoord);

    vec3 eye = position - cameraPos.y;
    float diff = waterLevel - position.y;
    float camDepth = cameraPos.y - position.y;

    vec3 eyeNorm = normalize(eye);
    float t = (waterLevel - cameraPos.y) / eyeNorm.y;
    vec3 surface = cameraPos.xyz + eyeNorm * t;
    vec3 surfaceToCamera = normalize(cameraPos.xyz - surface);

    float surfaceLevel = waterLevel;

    if(cameraPos.y < surfaceLevel) {
        if(position.y > surfaceLevel) {
            // Under Water - Surface
            color += vec3(0.0, 0.0, 0.8);
        }
        else {
            // Under Water - Ambience
            color += vec3(0.0, 0.0, 0.2);
        }
    }
    else if(cameraPos.y >= surfaceLevel && position.y <= surfaceLevel) {
        // Above Water - Surface

        vec3 normal = getNormal(surface.xz);
        vec3 surfaceToLight = normalize(light.direction.xyz);

        vec3 refractColor = texture(sceneTexture, vs_texCoord).rgb;
        vec3 reflectColor = texture(reflectTexture, vs_texCoord * vec2(1.0, -1.0)).rgb;
        float refractValue = dot(surfaceToCamera, vec3(0.0, 1.0, 0.0));
        vec3 fresnelColor = mix(reflectColor, refractColor, refractValue);

        // Diffuse
        float diffuseCoefficient = max(0.0, dot(normal, surfaceToLight));
        vec3 diffuse = diffuseCoefficient * fresnelColor;

        // Specular
        float specularCoefficient = 0.0;
        if(diffuseCoefficient > 0.0) {
            specularCoefficient = pow(max(0.0, dot(surfaceToCamera, reflect(-surfaceToLight, normal))), 5.0);
        }
        vec3 specular = specularCoefficient * vec3(1.0, 0.8, 0.6);

        // Linear Color
        vec3 linearColor = diffuse + specular;

        // Final Color
        vec3 gamma = vec3(1.0 / 2.2);
        color = pow(linearColor, gamma);
    }

    fs_diffuse = vec4(color, 1.0);
}

vec3 getPosition(vec2 texCoord) {
    float depth = texture(depthTexture, texCoord).r;
	vec4 ssPos = invCamera.invProjMatrix * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
	vec4 wsPos = invCamera.invViewMatrix * ssPos;
	return wsPos.xyz / wsPos.w;
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

float getHeight(vec2 pos) {
    return getFractal(pos);
}

float getFractal(vec2 pos) {
    float amp = 1.0;
    float val = 0.0;
    for(int i=0; i<8; ++i) {
        val += noise(pos + time * 0.5) * amp;
        amp *= 0.5;
    }
    return val;
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