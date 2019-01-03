package main.redanda.flaschenpost;

import kademlia.node.KademliaId;
import main.redanda.core.Peer;
import main.redanda.core.Test;

import java.math.BigInteger;

public class Flaschenpost {


    public static void put(KademliaId destination, byte[] content) {


        //let us get the closest node in our peerlist to the destination

        // we only want to find peers which are closer to destination than we are
        int nearestDistance = destination.getDistance(Test.NONCE);
        Peer nearestPeer = null;

        System.out.println("our distance: " + nearestDistance);

        Test.getPeerListLock().lock();
        try {
            for (Peer p : Test.getPeerList()) {

                if (p.getNodeId() == null) {
                    continue;
                }

                int distance = p.getNodeId().getDistance(destination);
                System.out.println("distance: " + distance);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPeer = p;
                }

            }


            if (nearestPeer == null) {
                System.out.println("we are the closest peer!");
            } else {
                System.out.println("found peer with distance: " + nearestDistance + " peer: " + nearestPeer.getNodeId());
            }


        } finally {
            Test.getPeerListLock().unlock();
        }


    }

}
