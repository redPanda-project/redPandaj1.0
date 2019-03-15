package main.redanda.websockets;

import kademlia.node.KademliaId;
import main.redanda.core.Command;
import main.redanda.core.Log;
import main.redanda.core.Peer;
import main.redanda.core.Test;
import main.redanda.jobs.KademliaInsertJob;
import main.redanda.kademlia.KadContent;
import main.redanda.kademlia.KadStoreManager;
import main.redanda.socketio.DPeer;
import main.redanda.socketio.DUpdateObject;
import org.java_websocket.WebSocket;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.*;

public class WSParser {


    public static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(40, 40, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    public static void parse(WebSocket conn, ByteBuffer message) {

//        System.out.println("New byte message from WebSockets");


        if (message.limit() < 1) {
            Log.put("received empty buffer??", 20);
            return;
        }

        byte b = message.get();


//        System.out.println("byte: " + b);

        switch (b) {

            case Command.PEERLIST:
                parsePeerlist(conn, message);
                break;

            case Command.getAndroidTimeStamp:
                parseAndroidTimestamp(conn);
                break;

            case Command.getAndroidApk:
                System.out.println("get android apk");

                try {
                    Path path = Paths.get("android.apk");
                    byte[] fileData = Files.readAllBytes(path);

                    File file = new File("android.apk");
                    long myCurrentVersionTimestamp = (long) Math.ceil(file.lastModified() / 1000.) * 1000;

                    DUpdateObject testObject = new DUpdateObject(fileData, Test.localSettings.getUpdateAndroidSignature(), myCurrentVersionTimestamp);

                    ByteBuffer bb = ByteBuffer.allocate(1 + 8 + 4 + Test.localSettings.getUpdateAndroidSignature().length + 4 + fileData.length);
                    bb.put(Command.getAndroidApk);
                    bb.putLong(myCurrentVersionTimestamp);

                    System.out.println("asdf " + myCurrentVersionTimestamp);
                    System.out.println("asdf " + Test.localSettings.getUpdateAndroidSignature().length);
                    System.out.println("asdf " + fileData.length);

                    bb.putInt(Test.localSettings.getUpdateAndroidSignature().length);
                    bb.put(Test.localSettings.getUpdateAndroidSignature());

                    bb.putInt(fileData.length);
                    bb.put(fileData);

                    write(conn, bb);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                break;


            case Command.dhtStore:
                System.out.println("got dht store command:");

                int commandId = message.getInt();

                byte[] kadIdBytes = new byte[KademliaId.ID_LENGTH / 8];
                message.get(kadIdBytes);
                KademliaId kademliaId = new KademliaId(kadIdBytes);
                long timestamp = message.getLong();

                System.out.println("long: " + timestamp);

                byte[] pubkey = new byte[KadContent.PUBKEY_LEN];
                message.get(pubkey);

                int contentLen = message.getInt();
                byte[] content = new byte[contentLen];
                message.get(content);

                System.out.println("len: " + contentLen);

                byte[] signature = new byte[KadContent.SIGNATURE_LEN];
                message.get(signature);


                KadContent kadContent = new KadContent(kademliaId, timestamp, pubkey, content, signature);


                System.out.println("remaining: " + message.remaining() + " " + kadContent.verify());


                if (kadContent.verify()) {
                    new KademliaInsertJob(kadContent).start();
//                    KadStoreManager.put(kadContent);
//                    System.out.println("stored!");
                }


                break;

            default:
                Log.put("Command not found in WSParse: " + b, 20);
                break;
        }

//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        System.out.println("threadPool: " + threadPool.getPoolSize());

    }

    private static void parsePeerlist(WebSocket conn, ByteBuffer message) {
        if (message.remaining() < 4) {
            Log.put("not enough bytes for peerlist", 20);
            return;
        }

        int toSendPeersNum = message.getInt();

        toSendPeersNum = Math.min(100, toSendPeersNum); //we send max 100 peers per request!

//        Log.put("Send peerlist to peer: ", 200);

        //get peers
        ArrayList<Peer> cp = Test.getClonedPeerList();

//        System.out.println("shuffle");

        //randomize the peers
        Collections.shuffle(cp);

//        System.out.println("shuffled");

        ByteBuffer bb = ByteBuffer.allocate(400 + 100 * toSendPeersNum); //rough estimate
        bb.put(Command.PEERLIST);

        int byteCount = 0;

//        System.out.println("before loop");

        int cnt = 0;
        for (Peer p : cp) {
//            System.out.println("loop");
            if (!p.isConnected()) {
                continue;
            }

            cnt++;
            if (cnt > toSendPeersNum) {
                break;
            }

            //add peer to bytebuffer
            String nodeId = p.getNodeId().toString();
            String url = "ws://" + p.ip + ":" + (p.port + 100);

            bb.putInt(nodeId.length());
            bb.put(nodeId.getBytes());

            bb.putInt(url.length());
            bb.put(url.getBytes());

        }
//        System.out.println("end loop");
        //send now
        write(conn, bb);
//        System.out.println("send nodes to peer: " + cnt);

    }

    private static void parseAndroidTimestamp(WebSocket conn) {
//        System.out.println("getAndroidTimeStamp");

        File file = new File("android.apk");
        long myCurrentVersionTimestamp = (long) Math.ceil(file.lastModified() / 1000.) * 1000;

        ByteBuffer bb = ByteBuffer.allocate(1 + 8);
        bb.put(Command.getAndroidTimeStamp);
        bb.putLong(myCurrentVersionTimestamp);
        write(conn, bb);
    }

    public static void write(WebSocket conn, ByteBuffer bb) {
        bb.flip();
        byte[] bytes = new byte[bb.remaining()];
        bb.get(bytes);
        conn.send(bytes);
    }


    public static void parseAsync(WebSocket conn, ByteBuffer message) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

//                System.out.println(threadPool.getPoolSize());

                if (threadPool.getPoolSize() < threadPool.getMaximumPoolSize() && threadPool.getQueue().size() > 1) {
                    threadPool.setCorePoolSize(threadPool.getPoolSize() + 1);
//                    System.out.println("increase threadpool!");
                } else if (threadPool.getCorePoolSize() > 2) {
                    threadPool.setCorePoolSize(2);
//                    System.out.println("set threadpool to default");
                }

                parse(conn, message);


            }
        };

        Future<?> submit = threadPool.submit(runnable);

        //lets check for errors!
        try {
            submit.get();
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }
}
