package net.minecraftforge.client.model.obj;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;

/**
 * DISCLAIMER: This class has been copied from Minecraftforge for 1.7.10 and will be removed
 *             once Forge for 1.8 adds support for .obj model files.
 */
public class GroupObject
{
    public String name;
    public ArrayList<Face> faces = new ArrayList<Face>();
    public int glDrawingMode;

    public GroupObject()
    {
        this("");
    }

    public GroupObject(String name)
    {
        this(name, -1);
    }

    public GroupObject(String name, int glDrawingMode)
    {
        this.name = name;
        this.glDrawingMode = glDrawingMode;
    }

    @SideOnly(Side.CLIENT)
    public void render()
    {
        if (faces.size() > 0)
        {
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();
            worldRenderer.startDrawing(glDrawingMode);
            render(worldRenderer);
            tessellator.draw();
        }
    }

    @SideOnly(Side.CLIENT)
    public void render(WorldRenderer worldRenderer)
    {
        if (faces.size() > 0)
        {
            for (Face face : faces)
            {
                face.addFaceForRender(worldRenderer);
            }
        }
    }
}