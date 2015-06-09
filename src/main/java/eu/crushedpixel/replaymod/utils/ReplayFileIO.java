package eu.crushedpixel.replaymod.utils;

import com.google.gson.Gson;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.holders.KeyframeSet;
import eu.crushedpixel.replaymod.holders.MarkerKeyframe;
import eu.crushedpixel.replaymod.holders.PacketData;
import eu.crushedpixel.replaymod.holders.PlayerVisibility;
import eu.crushedpixel.replaymod.recording.PacketSerializer;
import eu.crushedpixel.replaymod.recording.ReplayMetaData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("resource") //Gets handled by finalizer
public class ReplayFileIO {

    private static final PacketSerializer packetSerializer = new PacketSerializer(EnumPacketDirection.CLIENTBOUND);
    private static final byte[] uniqueBytes = new byte[]{0, 1, 1, 2, 3, 5, 8};
    private static File lastReplayFile = null;
    private static boolean lastContainsJoinPacket = false;

    public static File getRenderFolder() {
        File folder = new File(ReplayMod.replaySettings.getRenderPath());
        folder.mkdirs();
        return folder;
    }

    public static File getReplayFolder() {
        String path = ReplayMod.replaySettings.getRecordingPath();
        File folder = new File(path);
        folder.mkdirs();
        return folder;
    }

    public static List<File> getAllReplayFiles() {
        List<File> files = new ArrayList<File>();
        File folder = getReplayFolder();
        for(File file : folder.listFiles()) {
            if(("." + FilenameUtils.getExtension(file.getAbsolutePath())).equals(ReplayFile.ZIP_FILE_EXTENSION)) {
                files.add(file);
            }
        }
        return files;
    }

