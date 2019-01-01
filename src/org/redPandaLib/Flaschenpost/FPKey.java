package org.redPandaLib.Flaschenpost;

import kademlia.node.KademliaId;
import org.redPandaLib.crypt.Sha256Hash;

import java.util.Objects;

public class FPKey {


    KademliaId id;
    long timestamp;
    int contentHash;

    public FPKey(KademliaId id, long timestamp, int contentHash) {
        this.id = id;
        this.timestamp = timestamp;
        this.contentHash = contentHash;
    }

    public FPKey(KademliaId id, long timestamp, byte[] content) {
        return; new FPKey(id,timestamp,Sha256Hash.create(content).hashCode());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FPKey fpKey = (FPKey) o;
        return timestamp == fpKey.timestamp &&
                contentHash == fpKey.contentHash &&
                Objects.equals(id, fpKey.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, contentHash);
    }
}
