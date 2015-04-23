package eu.crushedpixel.replaymod.api.client.holders;

public class UserFiles {

    private String user;
    private FileInfo[] files;
    private int total_size;

    public String getUser() {
        return user;
    }

    public FileInfo[] getFiles() {
        return files;
    }

    public int getTotal_size() {
        return total_size;
    }


}
