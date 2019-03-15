package main.redanda.jobs;

import kademlia.node.KademliaId;
import main.redanda.core.Command;
import main.redanda.core.Peer;
import main.redanda.core.Test;
import main.redanda.kademlia.KadContent;
import main.redanda.kademlia.KadStoreManager;
import main.redanda.kademlia.PeerComparator;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class KademliaInsertJob extends Job {

    public static final int SEND_TO_NODES = 1;
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


        //We first save the KadContent in our StoreManager, we use "dht-caching"
        // such that too far away entries will be removed faster
        KadStoreManager.put(kadContent);

        //lets sort the peers by the destination key
        peers = new TreeMap<>(new PeerComparator(kadContent.getId()));

        //insert all nodes
        Test.getPeerListLock().lock();
        try {
            ArrayList<Peer> peerList = Test.getPeerList();

            if (peerList == null) {
                initilized = false;
                return;
            }

            for (Peer p : peerList) {

                if (p.getNodeId() == null) {
                    continue;
                }

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


            if (successfullPeers >= SEND_TO_NODES) {
                done();
                break;
            }


            if (askedPeers >= SEND_TO_NODES) {
                break;
            }


            if (p.isConnected() && p.isIntegrated()) {

                try {
                    //lets not wait too long for a lock, since this job may timeout otherwise
                    boolean lockedByMe = p.getWriteBufferLock().tryLock(50, TimeUnit.MILLISECONDS);
                    if (lockedByMe) {
                        try {

                            ByteBuffer writeBuffer = p.getWriteBuffer();

                            if (writeBuffer == null) {
                                continue;
                            }

                            peers.put(p, ASKED);
                            askedPeers++;


                            System.out.println("putKadCmd to peer: " + p.getNodeId().toString() + " size: " + peers.size() + " distance: " + kadContent.getId().getDistance(p.getNodeId()) + " target: " + kadContent.getId());

                            int toWriteBytes = writeBuffer.position() + kadContent.getContent().length + 1024;

//                            if (p.writeBuffer.remaining() < toWriteBytes) {
//                                ByteBuffer allocate = ByteBuffer.allocate(toWriteBytes);
//                                p.writeBuffer.flip();
//                                allocate.put(p.writeBuffer);
//                                p.writeBuffer = allocate;
//                                writeBuffer = allocate;
//                            }


                            writeBuffer.put(Command.KADEMLIA_STORE);
                            writeBuffer.putInt(getJobId());
                            writeBuffer.put(kadContent.getId().getBytes());
                            writeBuffer.putLong(kadContent.getTimestamp());
                            writeBuffer.put(kadContent.getPubkey());
                            writeBuffer.putInt(kadContent.getContent().length);
                            writeBuffer.put(kadContent.getContent());
                            writeBuffer.put(kadContent.getSignature());

                            p.setWriteBufferFilled();

                        } finally {
                            p.getWriteBufferLock().unlock();
                        }
                    } else {

                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }


        }


        if (successfullPeers > SEND_TO_NODES) {
            done();
        }


    }


    public void ack(Peer p) {
        //todo: concurrency?
        peers.put(p, SUCCESS);
    }


}
