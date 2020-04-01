#version 110

varying vec4 vertColor;

varying vec4 textureCoord;
varying vec4 lightMapCoord;
//#if MC>=11500
//$$ varying vec4 overlayCoords;
//#endif

uniform sampler2D texture;
uniform sampler2D lightMap;
//#if MC>=11500
//$$ uniform sampler2D overlay;
//#endif

uniform bool textureEnabled;
uniform bool lightMapEnabled;
uniform bool overlayEnabled;
uniform bool fogEnabled;

void main() {
    vec4 color = vertColor;
    if (textureEnabled) {
        color *= texture2D(texture, textureCoord.st);
    }
    if (overlayEnabled) {
        //#if MC>=11500
        //$$ vec4 c = texture2D(overlay, overlayCoords.st);
        //$$ color = vec4(mix(c.rgb, color.rgb, c.a), color.a);
        //#else
        color = vec4(mix(color.rgb, vec3(1, 0, 0), 0.3), color.a);
        //#endif
    }
    if (lightMapEnabled) {
        color *= texture2D(lightMap, lightMapCoord.st);
    }
    if (fogEnabled) {
	    color.rgb = mix(color.rgb, gl_Fog.color.rgb, clamp((gl_FogFragCoord - gl_Fog.start) * gl_Fog.scale, 0.0, 1.0));
	}
    gl_FragColor = color;
}