package io.mosn.coder.intellij.option;

public class Source {

    private String name;

    private String path;

    private String content;

    private byte[] rawBytes;

    public Source(String name, String path, String content) {
        this.name = name;
        this.path = path;
        this.content = content;
    }

    public Source(String name, String path, byte[] rawBytes) {
        this.name = name;
        this.path = path;
        this.rawBytes = rawBytes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public byte[] getRawBytes() {
        return rawBytes;
    }

    public void setRawBytes(byte[] rawBytes) {
        this.rawBytes = rawBytes;
    }
}
