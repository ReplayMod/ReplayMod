package net.minecraftforge.client.model;

import net.minecraft.util.ResourceLocation;

/**
 * DISCLAIMER: This class has been copied from Minecraftforge for 1.7.10 and will be removed
 *             once Forge for 1.8 adds support for .obj model files.
 *
 * Instances of this class act as factories for their model type
 *
 * @author cpw
 *
 */
public interface IModelCustomLoader {
    /**
     * Get the main type name for this loader
     * @return the type name
     */
    String getType();
    /**
     * Get resource suffixes this model loader recognizes
     * @return a list of suffixes
     */
    String[] getSuffixes();
    /**
     * Load a model instance from the supplied path
     * @param resource The ResourceLocation of the model
     * @return A model instance
     * @throws ModelFormatException if the model format is not correct
     */
    IModelCustom loadInstance(ResourceLocation resource) throws ModelFormatException;
}