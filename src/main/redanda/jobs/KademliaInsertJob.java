package main.redanda.jobs;

import kademlia.node.KademliaId;
import main.redanda.core.Command;
import main.redanda.core.Peer;
import main.redanda.core.Test;
import main.redanda.kademlia.KadContent;
import main.redanda.kademlia.PeerComparator;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.TreeMap;

public class KademliaInsertJob extends Job {

    public static final int SEND_TO_NODES = 3;
    private static final int NONE = 0;
    private static final int ASKED = 2;
    private static final int SUCCESS = 1;

    private KadContent kadContent;
    private TreeMap<Peer, Integer> peers = null;


    public KademliaInsertJob(KadContent kadContent) {
        this.kadContent = kadContent;
    }

    @Override
    public void init() {

        //lets sort the peers by the destination key
        peers = new TreeMap<>(new PeerComparator(kadContent.getId()));

        //insert all nodes
        Test.getPeerListLock().lock();
        try {
            ArrayList<Peer> peerList = Test.getPeerList();

            for (Peer p : peerList) {
                peers.put(p, NONE);
            }
        } finally {
            Test.getPeerListLock().unlock();
        }
    }

    @Override
    public void work() {


        int askedPeers = 0;
        int successfullPeers = 0;
        for (Peer p : peers.keySet()) {


            Integer status = peers.get(p);
            if (status == SUCCESS) {
                successfullPeers++;
                askedPeers++;
                continue;
            } else if (status == ASKED) {
                continue;
            }


            if (askedPeers >= SEND_TO_NODES) {
                break;
            }

            if (p.isConnected()) {

                p.getWriteBufferLock().lock();
                try {

                    peers.put(p, ASKED);
                    askedPeers++;

                    ByteBuffer writeBuffer = p.getWriteBuffer();

                    System.out.println("putKadCmd to peer: " + p.getNodeId().toString() + " size: " + peers.size());

//                    writeBuffer.put(Command.KADEMLIA_STORE);
//                    writeBuffer.put(kadContent.getId().getBytes());
//                    writeBuffer.putLong(kadContent.getTimestamp());
//                    writeBuffer.put(kadContent.getPubkey());
//                    writeBuffer.putInt(kadContent.getContent().length);
//                    writeBuffer.put(kadContent.getContent());
//                    writeBuffer.put(kadContent.getSignature());


                } finally {
                    p.getWriteBufferLock().unlock();
                }


            }


        }


        if (successfullPeers > SEND_TO_NODES) {
            done();
        }


    }


}
