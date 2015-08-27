package net.minecraftforge.client.model;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * DISCLAIMER: This class has been copied from Minecraftforge for 1.7.10 and will be removed
 *             once Forge for 1.8 adds support for .obj model files.
 */
public interface IModelCustom
{
    String getType();
    @SideOnly(Side.CLIENT)
    void renderAll();
    @SideOnly(Side.CLIENT)
    void renderOnly(String... groupNames);
    @SideOnly(Side.CLIENT)
    void renderPart(String partName);
    @SideOnly(Side.CLIENT)
    void renderAllExcept(String... excludedGroupNames);
}