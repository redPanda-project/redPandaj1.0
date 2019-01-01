package org.redPandaLib.Flaschenpost;

import kademlia.node.KademliaId;
import org.redPandaLib.core.Log;
import org.redPandaLib.kademlia.KadContent;

import javax.annotation.concurrent.ThreadSafe;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;


@ThreadSafe
public class FPStoreManager {


    private static final HashSet<FPKey> entries = new HashSet<>();
    private static final ReentrantLock lock = new ReentrantLock();


    /**
     * we put a Flaschenpost into this set to remember that
     * we alreay broadcasted that Flaschenpost to other peers
     * to mitigate content changing attacks, we have to add the content hash to the key
     * @param key
     */
    public void put(FPKey key) {

        //ToDO maybe batter performance if we omit the object creation (FPkey)

        if (System.currentTimeMillis() + 1000L * 60L * 15L > key.getTimestamp()) {
            Log.put("Content for DHT entry is too new!", 50);
            return;
        }

        lock.lock();
        try {
            entries.add(key);
        } finally {
            lock.unlock();
        }


    }

    public boolean contains(FPKey key) {
        lock.lock();
        try {
            return entries.contains(key);
        } finally {
            lock.unlock();
        }
    }

}
