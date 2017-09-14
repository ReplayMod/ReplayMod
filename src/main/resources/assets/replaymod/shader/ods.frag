#version 110

varying vec4 vertColor;

varying vec4 textureCoord;
varying vec4 lightMapCoord;

uniform sampler2D texture;
uniform sampler2D lightMap;

uniform bool textureEnabled;
uniform bool lightMapEnabled;
uniform bool fogEnabled;

void main() {
    vec4 color = vertColor;
    if (textureEnabled) {
        color *= texture2D(texture, textureCoord.st);
    }
    if (lightMapEnabled) {
        color *= texture2D(lightMap, lightMapCoord.st);
    }
    if (fogEnabled) {
	    color.rgb = mix(color.rgb, gl_Fog.color.rgb, clamp((gl_FogFragCoord - gl_Fog.start) * gl_Fog.scale, 0.0, 1.0));
	}
    gl_FragColor = color;
}