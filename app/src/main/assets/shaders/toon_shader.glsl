// === VERTEX ===
uniform mat4 uMVPMatrix;
uniform mat4 uModelMatrix;

attribute vec3 aPosition;
attribute vec3 aNormal;
attribute vec2 aTexCoord;
attribute vec3 aVertexColor;

varying vec3 vWorldPos;
varying vec3 vNormal;
varying vec2 vUv;
varying vec3 vCol;

void main() {
    vec4 wp = uModelMatrix * vec4(aPosition, 1.0);
    vWorldPos = wp.xyz;
    vNormal = normalize(mat3(uModelMatrix) * aNormal);
    vUv = aTexCoord;
    vCol = aVertexColor;
    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
}

// === FRAGMENT ===
precision mediump float;

uniform sampler2D uTexture;
uniform vec4 uBaseColor;
uniform vec4 uShadeTint;
uniform vec3 uSunDir;
uniform vec3 uCameraPos;
uniform vec3 uRimColor;
uniform vec3 uFogColor;
uniform float uRimStrength;
uniform float uUseTexture;
uniform float uAlphaCutoff;
uniform float uEmissive;
uniform float uUnlit;

varying vec3 vWorldPos;
varying vec3 vNormal;
varying vec2 vUv;
varying vec3 vCol;

void main() {
    vec4 tex = mix(vec4(1.0), texture2D(uTexture, vUv), uUseTexture);
    if (uAlphaCutoff > 0.0 && tex.a < uAlphaCutoff) discard;

    vec3 N = normalize(vNormal);
    vec3 L = normalize(uSunDir);
    vec3 V = normalize(uCameraPos - vWorldPos);
    vec3 albedo = tex.rgb * uBaseColor.rgb * vCol;

    float ndotl = dot(N, L);
    float band = smoothstep(0.02, 0.11, ndotl);
    vec3 lit = mix(albedo * uShadeTint.rgb, albedo * 1.09, band);
    float rim = pow(1.0 - clamp(dot(N, V), 0.0, 1.0), 3.0) * smoothstep(0.0, 0.42, ndotl + 0.35);

    vec3 shaded = mix(lit + uRimColor * rim * uRimStrength, albedo * 1.05, uUnlit);
    shaded += albedo * uEmissive;

    float dist = length(uCameraPos - vWorldPos);
    float fog = clamp((dist - 24.0) / 56.0, 0.0, 0.74);
    vec3 finalRgb = mix(shaded, uFogColor, fog * (1.0 - uUnlit * 0.45));
    gl_FragColor = vec4(finalRgb, uBaseColor.a * tex.a);
}
