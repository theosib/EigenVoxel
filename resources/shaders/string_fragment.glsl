#version 330 core

out vec4 FragColor;
in vec4 fgColorFrag;
in vec2 TexCoord;

uniform sampler2D ourTexture;

void main()
{
    vec4 tex = texture(ourTexture, TexCoord);
    float luma = tex.r;
    FragColor.a = luma;
    FragColor.rgb = fgColorFrag.rgb * luma;
    if (FragColor.a <= 0.0) discard;
}
