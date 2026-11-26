// === VERTEX ===
uniform mat4 uVPMatrix;
uniform mat4 uModelMatrix;
uniform float uOutlineWidth;

attribute vec3 aPosition;
attribute vec3 aSmoothNormal;

void main() {
    vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
    vec3 worldNormal = normalize(mat3(uModelMatrix) * aSmoothNormal);
    vec4 clipPos = uVPMatrix * worldPos;
    float distScale = clamp(clipPos.w * 0.14, 0.55, 2.25);
    vec4 extruded = vec4(worldPos.xyz + worldNormal * (uOutlineWidth * distScale), 1.0);
    gl_Position = uVPMatrix * extruded;
}

// === FRAGMENT ===
precision mediump float;

uniform vec4 uOutlineColor;

void main() {
    gl_FragColor = uOutlineColor;
}
