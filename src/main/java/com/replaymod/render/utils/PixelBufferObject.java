package com.replaymod.render.utils;

import com.google.common.base.Objects;
import com.replaymod.core.ReplayMod;
import org.apache.logging.log4j.LogManager;

//#if MC>=11400
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL;
//#else
//$$ import org.lwjgl.opengl.ARBBufferObject;
//$$ import org.lwjgl.opengl.GLContext;
//$$ import org.lwjgl.opengl.Util;
//#endif

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.ARBPixelBufferObject.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER;

//#if MC>=11400
import static org.lwjgl.opengl.ARBVertexBufferObject.*;
//#endif

public class PixelBufferObject {
    public enum Usage {
        COPY(GL_STREAM_COPY_ARB, GL_STREAM_COPY),
        DRAW(GL_STREAM_DRAW_ARB, GL_STREAM_DRAW),
        READ(GL_STREAM_READ_ARB, GL_STREAM_READ);

        private final int arb, gl15;

        Usage(int arb, int gl15) {
            this.arb = arb;
            this.gl15 = gl15;
        }
    }

    //#if MC>=11400
    public static final boolean SUPPORTED = GL.getCapabilities().GL_ARB_pixel_buffer_object || GL.getCapabilities().OpenGL15;
    private static final boolean arb = !GL.getCapabilities().OpenGL15;
    //#else
    //$$ public static final boolean SUPPORTED = GLContext.getCapabilities().GL_ARB_pixel_buffer_object || GLContext.getCapabilities().OpenGL15;
    //$$ private static final boolean arb = !GLContext.getCapabilities().OpenGL15;
    //#endif

    private static ThreadLocal<Integer> bound = new ThreadLocal<>();
    private static ThreadLocal<Integer> mapped = new ThreadLocal<>();

    private final long size;
    private long handle;

    public PixelBufferObject(long size, Usage usage) {
        if (!SUPPORTED) {
            throw new UnsupportedOperationException("PBOs not supported.");
        }

        this.size = size;
        //#if MC>=11400
        this.handle = arb ? ARBVertexBufferObject.glGenBuffersARB() : glGenBuffers();
        //#else
        //$$ this.handle = arb ? ARBBufferObject.glGenBuffersARB() : glGenBuffers();
        //#endif

        bind();

        if (arb) {
            //#if MC>=11400
            ARBVertexBufferObject.glBufferDataARB(GL_PIXEL_PACK_BUFFER_ARB, size, usage.arb);
            //#else
            //$$ ARBBufferObject.glBufferDataARB(GL_PIXEL_PACK_BUFFER_ARB, size, usage.arb);
            //#endif
        } else {
            glBufferData(GL_PIXEL_PACK_BUFFER, size, usage.gl15);
        }

        unbind();
    }

    private int getHandle() {
        if (handle == -1) {
            throw new IllegalStateException("PBO not allocated.");
        }
        return (int) handle;
    }

    public void bind() {
        if (arb) {
            //#if MC>=11400
            ARBVertexBufferObject.glBindBufferARB(GL_PIXEL_PACK_BUFFER_ARB, getHandle());
            //#else
            //$$ ARBBufferObject.glBindBufferARB(GL_PIXEL_PACK_BUFFER_ARB, getHandle());
            //#endif
        } else {
            glBindBuffer(GL_PIXEL_PACK_BUFFER, getHandle());
        }
        bound.set(getHandle());
    }

    public void unbind() {
        checkBound();
        if (arb) {
            //#if MC>=11400
            ARBVertexBufferObject.glBindBufferARB(GL_PIXEL_PACK_BUFFER_ARB, 0);
            //#else
            //$$ ARBBufferObject.glBindBufferARB(GL_PIXEL_PACK_BUFFER_ARB, 0);
            //#endif
        } else {
            glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
        }
        bound.set(0);
    }

    private void checkBound() {
        if (!Objects.equal(getHandle(), bound.get())) {
            throw new IllegalStateException("Buffer not bound.");
        }
    }

    private void checkNotMapped() {
        if (Objects.equal(getHandle(), mapped.get())) {
            throw new IllegalStateException("Buffer already mapped.");
        }
    }

    public ByteBuffer mapReadOnly() {
        checkBound();
        checkNotMapped();
        ByteBuffer buffer;
        if (arb) {
            //#if MC>=11400
            buffer = ARBVertexBufferObject.glMapBufferARB(GL_PIXEL_PACK_BUFFER_ARB, GL_READ_ONLY_ARB, size, null);
            //#else
            //$$ buffer = ARBBufferObject.glMapBufferARB(GL_PIXEL_PACK_BUFFER_ARB, GL_READ_ONLY_ARB, size, null);
            //#endif
        } else {
            buffer = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_ONLY, size, null);
        }
        //#if MC<11400
        //$$ if (buffer == null) {
        //$$     Util.checkGLError();
        //$$ }
        //#endif
        mapped.set(getHandle());
        return buffer;
    }

    public ByteBuffer mapWriteOnly() {
        checkBound();
        checkNotMapped();
        ByteBuffer buffer;
        if (arb) {
            //#if MC>=11400
            buffer = ARBVertexBufferObject.glMapBufferARB(GL_PIXEL_PACK_BUFFER_ARB, GL_WRITE_ONLY_ARB, size, null);
            //#else
            //$$ buffer = ARBBufferObject.glMapBufferARB(GL_PIXEL_PACK_BUFFER_ARB, GL_WRITE_ONLY_ARB, size, null);
            //#endif
        } else {
            buffer = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_WRITE_ONLY, size, null);
        }
        //#if MC<11400
        //$$ if (buffer == null) {
        //$$     Util.checkGLError();
        //$$ }
        //#endif
        mapped.set(getHandle());
        return buffer;
    }

    public ByteBuffer mapReadWrite() {
        checkBound();
        checkNotMapped();
        ByteBuffer buffer;
        if (arb) {
            //#if MC>=11400
            buffer = ARBVertexBufferObject.glMapBufferARB(GL_PIXEL_PACK_BUFFER_ARB, GL_READ_WRITE_ARB, size, null);
            //#else
            //$$ buffer = ARBBufferObject.glMapBufferARB(GL_PIXEL_PACK_BUFFER_ARB, GL_READ_WRITE_ARB, size, null);
            //#endif
        } else {
            buffer = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_WRITE, size, null);
        }
        //#if MC<11400
        //$$ if (buffer == null) {
        //$$     Util.checkGLError();
        //$$ }
        //#endif
        mapped.set(getHandle());
        return buffer;
    }

    public void unmap() {
        checkBound();
        if (!Objects.equal(mapped.get(), getHandle())) {
            throw new IllegalStateException("Buffer not mapped.");
        }
        if (arb) {
            //#if MC>=11400
            ARBVertexBufferObject.glUnmapBufferARB(GL_PIXEL_PACK_BUFFER_ARB);
            //#else
            //$$ ARBBufferObject.glUnmapBufferARB(GL_PIXEL_PACK_BUFFER_ARB);
            //#endif
        } else {
            glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
        }
        mapped.set(0);
    }

    public void delete() {
        if (handle != -1) {
            if (arb) {
                //#if MC>=11400
                ARBVertexBufferObject.glDeleteBuffersARB(getHandle());
                //#else
                //$$ ARBBufferObject.glDeleteBuffersARB(getHandle());
                //#endif
            } else {
                glDeleteBuffers(getHandle());
            }
            handle = -1;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (handle != -1) {
            LogManager.getLogger().warn("PBO garbage collected before deleted!");
            ReplayMod.instance.runLater(this::delete);
        }
    }
}
