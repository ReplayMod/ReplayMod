package com.replaymod.render.utils;

import com.google.common.collect.Maps;
import org.lwjgl.BufferUtils;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ByteBufferPool {
    private static Map<Integer, List<SoftReference<ByteBuffer>>> bufferPool = Maps.newHashMap();

    public static synchronized ByteBuffer allocate(int size) {
        List<SoftReference<ByteBuffer>> available = bufferPool.get(size);
        if (available != null) {
            Iterator<SoftReference<ByteBuffer>> iter = available.iterator();
            try {
                while (iter.hasNext()) {
                    SoftReference<ByteBuffer> reference = iter.next();
                    ByteBuffer buffer = reference.get();
                    iter.remove();
                    if (buffer != null) {
                        return buffer;
                    }
                }
            } finally {
                if (!iter.hasNext()) {
                    bufferPool.remove(size);
                }
            }
        }
        return BufferUtils.createByteBuffer(size);
    }

    public static synchronized void release(ByteBuffer buffer) {
        buffer.clear();
        int size = buffer.capacity();
        List<SoftReference<ByteBuffer>> available = bufferPool.get(size);
        if (available == null) {
            available = new LinkedList<>();
            bufferPool.put(size, available);
        }
        available.add(new SoftReference<>(buffer));
    }
}
