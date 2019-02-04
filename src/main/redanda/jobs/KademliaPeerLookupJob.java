package main.redanda.jobs;

import kademlia.node.KademliaId;
import main.redanda.core.Peer;
import main.redanda.core.Test;

import java.util.ArrayList;
import java.util.TreeMap;

public class KademliaPeerLookupJob extends Job {


    private KademliaId destination;
    private TreeMap<Peer, Integer> peers = new TreeMap<>();


    public KademliaPeerLookupJob(KademliaId destination) {
        this.destination = destination;


        //insert all nodes
        Test.getPeerListLock().lock();
        try {
            ArrayList<Peer> peerList = Test.getPeerList();

            for (Peer p : peerList) {
                peers.put(p, 0);
            }

        } finally {
            Test.getPeerListLock().unlock();
        }

    }

    @Override
    public void init() {

    }

    @Override
    public void work() {

        for (Peer p : peers.keySet()) {


            if (!p.isConnected() && !p.isConnecting) {

            }


        }


    }


}
