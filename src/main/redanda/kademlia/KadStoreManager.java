package main.redanda.kademlia;

import kademlia.node.KademliaId;
import main.redanda.core.Channel;
import main.redanda.core.JobScheduler;
import main.redanda.core.Log;
import main.redanda.crypt.Base58;
import main.redanda.crypt.ECKey;
import main.redanda.crypt.Sha256Hash;
import main.redanda.crypt.Utils;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
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


    public static void main(String[] args) {

        //lets create a keypair for a DHT destination key, should be included in channel later
        ECKey key = new ECKey();

        //lets calculate the destination
        byte[] pubKey = key.getPubKey();


        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        System.out.println("UTC Date is: " + dateFormat.format(date));

        byte[] dateStringBytes = dateFormat.format(date).getBytes();

        ByteBuffer buffer = ByteBuffer.allocate(pubKey.length + dateStringBytes.length);
        buffer.put(pubKey);
        buffer.put(dateStringBytes);

        Sha256Hash dhtKey = Sha256Hash.create(buffer.array());

        System.out.println("" + Base58.encode((dhtKey.getBytes())) + " byteLen: " + dhtKey.getBytes().length);

        KademliaId kademliaId = KademliaId.fromFirstBytes(dhtKey.getBytes());

//        System.out.println("kadid: " + kademliaId.hexRepresentation());
//        System.out.println("kadid: " + Utils.bytesToHexString(dhtKey.getBytes()));

        System.out.println("kadid: " + kademliaId.toString());

        //random content
        byte[] payload = new byte[1024];
        new Random().nextBytes(payload);


        KadContent kadContent = new KadContent(kademliaId, key.getPubKey(), payload);

        kadContent.signWith(key);

        System.out.println("signature: " + Utils.bytesToHexString(kadContent.getSignature()) + " len: " + kadContent.getSignature().length);


        //lets check the signature

        System.out.println("verified: " + kadContent.verify());


        //assoziate an command pointer to the job
        HashMap<Integer, ScheduledFuture> runningJobs = new HashMap<>();


        final int pointer = new Random().nextInt();

        Job job = new Job(runningJobs, pointer);


        ScheduledFuture future = JobScheduler.insert(job, 500);
        runningJobs.put(pointer, future);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ScheduledFuture scheduledFuture = runningJobs.get(pointer);

        Job r = (Job) job;

        boolean couldCancel = scheduledFuture.cancel(false);
        System.out.println("cancel: " + couldCancel);


        //if we are able to cancel the runnable, we have to transmit the new data to the runnable
        if (couldCancel) {
            r.setData("new data");
            r.run();
        }

        System.out.println("asd");

    }

    static class Job implements Runnable {

        HashMap<Integer, ScheduledFuture> runningJobs;
        private Integer pointer;
        private String data = null;

        public Job(HashMap<Integer, ScheduledFuture> runningJobs, Integer pointer) {
            this.runningJobs = runningJobs;
            this.pointer = pointer;
        }

        boolean done = false;
        int timesRun = 0;

        @Override
        public void run() {


            System.out.println("asdf " + data + " done: " + done);

            if (done) {
                ScheduledFuture sf = runningJobs.remove(pointer);
                sf.cancel(false);
            }
            timesRun++;
        }

        public void setData(String str) {
            data = str;
            done = true;
        }
    }

}
