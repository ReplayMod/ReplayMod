package com.replaymod.render.blend;

import java.io.IOException;

public interface Exporter {
    default void setup() throws IOException {}
    default void tearDown() throws IOException {}
    default void preFrame(int frame) throws IOException {}
    default void postFrame(int frame) throws IOException {}
}
