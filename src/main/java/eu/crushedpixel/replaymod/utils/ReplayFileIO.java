package eu.crushedpixel.replaymod.utils;

import com.google.gson.Gson;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.assets.CustomObjectRepository;
import eu.crushedpixel.replaymod.holders.*;
import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import eu.crushedpixel.replaymod.recording.PacketSerializer;
import eu.crushedpixel.replaymod.recording.ReplayMetaData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
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

    public static File getRenderFolder() throws IOException {
        return makeFolderFromPath(ReplayMod.replaySettings.getRenderPath());
    }

    public static File getReplayFolder() throws IOException {
        return makeFolderFromPath(ReplayMod.replaySettings.getRecordingPath());
    }

    public static File getReplayDownloadFolder() throws IOException {
        return makeFolderFromPath(ReplayMod.replaySettings.getDownloadPath());
    }

    private static File makeFolderFromPath(String path) throws IOException {
        File folder = new File(path);
        FileUtils.forceMkdir(folder);
        return folder;
    }

    public static File getNextFreeFile(File file) {
        if(!file.exists()) return file;
        File folder = file.getParentFile();
        String filename = FilenameUtils.getBaseName(file.getAbsolutePath());
        String extension = FilenameUtils.getExtension(file.getAbsolutePath());

        int i = 1;
        while(file.exists()) {
            file = new File(folder, filename+"_"+i+"."+extension);
            i++;
        }

        return file;
    }

    public static List<File> getAllReplayFiles() {
        List<File> files = new ArrayList<File>();
        try {
            files.addAll(FileUtils.listFiles(getReplayFolder(), new String[]{"mcpr"}, false));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }

    public static void writeReplayFile(File replayFile, File tempFile, ReplayMetaData metaData, Set<Keyframe<Marker>> markers,
                                       Map<String, File> resourcePacks, Map<Integer, String> resourcePackRequests) throws IOException {
        byte[] buffer = new byte[1024];

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
            pw.write(new Gson().toJson(markers.toArray(new Keyframe[markers.size()])));
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

    private static final Gson gson = new Gson();

    private static void write(Object obj, File file) throws IOException {
        String json = gson.toJson(obj);
        FileUtils.write(file, json);
    }

    public static void write(KeyframeSet[] keyframeRegistry, File file) throws IOException {
        write((Object) keyframeRegistry, file);
    }

    public static void write(PlayerVisibility visibility, File file) throws IOException {
        write((Object) visibility, file);
    }

    public static void write(ReplayMetaData metaData, File file) throws IOException {
        write((Object) metaData, file);
    }

    public static void write(KeyframeList<Marker> markers, File file) throws IOException {
        write((Object) markers, file);
    }

    public static void write(CustomObjectRepository repository, File file) throws IOException {
        write((Object) repository, file);
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

        FileUtils.forceDelete(zipFile);
        FileUtils.moveFile(tempFile, zipFile);
    }
}
