#version 300 es

#define LIMIT 500

precision highp float;

out vec4 outColor;
in vec2 point;
uniform mat4 zoomMat;
uniform vec4 baseColor;
uniform vec2 constNum;
uniform vec4 boundedColor;

vec4 getColorSin(float iter, vec4 base) {
    iter *= 0.2;
    return 0.4 + 0.5*cos(iter + base);
}

void main() {
    vec4 zn = vec4(point.xy, 0, 1);
    zn = zoomMat * zn;
    vec4 c = vec4(constNum.x, constNum.y, 0, 0);
    int iter = 0;

    while (iter <= LIMIT) {

        float temp = zn.x;
        zn.x = (zn.x - zn.y) * (zn.x + zn.y) + c.x;;
        zn.y = 2.0*temp*zn.y + c.y;

        float sum = dot(zn, zn);

        if (sum > 16.0) {
            float fIter = float(iter) - log2(log2(float(sum))) + 4.0;
            outColor = getColorSin(fIter, baseColor);
            break;
        } else if (iter == LIMIT) {
            outColor = boundedColor;
        }
        ++iter;
    }
}
