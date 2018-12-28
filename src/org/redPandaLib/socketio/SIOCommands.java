package org.redPandaLib.socketio;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import org.redPandaLib.core.Peer;
import org.redPandaLib.core.Settings;
import org.redPandaLib.core.Test;


import java.util.ArrayList;

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

                int cnt = 0;
                for (Peer p : cp) {

                    if (!p.isConnected()) {
                        continue;
                    }

                    cnt++;
                    if (cnt > d.getCount()) {
                        break;
                    }
                    peers.add(new DPeer("http://" + p.ip + ":" + (p.port + 100), p.getNodeId().toString()));
                }


                for (int i = 0; i < 5; i++) {
                    peers.add(new DPeer("url " + i, "nodeId " + i));
                }

                ackRequest.sendAckData(peers);

            }
        });

    }

}
