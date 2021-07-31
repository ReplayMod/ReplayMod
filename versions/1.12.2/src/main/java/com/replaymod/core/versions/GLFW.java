package com.replaymod.core.versions;

import org.lwjgl.opengl.Display;

public class GLFW {
    public static boolean glfwWindowShouldClose(@SuppressWarnings("unused") long handle) {
        return Display.isCloseRequested();
    }
}
