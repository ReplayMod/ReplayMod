package com.replaymod.render.metadata;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.*;
import com.google.common.primitives.Bytes;
import com.googlecode.mp4parser.BasicContainer;
import net.minecraftforge.fml.common.FMLLog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MetadataInjector {

    private static final String STITCHING_SOFTWARE = "Minecraft ReplayMod";

    /*
     * The Spherical XML is a modified version of https://github.com/google/spatial-media/tree/master/360-Videos-Metadata
     */
    private static final String SPHERICAL_XML_HEADER =
            "<?xml version=\"1.0\"?> " +
            "<rdf:SphericalVideo xmlns:rdf=" +
            "\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:GSpherical=\"http://ns.google.com/videos/1.0/spherical/\"> ";
    private static final String SPHERICAL_XML_CONTENTS =
            "<GSpherical:Spherical>true</GSpherical:Spherical> " +
            "<GSpherical:Stitched>true</GSpherical:Stitched> " +
            "<GSpherical:StitchingSoftware>"+STITCHING_SOFTWARE+"</GSpherical:StitchingSoftware> " +
            "<GSpherical:ProjectionType>equirectangular</GSpherical:ProjectionType> ";
    private static final String SPHERICAL_XML_FOOTER = "</rdf:SphericalVideo>";

    private static final String XML_360_METADATA = SPHERICAL_XML_HEADER + SPHERICAL_XML_CONTENTS + SPHERICAL_XML_FOOTER;

    /**
     * These bytes are taken from the variable 'spherical_uuid_id'
     * in https://github.com/google/spatial-media/tree/master/360-Videos-Metadata
     */
    private static final byte[] UUID_BYTES = new byte[] {
            (byte)0xff, (byte)0xcc, (byte)0x82, (byte)0x63,
            (byte)0xf8, (byte)0x55, (byte)0x4a, (byte)0x93,
            (byte)0x88, (byte)0x14, (byte)0x58, (byte)0x7a,
            (byte)0x02, (byte)0x52, (byte)0x1f, (byte)0xdd
    };

    private static final byte[] BYTES_360_METADATA = Bytes.concat(UUID_BYTES, XML_360_METADATA.getBytes());

    public static void inject360Metadata(File videoFile) {
        writeMetadata(videoFile, BYTES_360_METADATA);
    }

    private static void writeMetadata(File videoFile, byte[] metadata) {
        File tempFile = null;
        FileOutputStream videoFileOutputStream = null;
        IsoFile tempIsoFile = null;

        try {
            tempFile = File.createTempFile("videoCopy", "mp4");
            FileUtils.copyFile(videoFile, tempFile);

            tempIsoFile = new IsoFile(tempFile.getAbsolutePath());

            //first, get the "moov" box from the mp4
            MovieBox moovBox = (MovieBox)getBoxByName(tempIsoFile, "moov");
            if(moovBox == null) throw new IOException("Could not find moov box inside IsoFile");

            //get the Movie Track to which we will add the Metadata
            TrackBox trackBox = (TrackBox)getBoxByName(moovBox, "trak");
            if(trackBox == null) throw new IOException("Could not find trak box inside moov box");

            //create a new UserBox, which actually contains the Metadata bytes
            UserBox metadataBox = new UserBox(new byte[0]);
            metadataBox.setData(metadata);

            //add the Metadata UserBox to the Movie Track
            trackBox.addBox(metadataBox);

            //finally, reduce the Video's FreeBox, whose size we will need
            //to reduce by the Metadata Box's size to preserve the Video's File size
            FreeBox freeBox = (FreeBox)getBoxByName(tempIsoFile, "free");
            if(freeBox == null) throw new IOException("Could not find free box inside IsoFile");

            int freeSize = Math.max(0, freeBox.getData().capacity() - (int)metadataBox.getSize());
            freeBox.setData(ByteBuffer.allocate(freeSize));

            //save the ISO file to disk
            videoFileOutputStream = new FileOutputStream(videoFile);
            tempIsoFile.getBox(videoFileOutputStream.getChannel());
        } catch(Exception e) {
            FMLLog.getLogger().error("360 Degree Metadata couldn't be injected", e);
        } finally {
            IOUtils.closeQuietly(tempIsoFile);
            IOUtils.closeQuietly(videoFileOutputStream);
            FileUtils.deleteQuietly(tempFile);
        }
    }

    private static Box getBoxByName(BasicContainer container, String boxName) {
        for (Box box : container.getBoxes()) {
            if(box.getType().equals(boxName)) return box;
        }
        return null;
    }

}