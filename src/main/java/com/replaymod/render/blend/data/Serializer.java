package com.replaymod.render.blend.data;

import com.replaymod.render.blend.Util;
import com.replaymod.render.blend.Util.IOBiConsumer;
import com.replaymod.render.blend.Util.IOCallable;
import com.replaymod.render.blend.Util.IOConsumer;
import org.blender.dna.ID;
import org.blender.dna.Link;
import org.blender.dna.ListBase;
import org.cakelab.blender.io.block.BlockCodes;
import org.cakelab.blender.nio.CArrayFacade;
import org.cakelab.blender.nio.CFacade;
import org.cakelab.blender.nio.CPointer;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.replaymod.render.blend.Util.align4;
import static com.replaymod.render.blend.Util.factory;
import static org.cakelab.blender.io.block.BlockCodes.ID_DATA;

public class Serializer {
    private final Map<Object, CPointer<Object>> serialized = new HashMap<>();
    private final Set<String> usedNames = new HashSet<>();

    public <T> CPointer<T> getMajor(Object obj, Class<T> clazz) {
        CPointer<Object> pointer = serialized.get(obj);
        if (pointer == null) {
            return null;
        }
        return pointer.cast(clazz);
    }

    public <T extends CFacade> CPointer<T> writeMajor(Object obj, DId id, Class<T> clazz) throws IOException {
        if (serialized.containsKey(obj)) {
            throw new IllegalStateException("Object " + obj + " already serialized.");
        }

        T val = factory().newCStructBlock(id.code, clazz);
        CPointer<T> pointer = CFacade.__io__addressof(val);

        serialized.put(obj, pointer.cast(Object.class));

        if (id.code != ID_DATA) {
            ID asID = pointer.cast(ID.class).get();
            String name = id.code.toString().substring(0, 2) + id.name;
            String fullName = name;
            int i = 0;
            while (usedNames.contains(fullName)) {
                fullName = name + "." + i;
                i++;
            }
            usedNames.add(fullName);
            asID.getName().fromString(fullName);
        }

        return pointer;
    }

    public <T extends CFacade> CPointer<T> maybeMajor(Object obj, DId id, Class<T> clazz, IOCallable<IOConsumer<T>> prepare) throws IOException {
        CPointer<T> result = getMajor(obj, clazz);
        if (result == null) {
            IOConsumer<T> configure = prepare.call();
            result = writeMajor(obj, id, clazz);
            configure.accept(result.get());
        }
        return result;
    }

    public CArrayFacade<Byte> writeString0(String str) throws IOException {
        byte[] bytes = (str + '\0').getBytes();
        CArrayFacade<Byte> pointer = factory().newCArrayBlock(BlockCodes.ID_DATA, Byte.class, align4(bytes.length));
        pointer.fromArray(bytes);
        return pointer;
    }

    public CPointer<Byte> writeBytes(byte[] bytes) throws IOException {
        CArrayFacade<Byte> pointer = factory().newCArrayBlock(BlockCodes.ID_DATA, Byte.class, align4(bytes.length));
        pointer.fromArray(bytes);
        return pointer;
    }

    public <T extends CFacade> T writeData(Class<T> clazz) throws IOException {
        return factory().newCStructBlock(ID_DATA, clazz);
    }

    public <T extends CFacade> CArrayFacade<T> writeData(Class<T> clazz, int count) throws IOException {
        return factory().newCStructBlock(ID_DATA, clazz, count);
    }

    public <T extends CFacade> CArrayFacade<T> writeData(Class<T> clazz, int count, IOBiConsumer<Integer, T> forElem) throws IOException {
        if (count == 0) return null;
        CArrayFacade<T> arrayFacade = writeData(clazz, count);
        CPointer<T> pointer = arrayFacade;
        for (int i = 0; i < count; i++) {
            forElem.accept(i, pointer.get());
            pointer = Util.plus(pointer, 1);
        }
        return arrayFacade;
    }

    public <T> CArrayFacade<CPointer<T>> writeDataPArray(Class<T> clazz, int count, Util.IOFunction<Integer, CPointer<T>> forElem) throws IOException {
        if (count == 0) return null;
        CArrayFacade<CPointer<T>> arrayFacade = factory().newCPointerBlock(ID_DATA, new Class[]{CPointer.class, clazz}, count);
        for (int i = 0; i < count; i++) {
            arrayFacade.set(i, forElem.apply(i));
        }
        return arrayFacade;
    }

    public <T extends CFacade> void writeDataList(Class<T> clazz, ListBase listBase, int size, IOBiConsumer<Integer, T> forElem) throws IOException {
        CPointer<Link> prevPointer = null;
        Link prev = null;
        for (int i = 0; i < size; i++) {
            CPointer<T> pointer = CFacade.__io__addressof(writeData(clazz));
            CPointer<Link> linkPointer = pointer.cast(Link.class);
            Link linkElem = linkPointer.get();
            if (prevPointer == null) {
                listBase.setFirst(pointer.cast(Object.class));
            } else {
                prev.setNext(linkPointer);
                linkElem.setPrev(prevPointer);
            }

            forElem.accept(i, pointer.get());

            prevPointer = linkPointer;
            prev = linkElem;
        }
        if (prevPointer != null) {
            listBase.setLast(prevPointer.cast(Object.class));
        }
    }
}
