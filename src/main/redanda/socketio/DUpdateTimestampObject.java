package main.redanda.socketio;

public class DUpdateTimestampObject {

    long timestamp;

    public DUpdateTimestampObject() {
    }

    public DUpdateTimestampObject(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
