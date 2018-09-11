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
layout(binding=3) uniform sampler2D heightMap;
layout(binding=4) uniform sampler2D normalMap;

const float waterLevel = 8.0;

const float fadeSpeed = 0.15;

const float normalScale = 1.0;

const float R0 = 0.5;

const float maxAmplitude = 1.0;

const vec3 sunColor = vec3(1.0, 1.0, 1.0);

const float shoreHardness = 1.0;

const float refractionStrength = 0.0;

const vec4 normalModifier = vec4(1.0, 2.0, 4.0, 8.0);

const float displace = 1.7;

const vec3 foamExistence = vec3(0.65, 1.35, 0.5);

const float sunScale = 3.0;

const mat4 matReflection = mat4(
    0.5, 0.0, 0.0, 0.0,
    0.0, -0.5, 0.0, 0.0,
    0.0, 0.0, 0.0, 0.0,
    0.5, 0.5, 0.5, 1.0);

const float shininess = 0.7;
const float specularIntensity = 0.32;

const vec3 depthColor = vec3(0.0078, 0.5176, 0.7);

const vec3 bigDepthColor = vec3(0.0039, 0.00196, 0.145);
const vec3 extinction = vec3(7.0, 30.0, 40.0);

const float visibility = 4.0;

const vec2 scale = vec2(0.005, 0.005);
const float refractionScale = 0.005;

const vec2 wind = vec2(-0.3, 0.7);

mat3 computeTangentFrame(vec3 N, vec3 P, vec2 UV) {
    vec3 dp1 = dFdx(P);
    vec3 dp2 = dFdy(P);
    vec2 duv1 = dFdx(UV);
    vec2 duv2 = dFdy(UV);

    mat3 M = mat3(dp1, dp2, cross(dp1, dp2));
    mat2x3 invM = mat2x3(cross(M[1], M[2]), cross(M[2], M[0]));
    vec3 T = invM * vec2(duv1.x, duv2.x);
    vec3 B = invM * vec2(duv1.y, duv2.y);

    return mat3(normalize(T), normalize(B), N);
}

float fresnelTerm(vec3 normal, vec3 eyeVec) {
    float angle = 1.0 - clamp(dot(normal, eyeVec), 0.0, 1.0);
    float fresnel = pow(angle, 5.0);
    return clamp(fresnel * (1.0 - clamp(R0, 0.0, 1.0)) + R0 - refractionStrength, 0.0, 1.0);
}

vec3 getPosition(vec2 texCoord) {
    float depth = texture(depthTexture, texCoord).r;
	vec4 ssPos = invCamera.invProjMatrix * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
	vec4 wsPos = invCamera.invViewMatrix * ssPos;
	return wsPos.xyz / wsPos.w;
}

