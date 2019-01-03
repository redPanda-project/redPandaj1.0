package main.redanda.socketio;

public class DUpdateObject {

    private byte[] data;
    private byte[] signature;
    private long timestamp;

    public DUpdateObject() {
    }

    public DUpdateObject(byte[] data, byte[] signature, long timestamp) {
        this.data = data;
        this.signature = signature;
        this.timestamp = timestamp;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
