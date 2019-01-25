package main.redanda.websockets;

import main.redanda.core.Command;
import main.redanda.core.Log;
import main.redanda.core.Peer;
import main.redanda.core.Test;
import main.redanda.socketio.DPeer;
import org.java_websocket.WebSocket;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WSParser {


    public static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 40, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    public static void parse(WebSocket conn, ByteBuffer message) {

        System.out.println("New byte message from WebSockets");


        if (message.limit() < 1) {
            Log.put("received empty buffer??", 20);
            return;
        }

        byte b = message.get();


//        System.out.println("byte: " + b);

        switch (b) {

            case Command.PEERLIST:

                if (message.remaining() < 4) {
                    Log.put("not enough bytes for peerlist", 20);
                    return;
                }

                int toSendPeersNum = message.getInt();

                toSendPeersNum = Math.min(100, toSendPeersNum); //we send max 100 peers per request!

                Log.put("Send peerlist to peer: ", 20);
//                conn.send("tuuut");


                System.out.println("getPeers: " + toSendPeersNum);

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
                    System.out.println("put: " + p.getNodeId().toString());
                    peers.add(new DPeer("http://" + p.ip + ":" + (p.port + 100), p.getNodeId().toString()));

                    String nodeId = p.getNodeId().toString();
                    String url = "ws://" + p.ip + ":" + (p.port + 100);

                    bb.putInt(nodeId.length());
                    bb.put(nodeId.getBytes());

                    bb.putInt(url.length());
                    bb.put(url.getBytes());

                    System.out.println("rzn5hz753");

                }

                write(conn, bb);


//                for (int i = 0; i < 5; i++) {
//                    peers.add(new DPeer("url " + i, "nodeId " + i));
//                }


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
