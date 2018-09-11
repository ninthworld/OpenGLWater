#version 430

in vec4 gs_ssPosition;
in vec3 gs_position;
in vec3 gs_normal;
in vec3 gs_tangent;
in vec3 gs_bitangent;
in vec2 gs_texCoord;

out vec4 fs_diffuse;

layout(std140, binding=1) uniform Light {
    vec4 direction;
} light;

layout(location=1) uniform vec4 cameraPos;
layout(location=2) uniform float time;

layout(binding=0) uniform sampler2D refractTexture;
layout(binding=1) uniform sampler2D reflectTexture;
layout(binding=2) uniform sampler2D heightMap;

#define GAMMA 0.8
#define CONTRAST 1.1
#define SATURATION 1.3
#define BRIGHTNESS 1.3
#define ANTIALIAS_SAMPLES 16
#define NOISE_PASSES 8
#define RAY_DEPTH 256
#define MAX_DEPTH 200.0
#define DISTANCE_MIN 0.003
#define PI 3.14159265

const vec3 WATER_COLOR = vec3(0.6,0.85,0.65);
const vec3 SPECULAR_COLOR = vec3(1.1,0.8,0.6);

const vec2 delta = vec2(DISTANCE_MIN, 0.);

float Hash(in float n)
{
    return fract(sin(n)*43758.5453123);
}

float Noise(in vec2 x)
{
    vec2 p = floor(x);
    vec2 f = fract(x);
    f = f*f*(3.0-2.0*f);
    float n = p.x + p.y*57.0;
    float res = mix(mix( Hash(n+  0.0), Hash(n+  1.0),f.x),
                    mix( Hash(n+ 57.0), Hash(n+ 58.0),f.x),f.y);
    return res;
}

//	FAST32_hash
//	A very fast hashing function.  Requires 32bit support.
//	http://briansharpe.wordpress.com/2011/11/15/a-fast-and-simple-32bit-floating-point-hash-function/
void FAST32_hash_2D( vec2 gridcell, out vec4 hash_0, out vec4 hash_1 )
{
   // gridcell is assumed to be an integer coordinate
   const vec2 OFFSET = vec2( 26.0, 161.0 );
   const float DOMAIN = 71.0;
   const vec2 SOMELARGEFLOATS = vec2( 951.135664, 642.949883 );
   vec4 P = vec4( gridcell.xy, gridcell.xy + 1.0 );
   P = P - floor(P * ( 1.0 / DOMAIN )) * DOMAIN;
   P += OFFSET.xyxy;
   P *= P;
   P = P.xzxz * P.yyww;
   hash_0 = fract( P * ( 1.0 / SOMELARGEFLOATS.x ) );
   hash_1 = fract( P * ( 1.0 / SOMELARGEFLOATS.y ) );
}

vec2 Interpolation_C2( vec2 x ) { return x * x * x * (x * (x * 6.0 - 15.0) + 10.0); }

//	Perlin Noise 2D  ( gradient noise )
//	Return value range of -1.0->1.0
//	http://briansharpe.files.wordpress.com/2011/11/perlinsample.jpg
float Perlin2D( vec2 P )
{
    //	establish our grid cell and unit position
    vec2 Pi = floor(P);
    vec4 Pf_Pfmin1 = P.xyxy - vec4( Pi, Pi + 1.0 );

    //	calculate the hash.
    vec4 hash_x, hash_y;
    FAST32_hash_2D( Pi, hash_x, hash_y );

    //	calculate the gradient results
    vec4 grad_x = hash_x - 0.49999;
    vec4 grad_y = hash_y - 0.49999;
    vec4 grad_results = inversesqrt( grad_x * grad_x + grad_y * grad_y ) * ( grad_x * Pf_Pfmin1.xzxz + grad_y * Pf_Pfmin1.yyww );

    //	Classic Perlin Interpolation
    grad_results *= 1.4142135623730950488016887242097;
    vec2 blend = Interpolation_C2( Pf_Pfmin1.xy );
    vec4 blend2 = vec4( blend, vec2( 1.0 - blend ) );
    return dot( grad_results, blend2.zxzx * blend2.wwyy );
}

