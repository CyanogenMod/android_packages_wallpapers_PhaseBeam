varying float pointSize;
varying vec3 adjust;

// inspired by http://www.chilliant.com/rgb2hsv.html
vec3 hsl2rgb(vec3 hsl)
{
    // each line has the form abs(6 * hue - K1) * K2 + C
    const vec3 K1 = vec3(0.5, 1.0 / 3.0, 2.0 / 3.0);
    const vec3 K2 = vec3(1.0, -1.0, -1.0);
    const vec3 C = vec3(-1.0, 2.0, 2.0);

    vec3 rgb = clamp(abs(6.0 * (hsl.xxx - K1.xyz)) * K2 + C, 0.0, 1.0);
    float chroma = (1.0 - abs(2.0 * hsl.z - 1.0)) * hsl.y;
    return (rgb - 0.5) * chroma + hsl.z;
}

void main() {
    vec3 rgb = texture2D(UNI_Tex0, gl_PointCoord).rgb;

    if (adjust.x >= 0.0) {
        // rgb is already greyscale in that case, so r = g = b
        vec3 hsl = adjust * vec3(1.0, 1.0, rgb.r);
        rgb = hsl2rgb(hsl);
    }

    // output pixel color
    gl_FragColor = vec4(rgb, pointSize);
}
