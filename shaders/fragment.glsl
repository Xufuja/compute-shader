#version 460 core

layout(location = 0) out vec4 o_Color;

in vec2 v_TexCoord;

void main()
{
    o_Color = vec4(v_TexCoord, 0, 1);
}