package main.redanda.kademlia;

import kademlia.node.KademliaId;
import main.redanda.core.Peer;
import main.redanda.core.Test;

public class Kad {


    public static void put(KadContent kadContent) {

        //ToDo: find desitnation peers and put there
        // since the network will be small at the beginning we store every entry and send it to every node


        KadStoreManager.put(kadContent);


        Test.peerListLock.lock();
        if (Test.peerList != null) {
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


}
