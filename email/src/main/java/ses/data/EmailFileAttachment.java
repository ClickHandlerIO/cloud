package ses.data;

public class EmailFileAttachment {
    private String name;
    private String mimeType;
    private byte[] file;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

    public EmailFileAttachment() {
    }

    public EmailFileAttachment(String name, String mimeType, byte[] file) {
        this.name = name;
        this.mimeType = mimeType;
        this.file = file;
    }
}
