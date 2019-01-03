package main.redanda.kademlia;

import kademlia.node.KademliaId;
import main.redanda.core.Log;

import javax.annotation.concurrent.ThreadSafe;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@ThreadSafe
public class KadStoreManager {

    private static final int MIN_SIZE = 1024 * 1024 * 50; //size of content without key
    private static final long KEEP_TIME = 1000L * 60L * 60L * 24L * 7L;

    private static final Map<KademliaId, KadContent> entries = new HashMap<>();
    private static final ReentrantLock lock = new ReentrantLock();
    private static long lastCleanup = 0;
    private static int size = 0;


    /**
     * basic put operation into our DHT Storage, if entry exists with same KadId,
     * only the one with the highest timestamp is kept.
     * If timestamp is too far in the future, the content is ignored!
     *
     * @param content
     */
    public static void put(KadContent content) {

        KademliaId id = content.getId();

        long currTime = System.currentTimeMillis();

        if (content.getTimestamp() - currTime > 1000L * 60L * 15L) {
            Log.put("Content for DHT entry is too new!", 50);
            return;
        }


        lock.lock();
        try {
            KadContent foundContent = entries.get(id);

            if (foundContent == null || content.getTimestamp() > foundContent.getTimestamp()) {
                entries.put(id, content);
                size += content.getContent().length;
            }


            if (size > MIN_SIZE && currTime > lastCleanup + 1000L * 60L * 10L) {
                lastCleanup = currTime;

                for (KadContent c : entries.values()) {
                    if (c.getTimestamp() < currTime - KEEP_TIME) {
                        entries.remove(c.getId());
                        size -= c.getContent().length;
                    }
                }

            }


        } finally {
            lock.unlock();
        }


    }

    public static KadContent get(KademliaId id) {
        lock.lock();
        try {
            return entries.get(id);
        } finally {
            lock.unlock();
        }
    }

}
