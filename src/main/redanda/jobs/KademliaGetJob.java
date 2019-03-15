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
import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class KademliaGetJob extends Job {

    public static final int SEND_TO_NODES = 2;
    private static final int NONE = 0;
    private static final int ASKED = 2;
    private static final int SUCCESS = 1;

    private KademliaId id;
    private TreeMap<Peer, Integer> peers = null;
    private ArrayList<KadContent> contents = new ArrayList<>();

    public KademliaGetJob(KademliaId id) {
        this.id = id;
    }

    @Override
    public void init() {



        //lets sort the peers by the destination key
        peers = new TreeMap<>(new PeerComparator(id));

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

                success();

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


                            System.out.println("seach KadId from peer: " + p.getNodeId().toString() + " size: " + peers.size() + " distance: " + id.getDistance(p.getNodeId()) + " target: " + id);




                            writeBuffer.put(Command.KADEMLIA_GET);
                            writeBuffer.putInt(getJobId());
                            writeBuffer.put(id.getBytes());

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

    private void success() {

        System.out.println("sucess!!!222");

        //lets get the newest one!
        contents.sort(new Comparator<KadContent>() {
            @Override
            public int compare(KadContent o1, KadContent o2) {
                return o1.getTimestamp()<o1.getTimestamp()?-1:
                        o1.getTimestamp()>o1.getTimestamp()?1:0;
            }
        });

        System.out.println("newst content found: " + contents.get(0).getTimestamp());


    }


    public void ack(KadContent c, Peer p) {
        //todo: concurrency?
        contents.add(c);
        peers.put(p, SUCCESS);
        System.out.println("ack2!!");
    }


}