// Octave transform matrix from Alexander Alekseev aka TDM
mat2 octave_m = mat2(1.6,1.2,-1.2,1.6);

// FBM Noise - mixing Value noise and Perlin Noise - also ridged turbulence at smaller octaves
float FractalNoise(in vec2 xy)
{
   float m = 1.25;
   float w = 0.6;
   float f = 0.0;
   for (int i = 0; i < NOISE_PASSES; i++)
   {
      f += Noise(xy.xy+time*0.655) * m * 0.25;
      if (i < 2)
      {
         f += Perlin2D(xy.yx-time*0.233) * w * 0.12;
      }
      else
      {
         // ridged turbulence at smaller scales - moves 4x faster
         f += abs(Perlin2D(xy.yx-time*0.932) * w * 0.05)*1.75;
      }
      w *= 0.45;
      m *= 0.35;
      xy *= octave_m;
   }
   return f;
}

vec3 getNormal(vec2 pos) {
    const float o = 0.01;
    float h01 = FractalNoise(pos + vec2(-o, 0));
    float h21 = FractalNoise(pos + vec2(o, 0));
    float h10 = FractalNoise(pos + vec2(0, -o));
    float h12 = FractalNoise(pos + vec2(0, o));
    vec3 a = normalize(vec3(2.0 * o, 0.0, h01 - h21));
    vec3 b = normalize(vec3(0.0, 2.0 * o, h10 - h12));
    return normalize(cross(a, b));
}

void main() {

    const float ambientCoefficent = 0.1;
    const vec3 intensities = vec3(1.0, 1.0, 1.0);
    const float materialShininess = 4.0;
    const vec3 materialSpecularColor = SPECULAR_COLOR;//vec3(1.0, 1.0, 1.0);

    vec3 normal = getNormal(gs_texCoord * 32.0);//gs_normal;
    vec3 surfacePos = gs_position;
    vec4 surfaceColor = vec4(0.6, 0.85, 0.65, 1.0);
    vec3 surfaceToLight = normalize(light.direction.xyz);
    vec3 surfaceToCamera = normalize(cameraPos.xyz - surfacePos);

    // Water Surface
    vec2 fragCoord = 0.3 * normal.xz + (gs_ssPosition.xy / gs_ssPosition.w) / 2.0 + 0.5;
    //fragCoord = clamp(fragCoord, 0.001, 0.999);

    vec3 refractColor = texture(refractTexture, fragCoord).rgb;
    vec3 reflectColor = texture(reflectTexture, fragCoord * vec2(1.0, -1.0)).rgb;
    float refractValue = dot(surfaceToCamera, vec3(0.0, 1.0, 0.0));
    vec3 fresnelColor = mix(reflectColor, refractColor, refractValue);
    surfaceColor.rgb *= fresnelColor;

    // Ambient
    vec3 ambient = ambientCoefficent * surfaceColor.rgb * intensities;

    // Diffuse
    float diffuseCoefficent = max(0.0, dot(normal, surfaceToLight));
    vec3 diffuse = diffuseCoefficent * surfaceColor.rgb * intensities;

    // Specular
    float specularCoefficient = 0.0;
    if(diffuseCoefficent > 0.0) {
        specularCoefficient = pow(max(0.0, dot(surfaceToCamera, reflect(-surfaceToLight, normal))), materialShininess);
    }
    vec3 specular = specularCoefficient * materialSpecularColor * intensities;

    // Linear Color
    vec3 linearColor = ambient + diffuse + specular;

    // Final Color
    vec3 gamma = vec3(1.0 / 2.2);
    fs_diffuse = vec4(pow(linearColor, gamma), surfaceColor.a);
}