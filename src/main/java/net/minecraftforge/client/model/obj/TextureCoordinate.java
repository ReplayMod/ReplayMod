package net.minecraftforge.client.model.obj;

/**
 * DISCLAIMER: This class has been copied from Minecraftforge for 1.7.10 and will be removed
 *             once Forge for 1.8 adds support for .obj model files.
 */
public class TextureCoordinate
{
    public float u, v, w;

    public TextureCoordinate(float u, float v)
    {
        this(u, v, 0F);
    }

    public TextureCoordinate(float u, float v, float w)
    {
        this.u = u;
        this.v = v;
        this.w = w;
    }
}