package main.redanda.socketio;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import main.redanda.core.Peer;
import main.redanda.core.Test;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public class SIOCommands {

    public static void init(SocketIOServer s) {

        s.addEventListener("set-nickname", ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
                System.out.println("" + data.getUserName() + " " + data.getMessage());
            }
        });


        s.addEventListener("getPeers", DgetPeers.class, new DataListener<DgetPeers>() {
            @Override
            public void onData(SocketIOClient client, DgetPeers d, AckRequest ackRequest) {
                System.out.println("getPeers: " + d.getCount());

                ArrayList<DPeer> peers = new ArrayList<>();


                ArrayList<Peer> cp = Test.getClonedPeerList();

                //randomize the peers
                Collections.shuffle(cp);

                int cnt = 0;
                for (Peer p : cp) {

                    if (!p.isConnected()) {
                        continue;
                    }

                    cnt++;
                    if (cnt > d.getCount()) {
                        break;
                    }
                    System.out.println("put: " + p.getNodeId().toString());
                    peers.add(new DPeer("http://" + p.ip + ":" + (p.port + 100), p.getNodeId().toString()));
                }


//                for (int i = 0; i < 5; i++) {
//                    peers.add(new DPeer("url " + i, "nodeId " + i));
//                }

                ackRequest.sendAckData(peers);

            }
        });


        s.addEventListener("getAndroid.apk", ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
                try {
                    Path path = Paths.get("android.apk");
                    byte[] fileData = Files.readAllBytes(path);

                    File file = new File("android.apk");
                    long myCurrentVersionTimestamp = file.lastModified();

                    DUpdateObject testObject = new DUpdateObject(fileData, Test.localSettings.getUpdateAndroidSignature(), myCurrentVersionTimestamp);

                    ackRequest.sendAckData(testObject);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        });

    }

}
