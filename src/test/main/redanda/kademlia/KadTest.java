package main.redanda.kademlia;

import kademlia.node.KademliaId;
import main.redanda.core.Log;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KadTest {

    @Test
    void put() {

        KademliaId id = new KademliaId();
        KadContent kadContent = new KadContent(id, "{ serverVotes: }".getBytes());
        Kad.put(kadContent);
        KadContent kadContent1 = KadStoreManager.get(id);
        assertNotNull(kadContent1);

        // try to put a too far in the future content into dht, it should not be stored!
        KademliaId id2 = new KademliaId();
        KadContent kadContent2 = new KadContent(id2, System.currentTimeMillis() + 1000L * 60L * 20L, "{ serverVotes: }".getBytes());
        Kad.put(kadContent2);
        KadContent kadContent3 = KadStoreManager.get(id2);
        assertNull(kadContent3);

    }
}