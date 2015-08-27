package net.minecraftforge.client.model;

/**
 * DISCLAIMER: This class has been copied from Minecraftforge for 1.7.10 and will be removed
 *             once Forge for 1.8 adds support for .obj model files.
 *
 * Thrown if there is a problem parsing the model
 *
 * @author cpw
 *
 */
public class ModelFormatException extends RuntimeException {

    private static final long serialVersionUID = 2023547503969671835L;

    public ModelFormatException()
    {
        super();
    }

    public ModelFormatException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ModelFormatException(String message)
    {
        super(message);
    }

    public ModelFormatException(Throwable cause)
    {
        super(cause);
    }

}