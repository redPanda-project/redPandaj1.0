package org.redPandaLib.kademlia;

import com.google.gson.Gson;
import kademlia.dht.GetParameter;
import kademlia.dht.KadContent;
import kademlia.dht.KademliaStorageEntry;
import kademlia.dht.KademliaStorageEntryMetadata;
import kademlia.exceptions.ContentNotFoundException;
import kademlia.node.KademliaId;
import org.redPandaLib.SpecialChannels;
import org.redPandaLib.core.Channel;
import org.redPandaLib.core.messages.RawMsg;
import org.redPandaLib.crypt.ECKey;
import org.redPandaLib.crypt.Sha256Hash;
import org.redPandaLib.crypt.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

public class KadContentUpdate implements KadContent {

    public static final transient String TYPE = "serverUpdate3";
    public static long lastUpdateTimestamp = 0;
    public static final KademliaId INITIAL_UPDATE_KEY = new KademliaId("ASF456789djem45674DH");
    public static boolean updateInProgress = false;

    private KademliaId key;
    private byte[] data;
    private String ownerId;
    private final long createTs;
    private long updateTs;
    private byte[] nextKeyId;


    {
        this.createTs = this.updateTs = System.currentTimeMillis() / 1000L;
    }

    public KadContentUpdate() {

    }


    public KadContentUpdate(KademliaId key, byte[] data, byte[] nextKeyId) {
        this.ownerId = "main";
        this.data = data;
        this.key = key;
        this.nextKeyId = nextKeyId;
    }

    public KadContentUpdate(KademliaId key, String ownerId) {
        this.key = key;
        this.ownerId = ownerId;
    }

    public void setData(byte[] newData) {
        this.data = newData;
        this.setUpdated();
    }

