varying lowp vec4 color;
varying vec3 adjust;

void main() {
    adjust = ATTRIB_adjust;
    color = ATTRIB_realColor;
    gl_Position = vec4(ATTRIB_position.x + ATTRIB_offsetX/3.5, ATTRIB_position.y, 0.0, 1.0);
}
