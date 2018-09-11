#version 420

layout(triangles) in;
layout(triangle_strip, max_vertices=3) out;

in vec3 vs_normal[];
in vec2 vs_texCoord[];

out vec3 gs_position;
out vec3 gs_normal;
out vec3 gs_tangent;
out vec3 gs_bitangent;
out vec2 gs_texCoord;

layout(std140, binding=0) uniform Camera {
    mat4 projMatrix;
    mat4 viewMatrix;
    vec4 position;
} camera;

vec3 calcTangent() {
	vec3 v0 = gl_in[0].gl_Position.xyz;
	vec3 v1 = gl_in[1].gl_Position.xyz;
	vec3 v2 = gl_in[2].gl_Position.xyz;

	vec3 e1 = v1 - v0;
	vec3 e2 = v2 - v0;

	vec2 uv0 = vs_texCoord[0];
	vec2 uv1 = vs_texCoord[1];
	vec2 uv2 = vs_texCoord[2];

	vec2 deltaUV1 = uv1 - uv0;
	vec2 deltaUV2 = uv2 - uv0;

	float r = 1.0 / (deltaUV1.x * deltaUV2.y - deltaUV1.y * deltaUV2.x);

	return normalize((e1 * deltaUV2.y - e2 * deltaUV1.y) * r);
}

void main() {
	for (int i = 0; i < gl_in.length(); ++i) {
	    gs_position = gl_in[i].gl_Position.xyz;
		gs_normal = vs_normal[i];
	    gs_tangent = calcTangent();
	    gs_bitangent = normalize(cross(gs_normal, gs_tangent));
	    gs_texCoord = vs_texCoord[i];
	    gl_Position = camera.projMatrix * camera.viewMatrix * gl_in[i].gl_Position;
	    EmitVertex();
	}
	EndPrimitive();
}
