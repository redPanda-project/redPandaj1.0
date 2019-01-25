package main.redanda.websockets;

import main.redanda.core.Command;
import main.redanda.core.Log;
import main.redanda.core.Peer;
import main.redanda.core.Test;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WSParser {


    public static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 40, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

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
                    long myCurrentVersionTimestamp =(long) Math.ceil(file.lastModified() / 1000.) * 1000;

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

        Log.put("Send peerlist to peer: ", 200);


//                System.out.println("getPeers: " + toSendPeersNum);

        ArrayList<DPeer> peers = new ArrayList<>();


        ArrayList<Peer> cp = Test.getClonedPeerList();

        //randomize the peers
        Collections.shuffle(cp);

        ByteBuffer bb = ByteBuffer.allocate(toSendPeersNum * 60); //rough estimate
        bb.put(Command.PEERLIST);

        int byteCount = 0;

        int cnt = 0;
        for (Peer p : cp) {

            if (!p.isConnected()) {
                continue;
            }

            cnt++;
            if (cnt > toSendPeersNum) {
                break;
            }
//                    System.out.println("put: " + p.getNodeId().toString());
            peers.add(new DPeer("http://" + p.ip + ":" + (p.port + 100), p.getNodeId().toString()));

            String nodeId = p.getNodeId().toString();
            String url = "ws://" + p.ip + ":" + (p.port + 100);

            bb.putInt(nodeId.length());
            bb.put(nodeId.getBytes());

            bb.putInt(url.length());
            bb.put(url.getBytes());

        }

        write(conn, bb);


//                for (int i = 0; i < 5; i++) {
//                    peers.add(new DPeer("url " + i, "nodeId " + i));
//                }
    }

    private static void parseAndroidTimestamp(WebSocket conn) {
        System.out.println("getAndroidTimeStamp");

        File file = new File("android.apk");
        long myCurrentVersionTimestamp = (long) Math.ceil(file.lastModified() / 1000.) * 1000;

        ByteBuffer bb = ByteBuffer.allocate(1 + 8);
        bb.put(Command.getAndroidTimeStamp);
        bb.putLong(myCurrentVersionTimestamp);
        write(conn, bb);
    }

    private static void write(WebSocket conn, ByteBuffer bb) {
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
                } else if (threadPool.getCorePoolSize() > 1) {
                    threadPool.setCorePoolSize(1);
//                    System.out.println("set threadpool to default");
                }

                parse(conn, message);


            }
        };
        threadPool.submit(runnable);

    }
}
