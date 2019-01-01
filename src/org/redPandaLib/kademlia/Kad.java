package org.redPandaLib.kademlia;

import kademlia.node.KademliaId;
import org.redPandaLib.core.Peer;
import org.redPandaLib.core.Test;

public class Kad {


    public static void put() {

        //ToDo: find desitnation peers and put there
        // since the network will be small at the beginning we store every entry and send it to every node


        KademliaId id = new KademliaId();

        KadContent kadContent = new KadContent(id, "{ serverVotes: }".getBytes());

        KadStoreManager.put(kadContent);


        Test.peerListLock.lock();
        try {

            for (Peer p : Test.peerList) {
                if (p.isConnected()) {

//                    p.writeBufferLock.lock();
//                    try {
//                        p.writeBuffer.put();
//                    } finally {
//                        p.writeBufferLock.unlock();
//                    }

                }
            }

        } finally {
            Test.peerListLock.unlock();
        }

    }


}
