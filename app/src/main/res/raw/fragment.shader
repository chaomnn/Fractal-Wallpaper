#version 300 es

#define THRESHOLD 0.000001

precision highp float;

out vec4 outColor;
in vec2 point;
uniform mat4 zoomMat;
uniform vec4 baseColor;
uniform vec2 constNum;
uniform bool useLogColor;
uniform int limit;

vec4 getColor(float iter, vec4 base) {
    iter = useLogColor ? log2(iter) : iter * 0.2;
    return 0.4 + 0.5*cos(iter + base);
}

void main() {
    vec4 zn = vec4(point.xy, 0, 1);
    vec4 next = zn;
    zn = zoomMat * zn;
    vec4 c = vec4(constNum.x, constNum.y, 0, 0);
    int iter = 0;
    float lastDist = 0.0;
    float logT = log(THRESHOLD);

    while (iter <= limit) {

        float temp = zn.x;
        next.x = (zn.x - zn.y) * (zn.x + zn.y) + c.x;
        next.y = 2.0*temp*zn.y + c.y;
        vec4 diff = next - zn;
        zn = next;

        float sum = dot(zn, zn);
        float fIter = 0.0;

        if (sum > 16.0) {
            // outer
            fIter = float(iter) - log2(log2(float(sum))) + 4.0;
            outColor = getColor(fIter, baseColor);
            break;
        }
        float dist = dot(diff, diff);
        if (dist >= THRESHOLD) {
            lastDist = dist;
            if (iter == limit) {
                outColor = getColor(float(limit), baseColor);
                break;
            }
        } else {
            // inner
            fIter = (float(iter) + (logT - log(lastDist)) / (log(dist) - log(lastDist)));
            outColor = getColor(fIter, baseColor);
            break;
        }
        ++iter;
    }
}