    public static void writeReplayFile(File replayFile, File tempFile, ReplayMetaData metaData, Set<MarkerKeyframe> markers,
                                       Map<String, File> resourcePacks, Map<Integer, String> resourcePackRequests) throws IOException {
        byte[] buffer = new byte[1024];

        if(!replayFile.exists()) {
            replayFile.createNewFile();
        }

        FileOutputStream fos = new FileOutputStream(replayFile);
        ZipOutputStream zos = new ZipOutputStream(fos);

        String json = new Gson().toJson(metaData);

        zos.putNextEntry(new ZipEntry(ReplayFile.ENTRY_METADATA));
        PrintWriter pw = new PrintWriter(zos);
        pw.write(json);
        pw.flush();
        zos.closeEntry();

        zos.putNextEntry(new ZipEntry(ReplayFile.ENTRY_RECORDING));
        FileInputStream fis = new FileInputStream(tempFile);
        int len;
        while((len = fis.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
        }

        fis.close();
        zos.closeEntry();

        if(!markers.isEmpty()) {
            zos.putNextEntry(new ZipEntry(ReplayFile.ENTRY_MARKERS));

            pw = new PrintWriter(zos);
            pw.write(new Gson().toJson(markers.toArray(new MarkerKeyframe[markers.size()])));
            pw.flush();
            zos.closeEntry();
        }

        if(!resourcePackRequests.isEmpty()) {
            zos.putNextEntry(new ZipEntry(ReplayFile.ENTRY_RESOURCE_PACK_INDEX));
            pw = new PrintWriter(zos);
            pw.write(new Gson().toJson(resourcePackRequests));
            pw.flush();
            zos.closeEntry();

            for(Map.Entry<String, File> entry : resourcePacks.entrySet()) {
                zos.putNextEntry(new ZipEntry(String.format(ReplayFile.ENTRY_RESOURCE_PACK, entry.getKey())));
                IOUtils.copy(new FileInputStream(entry.getValue()), zos);
                zos.closeEntry();
            }
        }

        zos.close();
    }

    public static ReplayMetaData getMetaData(File replayFile) throws IOException {
        ReplayFile file = new ReplayFile(replayFile);
        try {
            return file.metadata().get();
        } finally {
            file.close();
        }
    }

    /**
     * @param replayFile
     * @return Whether the given Replay File contains a {@link S01PacketJoinGame}.
     */
    public static boolean containsJoinPacket(File replayFile) {
        DataInputStream dis = null;
        if(replayFile == lastReplayFile) {
            return lastContainsJoinPacket;
        }
        ReplayFile file = null;

        lastReplayFile = replayFile;
        lastContainsJoinPacket = false;
        try {
            file = new ReplayFile(replayFile);
            dis = new DataInputStream(file.recording().get());
            PacketData pd = readPacketData(dis);
            while(dis.available() > 0) {
                Packet p = deserializePacket(pd.getByteArray());
                if(p instanceof S01PacketJoinGame) {
                    lastContainsJoinPacket = true;
                    return lastContainsJoinPacket;
                }
                if(p instanceof S08PacketPlayerPosLook) {
                    lastContainsJoinPacket = false;
                    return lastContainsJoinPacket;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(dis != null) {
                    dis.close();
                }
                if (file != null) {
                    file.close();
                }
            } catch(Exception ignored) {}
        }

        return false;
    }

    public static PacketData readPacketData(DataInputStream dis) throws IOException {
        int timestamp = dis.readInt();
        int bytes = dis.readInt();
        byte[] bb = new byte[bytes];
        dis.readFully(bb);

        return new PacketData(bb, timestamp);
    }

    public static Packet deserializePacket(byte[] bytes) throws InstantiationException, IllegalAccessException, IOException {
        try {
            ByteBuf bb = Unpooled.wrappedBuffer(bytes);
            PacketBuffer pb = new PacketBuffer(bb);

            int i = pb.readVarIntFromBuffer();

            Packet p = EnumConnectionState.PLAY.getPacket(EnumPacketDirection.CLIENTBOUND, i);
            p.readPacketData(pb);

            return p;
        } catch(Exception e) {
            return null;
        }
    }

    public static byte[] serializePacket(Packet packet) throws IOException {
        ByteBuf bb = Unpooled.buffer();
        packetSerializer.encode(EnumConnectionState.PLAY, packet, bb);
        bb.readerIndex(0);
        byte[] array = new byte[bb.readableBytes()];
        bb.readBytes(array);

        bb.readerIndex(0);

        return array;
    }

    public static void writePacket(PacketData pd, DataOutput out) throws IOException {
        out.writeInt(pd.getTimestamp());
        out.writeInt(pd.getByteArray().length);
        out.write(pd.getByteArray());
    }

    public static int getWrittenByteSize(PacketData pd) {
        return (2 * 4) + pd.getByteArray().length;
    }

    public static void writePackets(Collection<PacketData> p, DataOutput out) throws IOException {
        for(PacketData pd : p) {
            writePacket(pd, out);
        }
    }

    /**
     * @param replayFile     The File to reverse
     * @param outputFile     The File to save the reversed Packets in
     * @param seekJoinPacket Whether a {@link S01PacketJoinGame} should be seeked in the Replay File. If containsJoinPacket is being
     *                       called with the same File directly afterwards, this will reduce the amount of calculations. Only use if needed,
     *                       because this consumes a significant amount of time!
     * @param boundaries     Two timestamps which make a boundary for excluded Packets
     * @return Whether the action was successful
     */
    public static boolean reversePackets(File replayFile, File outputFile, boolean seekJoinPacket, int... boundaries) {
        lastReplayFile = replayFile;
        lastContainsJoinPacket = false;

        RandomAccessFile raf = null;
        DataInputStream dis = null;
        ReplayFile file = null;

        boolean bounds = false;
        int lower = 0, upper = 0;
        if(boundaries.length >= 2) {
            if(boundaries[0] > boundaries[1]) {
                upper = boundaries[0];
                lower = boundaries[1];
            } else {
                upper = boundaries[1];
                lower = boundaries[0];
            }
        }
        try {
            if(!outputFile.exists()) {
                outputFile.createNewFile();
            }
            raf = new RandomAccessFile(outputFile, "rw");
            file = new ReplayFile(replayFile);

            dis = new DataInputStream(file.recording().get());
            long fileLength = file.recordingEntry().getSize();

            raf.setLength(fileLength);

            long pointerBefore = fileLength;

            while(dis.available() > 0) {
                try {
                    PacketData pd = readPacketData(dis);

                    boolean write = true;
                    if(bounds) {
                        if(pd.getTimestamp() < lower || pd.getTimestamp() > upper) {
                            write = false;
                        }
                    }

                    if(write) {
                        if(seekJoinPacket && !lastContainsJoinPacket) {
                            Packet p = deserializePacket(pd.getByteArray());
                            if(p instanceof S01PacketJoinGame) lastContainsJoinPacket = true;
                        }
                        pointerBefore = pointerBefore - getWrittenByteSize(pd);
                        raf.seek(pointerBefore);
                        writePacket(pd, raf);
                    }
                } catch(EOFException e) {
                    e.printStackTrace();
                    break;
                }
            }
            return true;
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(raf != null) {
                    raf.close();
                }
                if(dis != null) {
                    dis.close();
                }
                if(file != null) {
                    file.close();
                }
            } catch(Exception ignored) {}
        }

        return false;
    }

    private static final Gson gson = new Gson();

    public static void writeKeyframeRegistryToFile(KeyframeSet[] keyframeRegistry, File file) throws IOException {
        file.mkdirs();
        file.createNewFile();

        String json = gson.toJson(keyframeRegistry);
        FileUtils.write(file, json);
    }

    public static void writePlayerVisibilityToFile(PlayerVisibility visibility, File file) throws IOException {
        file.mkdirs();
        file.createNewFile();

        String json = gson.toJson(visibility);
        FileUtils.write(file, json);
    }

    public static void writeReplayMetaDataToFile(ReplayMetaData metaData, File file) throws IOException {
        file.mkdirs();
        file.createNewFile();

        String json = gson.toJson(metaData);
        FileUtils.write(file, json);
    }

    public static void writeMarkersToFile(MarkerKeyframe[] markers, File file) throws IOException {
        file.mkdirs();
        file.createNewFile();

        String json = gson.toJson(markers);
        FileUtils.write(file, json);
    }

    /**
     * Adds Files as entries to a ZIP File.
     * @param zipFile The ZIP File to add the files to.
     * @param toAdd A Map containing the file's desired names as the keys and the files themselves as the values.
     *              Files may be null in order to remove entries with the respective name from the ZIP File.
     * @throws IOException
     */
    public static void addFilesToZip(File zipFile, HashMap<String, File> toAdd) throws IOException {
        // get a temp file
        File tempFile = File.createTempFile(zipFile.getName(), null, zipFile.getParentFile());
        // delete it, otherwise you cannot rename your existing zip to it.

        byte[] buf = new byte[1024];

        ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempFile));

        ZipEntry entry = zin.getNextEntry();

        while(entry != null) {
            String name = entry.getName();

            // Don't rewrite (keep) the old file if one of the new files uses the same name
            boolean willBeOverwritten = toAdd.containsKey(name);
            if(!willBeOverwritten) {
                out.putNextEntry(new ZipEntry(name));
                // Transfer bytes from the ZIP entry to the output entry
                int len;
                while((len = zin.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            entry = zin.getNextEntry();
        }
        // Close the streams
        zin.close();
        // Compress the files

        // Write all files to Replay File
        if(!toAdd.isEmpty()) {
            for(Map.Entry<String, File> e : toAdd.entrySet()) {
                String fileName = e.getKey();
                File toWrite = e.getValue();

                // The File may be null to simply be removed
                if(toWrite != null) {
                    InputStream in = new FileInputStream(toWrite);
                    // Add ZIP entry to output stream.
                    out.putNextEntry(new ZipEntry(fileName));

                    int len;
                    if(fileName.equals("thumb")) out.write(uniqueBytes);

                    while((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    // Complete the entry
                    out.closeEntry();
                    in.close();
                }
            }
        }

        // Complete the ZIP file
        out.close();

        zipFile.delete();
        tempFile.renameTo(zipFile);
    }
}