    @Override
    public KademliaId getKey() {
        return this.key;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getOwnerId() {
        return this.ownerId;
    }

    /**
     * Set the content as updated
     */
    public void setUpdated() {
        this.updateTs = System.currentTimeMillis() / 1000L;
    }

    @Override
    public long getCreatedTimestamp() {
        return this.createTs;
    }

    @Override
    public long getLastUpdatedTimestamp() {
        return this.updateTs;
    }

    @Override
    public byte[] toSerializedForm() {
        Gson gson = new Gson();
        return gson.toJson(this).getBytes();
    }

    @Override
    public KadContentUpdate fromSerializedForm(byte[] data) {
//        System.out.println("string: " + new String(data));
        Gson gson = new Gson();
        KadContentUpdate val = gson.fromJson(new String(data), KadContentUpdate.class);
        return val;
    }

    @Override
    public String toString() {
        return "DHTContentImpl[{data=" + this.data + "{ {key:" + this.key + "}]";
    }


    public static void insertNewUpdate() throws IOException {
        updateInProgress = true;

        File file = new File("out/artifacts/redPandaj_jar/redPandaj.jar");

        long timestamp = file.lastModified();

        System.out.println("timestamp : " + timestamp);

        Path path = Paths.get("out/artifacts/redPandaj_jar/redPandaj.jar");
        byte[] data = Files.readAllBytes(path);

        int updateSize = data.length;

//        data = new byte[32000];

        ByteBuffer d = ByteBuffer.wrap(data);
//        d.flip();

        int i = 0;

        int chunks = 15240;

        KademliaId kademliaId = new KademliaId("ASF456789djem45674DH");
        byte[] keyBytes = kademliaId.getBytes();

        System.out.println("start");

        KademliaId currId = kademliaId;
        KademliaId linkToId = kademliaId;

//        System.out.println(kademliaId.toString());
//        System.out.println(Utils.bytesToHexString(keyBytes));
//
//        System.out.println(Utils.bytesToHexString(keyBytes));

        long initTime = System.currentTimeMillis();

        DecimalFormat df = new DecimalFormat("##.##");
        df.setRoundingMode(RoundingMode.CEILING);

        int sendSpeed = 2000;
        int countSendToNodes = 0;
        while (d.hasRemaining()) {

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            double progress = (double) d.position() / (double) updateSize;

            double remainingTime = (double) (System.currentTimeMillis() - initTime) * (1 / progress - 1);

            System.out.println("generating chunk: " + i + " progress: " + df.format(progress) + " remaining: " + df.format(remainingTime / 1000.));


            chunks = Math.min(chunks, d.remaining());

            byte[] bytes = new byte[chunks];

            d.get(bytes);

//            System.out.println(toBinString(keyBytes));

//            increment(keyBytes);
//            incrementAtIndex(keyBytes, keyBytes.length - 4);
//            incrementAtIndex(keyBytes, keyBytes.length - 5);
//            incrementAtIndex(keyBytes, keyBytes.length - 6);
//            System.out.println(Utils.bytesToHexString(keyBytes));
//            byte[] bytes2 = Arrays.copyOf(keyBytes, keyBytes.length);
//            KadContentUpdate c = new KadContentUpdate(new KademliaId(bytes2), bytes);

            currId = linkToId;
            linkToId = new KademliaId();
            KadContentUpdate c;
//            if (!d.hasRemaining()) {
//                c = new KadContentUpdate(currId, bytes, null);
//            } else {
            c = new KadContentUpdate(currId, bytes, linkToId.getBytes());
//            System.out.println("" + c.updateTs);
//            }


            try {


                long time = System.currentTimeMillis();
                sendSpeed++;

                if (sendSpeed > 800) {
                    sendSpeed = 800;
                }

//                if (i < 5) {
                countSendToNodes = 0;
                Kad.node.putLocally(c);
                while (countSendToNodes < 3) {
                    countSendToNodes = Kad.node.put(c);
//                    System.out.println("countSendToNodes to n nodes: " + countSendToNodes);

                    if (countSendToNodes < 3) {
                        sendSpeed -= 5;
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        System.out.println("send to " + countSendToNodes + " send again!");
                    }
                }

                int sleep = (int) ((double) (bytes.length * countSendToNodes) / ((double) (sendSpeed)));
                System.out.println("sendSpeed: " + sendSpeed + " sleep: " + sleep);

                sleep = Math.max(0, (int) (sleep - (System.currentTimeMillis() - time)));

                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

//                }

//                Kad.node.putLocally(c);
                i++;
            } catch (IOException e) {

                if (e.getMessage().equals("Message is too big")) {
                    System.out.println("Message is too big: " + chunks);

                    d.position(d.position() - chunks);
                    chunks -= 1024;

                }

            }


        }


        //get priv key from external file:
        try {
            String keyString = new String(Files.readAllBytes(Paths.get("privateSigningKey.txt")));


            //lets build the signing chunk:
            Channel updateChannel = SpecialChannels.getUpdateChannel(keyString);

            Sha256Hash hash = Sha256Hash.create(data);
            System.out.println("hash: " + Utils.bytesToHexString(hash.getBytes()));
            ECKey.ECDSASignature sign = updateChannel.getKey().sign(hash);

            byte[] encodeToDER = new byte[RawMsg.SIGNATURE_LENGRTH];
            byte[] sigBytes = sign.encodeToDER();
            System.arraycopy(sigBytes, 0, encodeToDER, 0, sigBytes.length);

            byte[] newBytes = new byte[encodeToDER.length];

            int index = encodeToDER.length - 1;
            while (true) {
                System.arraycopy(encodeToDER, 0, newBytes, 0, index + 1);
                if (newBytes[index] == (byte) 0) {
                    newBytes = new byte[index];

                    index--;
                } else {
                    break;
                }

            }

            //System.out.println("Sigbytes len: " + sigBytes.length + " " + Utils.bytesToHexString(encodeToDER));
            //System.out.println("Sigbytes len: " + sigBytes.length + " " + Utils.bytesToHexString(newBytes));
            byte[] signature = encodeToDER;

            System.out.println("signature: " + Utils.bytesToHexString(signature));
            currId = linkToId;
            KadContentUpdate signContent = new KadContentUpdate(currId, signature, null);
            countSendToNodes = 0;
            while (countSendToNodes < 3) {
                countSendToNodes = Kad.node.put(signContent);
            }
            Kad.node.putLocally(signContent);

        } catch (IOException e) {
            e.printStackTrace();
        }

        updateInProgress = false;

    }

    public static void checkForUpdate() throws IOException, ContentNotFoundException {

        updateInProgress = true;

        lastUpdateTimestamp = 0;

        long myCurrentVersionTimestamp = 0;

        KademliaId searchKey = INITIAL_UPDATE_KEY;

        ByteBuffer storeBuffer = ByteBuffer.allocate(65536);
        int notFound = 0;

        byte[] signature = null;
        for (int i = 0; i < 50000; i++) {

//            System.out.println("searching (" + i + " - " + storeBuffer.position() + "): " + Utils.bytesToHexString(searchKey.getBytes()));


            GetParameter gp = new GetParameter(searchKey, KadContentUpdate.TYPE);
//            gp.setType(DHTContentImpl.TYPE);
            gp.setOwnerId("main");

            try {
                KademliaStorageEntry conte = Kad.node.get(gp);

                if (lastUpdateTimestamp == 0) {
                    lastUpdateTimestamp = conte.getContentMetadata().getLastUpdatedTimestamp() * 1000;
                    System.out.println("Update found from: " + new Date(lastUpdateTimestamp));


                    System.out.println("remove old updates...");
                    List<KademliaStorageEntryMetadata> storageEntries = Kad.node.getDHT().getStorageEntries();

                    for (KademliaStorageEntryMetadata e : storageEntries) {
                        if (!e.getType().equals(KadContentUpdate.TYPE) || e.getLastUpdatedTimestamp() * 1000 >= lastUpdateTimestamp) {
                            continue;
                        }
                        try {
                            Kad.node.getDHT().remove(e);
                            System.out.println("removed old update entry: " + new Date(e.getLastUpdatedTimestamp() * 1000));
                        } catch (Throwable exxx) {

                        }
                    }


                    //get timestamp of own version
                    File file = new File("redPandaj.jar");
                    myCurrentVersionTimestamp = file.lastModified();

                    if (!file.exists()) {
                        System.out.println("No jar to update found, exiting auto update!");
                        updateInProgress = false;
                        return;
                    }

                    if (myCurrentVersionTimestamp >= lastUpdateTimestamp) {
                        System.out.println("update not required, aborting...");
                        updateInProgress = false;
                        return;
                    }


                }

//                System.out.println(conte.getContentMetadata().getKey().equals(searchKey));
//                System.out.println(Utils.bytesToHexString(searchKey.getBytes()).equals(Utils.bytesToHexString(conte.getContentMetadata().getKey().getBytes())));


                KadContentUpdate kadContentUpdate = new KadContentUpdate().fromSerializedForm(conte.getContent());
//                Kad.node.putLocally(kadContentUpdate);
                notFound = 0;
//                if (i % 10 == 0) {
                System.out.print(".");
//                }
//                System.out.println("content: " + new String(kadContentUpdate.data));

                byte[] dataBlock = kadContentUpdate.data;
                if (kadContentUpdate.nextKeyId != null) {
                    while (storeBuffer.remaining() < dataBlock.length) {
                        ByteBuffer newBuffer = ByteBuffer.allocate(storeBuffer.capacity() * 2);
                        storeBuffer.flip();
                        newBuffer.put(storeBuffer);
                        storeBuffer = newBuffer;
//                        System.out.println("store buffer size: " + storeBuffer.capacity());
                    }

                    storeBuffer.put(dataBlock);
//                System.out.println("" + dataBlock.length);

                } else {


                    storeBuffer.flip();
                    System.out.println("\nupdate downloaded completely from network, bytes: " + storeBuffer.remaining());
                    storeBuffer.compact();

                    signature = dataBlock;

                    break;
                }

                searchKey = new KademliaId(kadContentUpdate.nextKeyId);


            } catch (ContentNotFoundException | NoSuchElementException e) {
                System.out.println("not found, retry");
                i--;
                notFound++;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                if (notFound > 60) {
                    System.out.println("WARNING: update could not be completely download from dht network, failed tile: " + (i + 1) + " downloaded kb: " + storeBuffer.position() / 1000.);
                    break;
                }
            }
        }

        if (signature != null) {

            System.out.println("signature found: " + Utils.bytesToHexString(signature));

            //lets check the signature chunk:
            Channel updateChannel = SpecialChannels.getUpdateChannel();

            storeBuffer.flip();

            byte[] finalUpdateBytes = new byte[storeBuffer.remaining()];
            storeBuffer.get(finalUpdateBytes);

            Sha256Hash hash = Sha256Hash.create(finalUpdateBytes);
            System.out.println("hash: " + Utils.bytesToHexString(hash.getBytes()));

            boolean verify = updateChannel.getKey().verify(hash.getBytes(), signature);

            System.out.println("update verfified: " + verify);

            File file = new File("redPandaj.jar");
            myCurrentVersionTimestamp = file.lastModified();
            if (!file.exists()) {
                System.out.println("No jar to update found, exiting auto update!");
                return;
            }

            if (myCurrentVersionTimestamp >= lastUpdateTimestamp) {
                System.out.println("update not required, aborting...");
                return;
            }


            if (verify) {

                try (FileOutputStream fos = new FileOutputStream("update")) {
                    fos.write(finalUpdateBytes);
                    //fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
                    System.out.println("updated store in update file");
                    Kad.shutdown();
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.exit(0);
                }

            }


//                ECKey.ECDSASignature sign = updateChannel.getKey().sign(hash);
//
//                byte[] encodeToDER = new byte[RawMsg.SIGNATURE_LENGRTH];
//                byte[] sigBytes = sign.encodeToDER();
//                System.arraycopy(sigBytes, 0, encodeToDER, 0, sigBytes.length);
//
//                byte[] newBytes = new byte[encodeToDER.length];
//
//                int index = encodeToDER.length - 1;
//                while (true) {
//                    System.arraycopy(encodeToDER, 0, newBytes, 0, index + 1);
//                    if (newBytes[index] == (byte) 0) {
//                        newBytes = new byte[index];
//
//                        index--;
//                    } else {
//                        break;
//                    }
//
//                }
//
//                //System.out.println("Sigbytes len: " + sigBytes.length + " " + Utils.bytesToHexString(encodeToDER));
//                //System.out.println("Sigbytes len: " + sigBytes.length + " " + Utils.bytesToHexString(newBytes));
//                byte[] signature = encodeToDER;
//
//                System.out.println("signature: " + Utils.bytesToHexString(signature));

        }
        updateInProgress = false;

    }

    public static void main(String[] args) throws IOException, InterruptedException, ContentNotFoundException {
        KadConfig.OPERATION_TIMEOUT = 300;
        Kad.startAsync();
        Thread.sleep(1000);
        insertNewUpdate();
//        checkForUpdate();
        Thread.sleep(10000);
        Kad.shutdown();
        Thread.sleep(1000);
        System.out.println(Kad.node);
        System.exit(0);
    }


    public static byte[] increment(byte[] A) {
        boolean carry = true;
        for (int i = (A.length - 1); i >= 0; i--) {
            if (carry) {
                if (A[i] == 0) {
                    A[i] = 1;
                    carry = false;
                } else {
                    A[i] = 0;
                    carry = true;
                }
            }
        }

        return A;
    }

    private static String toBinString(byte[] a) {
        String res = "";
        for (int i = 0; i < a.length; i++) {
            res += (a[i] != 0 ? "1" : "0");
        }
        return res;
    }


    public static void incrementAtIndex(byte[] array, int index) {
        if (array[index] == Byte.MAX_VALUE) {
            array[index] = 0;
            if (index > 0) {
                incrementAtIndex(array, index - 1);
            }
        } else {
//            System.out.println("erhoeht!");
            array[index]++;
        }
    }


}
