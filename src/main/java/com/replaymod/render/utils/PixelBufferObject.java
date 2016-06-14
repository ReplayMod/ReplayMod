package com.replaymod.render.utils;

import com.google.common.base.Objects;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Util;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.ARBPixelBufferObject.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER;

public class PixelBufferObject {
    @RequiredArgsConstructor
    public enum Usage {
        COPY(GL_STREAM_COPY_ARB, GL_STREAM_COPY),
        DRAW(GL_STREAM_DRAW_ARB, GL_STREAM_DRAW),
        READ(GL_STREAM_READ_ARB, GL_STREAM_READ);

        private final int arb, gl15;
    }

    public static final boolean SUPPORTED = GLContext.getCapabilities().GL_ARB_pixel_buffer_object || GLContext.getCapabilities().OpenGL15;
    private static final boolean arb = !GLContext.getCapabilities().OpenGL15;

    private static ThreadLocal<Integer> bound = new ThreadLocal<>();
    private static ThreadLocal<Integer> mapped = new ThreadLocal<>();

    private final long size;
    private long handle;

    public PixelBufferObject(long size, Usage usage) {
        if (!SUPPORTED) {
            throw new UnsupportedOperationException("PBOs not supported.");
        }

        this.size = size;
        this.handle = arb ? ARBBufferObject.glGenBuffersARB() : glGenBuffers();

        bind();

        if (arb) {
            ARBBufferObject.glBufferDataARB(GL_PIXEL_PACK_BUFFER_ARB, size, usage.arb);
        } else {
            glBufferData(GL_PIXEL_PACK_BUFFER, size, usage.gl15);
        }
    }

    private int getHandle() {
        if (handle == -1) {
            throw new IllegalStateException("PBO not allocated.");
        }
        return (int) handle;
    }

    public void bind() {
        if (arb) {
            ARBBufferObject.glBindBufferARB(GL_PIXEL_PACK_BUFFER_ARB, getHandle());
        } else {
            glBindBuffer(GL_PIXEL_PACK_BUFFER, getHandle());
        }
        bound.set(getHandle());
    }

    public void unbind() {
        checkBound();
        if (arb) {
            ARBBufferObject.glBindBufferARB(GL_PIXEL_PACK_BUFFER_ARB, 0);
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
            buffer = ARBBufferObject.glMapBufferARB(GL_PIXEL_PACK_BUFFER_ARB, GL_READ_ONLY_ARB, size, null);
        } else {
            buffer = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_ONLY, size, null);
        }
        if (buffer == null) {
            Util.checkGLError();
        }
        mapped.set(getHandle());
        return buffer;
    }

    public ByteBuffer mapWriteOnly() {
        checkBound();
        checkNotMapped();
        ByteBuffer buffer;
        if (arb) {
            buffer = ARBBufferObject.glMapBufferARB(GL_PIXEL_PACK_BUFFER_ARB, GL_WRITE_ONLY_ARB, size, null);
        } else {
            buffer = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_WRITE_ONLY, size, null);
        }
        if (buffer == null) {
            Util.checkGLError();
        }
        mapped.set(getHandle());
        return buffer;
    }

    public ByteBuffer mapReadWrite() {
        checkBound();
        checkNotMapped();
        ByteBuffer buffer;
        if (arb) {
            buffer = ARBBufferObject.glMapBufferARB(GL_PIXEL_PACK_BUFFER_ARB, GL_READ_WRITE_ARB, size, null);
        } else {
            buffer = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_WRITE, size, null);
        }
        if (buffer == null) {
            Util.checkGLError();
        }
        mapped.set(getHandle());
        return buffer;
    }

    public void unmap() {
        checkBound();
        if (!Objects.equal(mapped.get(), getHandle())) {
            throw new IllegalStateException("Buffer not mapped.");
        }
        if (arb) {
            ARBBufferObject.glUnmapBufferARB(GL_PIXEL_PACK_BUFFER_ARB);
        } else {
            glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
        }
        mapped.set(0);
    }

    public void delete() {
        if (handle != -1) {
            if (arb) {
                ARBBufferObject.glDeleteBuffersARB(getHandle());
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
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    delete();
                }
            });
        }
    }
}
