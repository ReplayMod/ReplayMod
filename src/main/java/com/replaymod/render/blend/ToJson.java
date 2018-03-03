package com.replaymod.render.blend;

import org.blender.utils.MainLib;
import org.cakelab.blender.generator.typemap.Renaming;
import org.cakelab.blender.io.BlenderFile;
import org.cakelab.blender.io.FileVersionInfo;
import org.cakelab.blender.io.block.Block;
import org.cakelab.blender.io.block.BlockCodes;
import org.cakelab.blender.io.block.BlockTable;
import org.cakelab.blender.metac.CMetaModel;
import org.cakelab.blender.metac.CStruct;
import org.cakelab.blender.nio.CArrayFacade;
import org.cakelab.blender.nio.CFacade;
import org.cakelab.blender.nio.CPointer;
import org.cakelab.json.JSONArray;
import org.cakelab.json.JSONObject;
import org.cakelab.json.codec.JSONCodec;
import org.cakelab.json.codec.JSONCodecConfiguration;
import org.cakelab.json.codec.JSONCodecException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ToJson {

    private static final String PACKAGE = "org.blender.dna";
    private static JSONCodec codec;
    private static CMetaModel model;
    private static BlenderFile blend;
    private static BlockTable blockTable;

    public static void main(String[] args) throws IOException, JSONCodecException {
        File fBlend = new File("/home/user/1.blend");
        //File fBlend = new File("test.blend");
        blend = new BlenderFile(fBlend);

        FileVersionInfo versions = blend.readFileGlobal();
        if (!MainLib.doVersionCheck(versions)) {
            System.err.println("Warning: Conversion will probably fail due to version mismatch!");
        }

        blockTable = blend.getBlockTable();
        model = blend.getMetaModel();
        codec = new JSONCodec(new JSONCodecConfiguration(false));

        JSONObject json = new JSONObject();
        JSONObject oHeader = createHeader(blend);
        json.put("header", oHeader);
        JSONArray aBlocks = new JSONArray();
        json.put("blocks", aBlocks);
        for (Block b : blend.getBlocks()) {
            addBlock(aBlocks, b);
        }
        blend.close();

        File fJsonOut = new File(fBlend.getParentFile(), fBlend.getName().replace(".blend", ".json"));
        FileOutputStream fout = new FileOutputStream(fJsonOut);
        codec.encodeObject(json, fout);
        fout.close();
        System.out.println("Finished");
    }

    private static JSONObject createHeader(BlenderFile blend) {
        JSONObject oHeader = new JSONObject();
        oHeader.put("MAGIC", "BLENDER");
        oHeader.put("addressWidth", blend.getEncoding().getAddressWidth());
        oHeader.put("endianess", blend.getEncoding().getByteOrder().toString());
        oHeader.put("version", blend.getVersion().toString());
        return oHeader;
    }

    private static void addBlock(JSONArray aBlocks, Block b) throws JSONCodecException, IOException {
        JSONObject oBlock = new JSONObject();
        aBlocks.add(oBlock);
        JSONObject header = (JSONObject) codec.encodeObjectJSON(b.header);
        oBlock.put("header", header);
        header.put("code", b.header.getCode().toString());
        CStruct struct = model.getStruct(b.header.getSdnaIndex());
        header.put("sdnaIndex", struct.getSignature() + "(" + b.header.getSdnaIndex() + ")");

        System.out.print("[" + b.header.getAddress() + ", " + (b.header.getAddress() + b.header.getSize()) + "] ");
        if (b.header.getCode().equals(BlockCodes.ID_DATA) && b.header.getSdnaIndex() == 0) {
            System.out.println(b.header.getCode().toString() + ": Link(0) or undef");
            byte[] buffer = new byte[b.header.getSize()];
            b.data.readFully(buffer);
            String str = toHexStr(buffer);
            oBlock.put("raw", str);
            int requiredSize = b.header.getCount() * struct.sizeof(blend.getEncoding().getAddressWidth());
            if (requiredSize == b.header.getSize()) {
                addStruct(oBlock, b, struct);
            }
        } else {
            System.out.println(b.header.getCode().toString() + ": " + struct.getSignature());
            addStruct(oBlock, b, struct);
        }
    }

    private static String toHexStr(byte[] buffer) {
        StringBuffer str = new StringBuffer();
        for (byte v : buffer) {
            String s = Integer.toHexString(v&0xff);
            if (s.length() == 1) s = "0"+s;
            str.append(s);
        }

        return str.toString();
    }

    private static void addStruct(JSONObject oBlock, Block b, CStruct struct) {
        if (b.header.getCount() == 1) {
            oBlock.put(struct.getSignature(), createStruct(b, 0, struct));
        } else {
            JSONArray aStructs = new JSONArray();
            oBlock.put(struct.getSignature() + "_array", aStructs);
            for (int i = 0; i < b.header.getCount(); i++) {
                aStructs.add(createStruct(b, i, struct));
            }
        }
    }

    private static Object createStruct(Block b, int index, CStruct struct) {
        JSONObject oStruct = null;

        try {
            Class<?> cStruct = Class.forName(PACKAGE + "." + Renaming.mapStruct2Class(struct.getSignature()));
            long address = b.header.getAddress() + index * struct.sizeof(blend.getEncoding().getAddressWidth());
            Constructor<?> constructor = cStruct.getDeclaredConstructor(long.class, Block.class, BlockTable.class);
            Object object = constructor.newInstance(address, b, blockTable);

            oStruct = (JSONObject) toJson(cStruct, object);

        } catch (Throwable e) {
            oStruct = new JSONObject();
            e.printStackTrace();
            oStruct.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        return oStruct;
    }

    private static Object toJson(Class<?> type, Object value) throws Throwable {
        try {
            assert(value != null);
            if (isPrimitive(type)) {
                return codec.encodeObjectJSON(value);
            } else if (value instanceof CArrayFacade){
                CArrayFacade<?> carray = ((CArrayFacade<?>)value);
                JSONArray array = new JSONArray();
                boolean hasString = false;
                for (int i = 0; i < carray.length(); i++) {
                    Object elem = carray.get(i);
                    if (elem instanceof Byte) {
                        hasString = true;
                    }
                    array.add(toJson(elem.getClass(), elem));
                }
                if (hasString) {
                    JSONObject debugArray = new JSONObject();
                    debugArray.put("str", carray.asString());
                    debugArray.put("data", array);
                    return debugArray;
                } else {
                    return array;
                }
            } else if (value instanceof CPointer) {
                return ((CPointer<?>)value).getAddress();
            } else if (value instanceof CFacade) {
                JSONObject oStruct = new JSONObject();
                try {
                    for (Method getter : type.getDeclaredMethods()) {
                        if (getter.getName().startsWith("get")) {
                            Object result = getter.invoke(value);
                            Class<?> rType = getter.getReturnType();
                            oStruct.put(getter.getName(), toJson(rType, result));
                        }
                    }
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof NullPointerException) {
                        throw (NullPointerException)cause;
                    } else {
                        throw e;
                    }
                }
                return oStruct;
            } else {
                return codec.encodeObjectJSON(value);
            }
        } catch (NullPointerException e) {
            // caught when data from an address, that was assumed to
            // be stored in the file, was not found. This happens in two cases:
            // 1. The sdna index stored in the block header is not correct. In many
            // cases index 0 (Link) is used for raw data.
            // 2. A pointer references non-persistent data which was not stored in the file.
            JSONObject o = new JSONObject();
            o.put("error", "data not found");
            return o;
        }
    }

    private static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive() || type.equals(String.class);
    }

}
