#version 420

#define OCTAVES 4

in vec2 vs_position;

out vec4 fs_color;

uniform float time;
uniform sampler2D colorTextures[OCTAVES];

//float getHeight(vec2 pos) {
//    float dt = time * 0.002; //time * 1.5;
//    float amp1 = 1.8;
//    float amp2 = 0.8;
//    float sum = 0.0;
//    for(int i=0; i<OCTAVES; ++i) {
//        sum += texture(colorTextures[i], pos + dt * 0.5).r * amp1 * 0.25;
//        if(i < 2) {
//            sum += texture(colorTextures[i], pos.yx - dt * 0.25).r * amp2 * 0.1;
//        }
//        else {
//            sum += abs(texture(colorTextures[i], pos.yx - dt * 0.8).r * amp2 * 0.05) * 2.0;
//        }
//        amp1 *= 0.4;
//        amp2 *= 0.5;
//        pos.x = pos.x * 1.5 + pos.y;
//        pos.y = pos.y * 1.5 - pos.x;
//    }
//    return sum;
//}
//
//void main(){
//
//    vec2 pos = vs_position * 32.0;
//    float dt = time * 0.05;
//    float val = 0.0;
//    val += texture(colorTextures[0], vec2(0.5, 1.0) * pos * 0.05 + vec2(dt, 0.0)).r * 0.8;
//    val += texture(colorTextures[1], vec2(1.0, 0.5) * pos * 0.1 + vec2(0.0, dt)).r * 0.1;
//    val += texture(colorTextures[2], pos * 0.2 + vec2(dt, dt)).r * 0.05;
//    val += texture(colorTextures[3], pos * 0.4 + vec2(-dt, -dt)).r * 0.05;
//
//    vec3 color = vec3(val);
//
//    fs_color = vec4(color, 1.0);
//}

#define GAMMA 0.8
#define CONTRAST 1.1
#define SATURATION 1.3
#define BRIGHTNESS 1.3
#define RAY_DEPTH 256
#define MAX_DEPTH 200.0
#define DISTANCE_MIN 0.001
#define PI 3.14159265

const vec2 delta = vec2(DISTANCE_MIN, 0.);
const vec2 delta2 = vec2(0.01, 0.);

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

// Octave transform matrix from Alexander Alekseev aka TDM
mat2 octave_m = mat2(1.6,1.2,-1.2,1.6);

float FractalNoise(in vec2 xy)
{
   float m = 1.5;
   float w = 0.5;
   float f = 0.0;
   for (int i = 0; i < 4; i++)
   {
      f += Noise(xy+time*0.511) * m * 0.15;
      f += Noise(xy.yx-time*0.333) * w * 0.25;
      w *= 0.5;
      m *= 0.25;
      xy *= octave_m;
   }
   return f;
}

float Dist(vec3 pos)
{
   return dot(pos-vec3(0.,-FractalNoise(pos.xz),0.), vec3(0.,1.,0.));
}

vec3 GetNormal(vec3 pos)
{
   vec3 n;
   n.x = Dist( pos + delta.xyy ) - Dist( pos - delta.xyy );
   n.y = Dist( pos + delta.yxy ) - Dist( pos - delta.yxy );
   n.z = Dist( pos + delta.yyx ) - Dist( pos - delta.yyx );

   return normalize(n);
}

float Noise2(in vec2 x, in int o)
{
    return texture(colorTextures[o], x * pow(0.5, o)).r;
}

float FractalNoise2(in vec2 xy)
{
   float m = 1.5;
   float w = 0.5;
   float f = 0.0;
   for (int i = 0; i < 4; i++)
   {
      f += Noise2(xy + time*0.0511, i) * m * 0.15;
      f += Noise2(xy.yx - time*0.0333, i) * w * 0.25;
      w *= 0.5;
      m *= 0.25;
      xy *= octave_m;
   }
   return f;
}

float Dist2(vec3 pos)
{
   return dot(pos-vec3(0.,-FractalNoise2(pos.xz),0.), vec3(0.,1.,0.));
}

vec3 GetNormal2(vec3 pos)
{
   vec3 n;
   n.x = Dist2( pos + delta2.xyy ) - Dist2( pos - delta2.xyy );
   n.y = Dist2( pos + delta2.yxy ) - Dist2( pos - delta2.yxy );
   n.z = Dist2( pos + delta2.yyx ) - Dist2( pos - delta2.yyx );

   return normalize(n);
}

void main() {

    vec3 pos = vec3(vs_position.x, 0.0, vs_position.y);

    vec3 color = vec3(0.0);
    if(vs_position.y < 0.5) {

        if(vs_position.x < 0.5) {
            color = GetNormal(pos * 32.0) * 0.5 + 0.5;
        }
        else {
            color = GetNormal2(pos * 2.0) * 0.5 + 0.5;
        }
    }
    else {
        if(vs_position.x < 0.5) {
            color = vec3(FractalNoise(pos.xz * 32.0));
        }
        else {
            color = vec3(FractalNoise2(pos.xz * 2.0));
        }
    }

    fs_color = vec4(color, 1.0);
}