void main() {

    vec3 color2 = texture(sceneTexture, vs_texCoord).rgb;
    vec3 color = color2;

    vec3 position = getPosition(vs_texCoord);
    float level = waterLevel;
    float depth = 0.0;

    if(level >= cameraPos.y) {
        fs_diffuse = vec4(color2, 1.0);
        return;
    }

    if(position.y <= level + maxAmplitude) {
        vec3 eyeVec = position - cameraPos.xyz;
        float diff = level - position.y;
        float cameraDepth = cameraPos.y - position.y;

        vec3 eyeVecNorm = normalize(eyeVec);
        float t = (level - cameraPos.y) / eyeVecNorm.y;
        vec3 surfacePoint = cameraPos.xyz + eyeVecNorm * t;

        eyeVecNorm = normalize(eyeVecNorm);

        vec2 texCoord = vec2(0.0, 0.0);
        for(int i=0; i<10; ++i) {
            texCoord = (surfacePoint.xz + eyeVecNorm.xz * 0.1) * scale + time * 0.00005 * wind;

            float bias = texture(heightMap, texCoord).r;

            bias *= 0.1;
            level += bias * maxAmplitude;
            t = (level - cameraPos.y) / eyeVecNorm.y;
            surfacePoint = cameraPos.xyz + eyeVecNorm * t;
        }

        depth = length(position - surfacePoint);
        float depth2 = surfacePoint.y - position.y;

        eyeVecNorm = normalize(cameraPos.xyz - surfacePoint);

        float normal1 = texture(heightMap, (texCoord + vec2(-1.0, 0.0) / 256.0)).r;
        float normal2 = texture(heightMap, (texCoord + vec2(1.0, 0.0) / 256.0)).r;
        float normal3 = texture(heightMap, (texCoord + vec2(0.0, -1.0) / 256.0)).r;
        float normal4 = texture(heightMap, (texCoord + vec2(0.0, 1.0) / 256.0)).r;

        vec3 myNormal = normalize(vec3((normal1 - normal2) * maxAmplitude, normalScale, (normal3 - normal4) * maxAmplitude));

        texCoord = surfacePoint.xz * 1.6 + wind * time * 0.00016;
        mat3 tangentFrame = computeTangentFrame(myNormal, eyeVecNorm, texCoord);
        vec3 normal0a = normalize(tangentFrame * (2.0 * texture(normalMap, texCoord).rgb - 1.0));

        texCoord = surfacePoint.xz * 0.8 + wind * time * 0.00008;
        tangentFrame = computeTangentFrame(myNormal, eyeVecNorm, texCoord);
        vec3 normal1a = normalize(tangentFrame * (2.0 * texture(normalMap, texCoord).rgb - 1.0));

        texCoord = surfacePoint.xz * 0.4 + wind * time * 0.00004;
        tangentFrame = computeTangentFrame(myNormal, eyeVecNorm, texCoord);
        vec3 normal2a = normalize(tangentFrame * (2.0 * texture(normalMap, texCoord).rgb - 1.0));

        texCoord = surfacePoint.xz * 0.1 + wind * time * 0.00001;
        tangentFrame = computeTangentFrame(myNormal, eyeVecNorm, texCoord);
        vec3 normal3a = normalize(tangentFrame * (2.0 * texture(normalMap, texCoord).rgb - 1.0));

        vec3 normal = normalize(normal0a * normalModifier.x + normal1a * normalModifier.y + normal2a * normalModifier.z + normal3a * normalModifier.w);

        texCoord = vs_texCoord;
        texCoord.x += sin(time * 0.002 + 3.0 * abs(position.y)) * (refractionScale * min(depth2, 1.0));
        vec3 refraction = texture(sceneTexture, texCoord).rgb;
        if(getPosition(texCoord).y > level) {
            refraction = color2;
        }

        mat4 matTextureProj = matReflection * (camera.projMatrix * camera.viewMatrix);

        vec3 waterPosition = surfacePoint.xyz;
        waterPosition.y -= (level - waterLevel);
        vec4 texCoordProj = matTextureProj * vec4(waterPosition, 1.0);
        texCoordProj.x += displace * normal.x;
        texCoordProj.z += displace * normal.z;

        vec3 reflection = texture(reflectTexture, texCoordProj.xy/texCoordProj.w).rgb;

        float fresnel = fresnelTerm(normal, eyeVecNorm);

        vec3 depthN = vec3(depth * fadeSpeed);
        vec3 waterCol = clamp(length(sunColor) / vec3(sunScale), 0.0, 1.0);
        refraction = mix(mix(refraction, depthColor * waterCol, clamp(depthN / visibility, 0.0, 1.0)), bigDepthColor * waterCol, clamp(depth2 / extinction, 0.0, 1.0));

        float foam = 0.0;

        texCoord = (surfacePoint.xz + eyeVecNorm.xz * 0.1) * 0.05 + time * 0.00001 * wind + sin(time * 0.001 + position.x) * 0.005;
        vec2 texCoord2 = (surfacePoint.xz + eyeVecNorm.xz * 0.1) * 0.05 + time * 0.00002 * wind + sin(time * 0.001 + position.z) * 0.005;

        if(depth2 < foamExistence.x) {
            foam = 0.5;
        }
        else if(depth2 < foamExistence.y) {
            foam = mix(0.5, 0.0, (depth2 - foamExistence.x) / (foamExistence.y - foamExistence.x));
        }

        if(maxAmplitude - foamExistence.z > 0.0001) {
            foam += 0.5 * clamp((level - (waterLevel + foamExistence.z)) / (maxAmplitude - foamExistence.z), 0.0, 1.0);
        }
        //foam = 0.0;

        vec3 specular = vec3(0.0, 0.0, 0.0);

        vec3 lightDir = light.direction.xyz;
        vec3 mirrorEye = (2.0 * dot(eyeVecNorm, normal) * normal - eyeVecNorm);
        float dotSpec = clamp(dot(mirrorEye.xyz, -lightDir) * 0.5 + 0.5, 0.0, 1.0);
        specular = (1.0 - fresnel) * clamp(-lightDir.y, 0.0, 1.0) * (pow(dotSpec, 512.0) * (shininess * 1.8 + 0.2)) * sunColor;
        specular += specular * 25.0 * clamp(shininess - 0.05, 0.0, 1.0) * sunColor;

        color = mix(refraction, reflection, fresnel);
        color = clamp(color + max(specular, foam * sunColor), 0.0, 1.0);
        color = mix(refraction, color, clamp(depth * shoreHardness, 0.0, 1.0));
    }

    fs_diffuse = vec4(color, 1.0);
}