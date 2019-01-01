package main.redanda.kademlia;

import kademlia.node.KademliaId;

public class KadContent {


    private KademliaId id; //we store the ID duplicated because of performance reasons (new lookup in the hashmap costs more than a bit of memory)
    private long timestamp; //created at (or updated)
    private byte[] content;


    public KadContent(KademliaId id, long timestamp, byte[] content) {
        this.id = id;
        this.timestamp = timestamp;
        this.content = content;
    }

    public KadContent(KademliaId id, byte[] content) {
        this.id = id;
        this.timestamp = System.currentTimeMillis();
        this.content = content;
    }

    public KademliaId getId() {
        return id;
    }

    public void setId(KademliaId id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
