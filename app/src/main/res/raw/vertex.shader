#version 300 es

in vec2 pos;
uniform mat4 transformMat;
out vec2 point;

void main() {
    point = pos.xy;
    gl_Position = transformMat * vec4(pos.xy, 0, 1);
}
