package kademlia.operation;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import kademlia.KadConfiguration;
import kademlia.KadServer;
import kademlia.KademliaNode;
import kademlia.dht.JKademliaStorageEntry;
import kademlia.dht.KademliaDHT;
import kademlia.dht.KademliaStorageEntryMetadata;
import kademlia.exceptions.ContentNotFoundException;
import kademlia.message.Message;
import kademlia.message.StoreContentMessage;
import kademlia.node.Node;
import org.redPandaLib.Main;
import org.redPandaLib.kademlia.KadContentUpdate;

/**
 * Refresh/Restore the data on this node by sending the data to the K-Closest nodes to the data
 *
 * @author Joshua Kissoon
 * @since 20140306
 */
public class ContentRefreshOperation implements Operation {

    public static int waitForRefresh = 60 * 4; //in mins
    private final KadServer server;
    private final KademliaNode localNode;
    private final KademliaDHT dht;
    private final KadConfiguration config;

    /**
     * We should start we an slow upload for content refresh...
     */
    private static int sleep = 10000;
    private int bytesSend = 0;
    private long timestart = System.currentTimeMillis();

    public ContentRefreshOperation(KadServer server, KademliaNode localNode, KademliaDHT dht, KadConfiguration config) {
        this.server = server;
        this.localNode = localNode;
        this.dht = dht;
        this.config = config;
    }

    /**
     * For each content stored on this DHT, distribute it to the K closest nodes
     * Also delete the content if this node is no longer one of the K closest nodes
     * <p>
     * We assume that our JKademliaRoutingTable is updated, and we can get the K closest nodes from that table
     *
     * @throws java.io.IOException
     */
    @Override
    public void execute() throws IOException {
        /* Get a list of all storage entries for content */
        List<KademliaStorageEntryMetadata> entries = this.dht.getStorageEntries();



        /* If a content was last republished before this time, then we need to republish it */
//        final long minRepublishTime = (System.currentTimeMillis() / 1000L) - this.config.restoreInterval();
        long minRepublishTime = (System.currentTimeMillis() / 1000L) - 60 * waitForRefresh;


        System.out.println("refresh content...");

        /* For each storage entry, distribute it */
        for (KademliaStorageEntryMetadata e : entries) {

            if (Main.shutdown) {
                break;
            }


            if (KadContentUpdate.updateInProgress) {
                System.out.println("content refresh aborted, update in progress");
                //resume refresh later
                break;
            }

//            System.out.println("refresh!: " + entries.toString() + " -- " + (e.lastRepublished() - minRepublishTime));


            boolean isSystemUpdate = e.getType().equals(KadContentUpdate.TYPE);

            if ((System.currentTimeMillis() / 1000L) - e.getLastUpdatedTimestamp() > 60 * 60 * 24 * 1 && !isSystemUpdate) {
                try {
                    this.dht.remove(e);
                } catch (ContentNotFoundException e1) {
                    e1.printStackTrace();
                }
                System.out.println("removed content because it was older than 1 day, seconds: " + ((System.currentTimeMillis() / 1000L) - e.getLastUpdatedTimestamp()));
                continue;
            }


            if (isSystemUpdate && (e.getLastUpdatedTimestamp() + 30 < KadContentUpdate.lastUpdateTimestamp / 1000)) {
                //we should not broadcast old updates
                System.out.println("old update");
                continue;
            }

            /* Check last update time of this entry and only distribute it if it has been last updated > 1 hour ago */
            if (e.lastRepublished() > minRepublishTime) {
                continue;
            }


//            if (bytesSend > 1024 * 1024*5) {
//                System.out.println("send bytes: " + bytesSend + " waiting for next run");
//                break;
//            }

//            System.out.println("refresh!: " + entries.toString());
            /* Set that this content is now republished */
            e.updateLastRepublished();
//            System.out.println("refresh!: " + entries.toString());
            /* Get the K closest nodes to this entries */
            List<Node> closestNodes = this.localNode.getRoutingTable().findClosest(e.getKey(), this.config.k());


            /* Create the message */
            Message msg = null;
            JKademliaStorageEntry jKademliaStorageEntry = null;
            try {
                jKademliaStorageEntry = dht.get(e);
                msg = new StoreContentMessage(this.localNode.getNode(), jKademliaStorageEntry);
            } catch (NoSuchElementException ex) {
                try {
                    dht.remove(e);
                } catch (ContentNotFoundException e1) {
                    e1.printStackTrace();
                }
                continue;
            }
//            System.out.println("refresh!: " + entries.toString());
            /*Store the message on all of the K-Nodes*/
            int cnt = 0;
            for (Node n : closestNodes) {
                /*We don't need to again store the content locally, it's already here*/
                if (!n.equals(this.localNode.getNode())) {
                    /* Send a contentstore operation to the K-Closest nodes */
                    this.server.sendMessage(n, msg, null);
//                    System.out.println("sendmsg!: " + msg.toString());
                    bytesSend += jKademliaStorageEntry.getContent().length;
//                    System.out.println("bytes send: " + bytesSend);
                    if (Main.shutdown) {
                        break;
                    }
                    checkSpeedAndWait();
                    cnt++;
                }
//                System.out.println("refresh sent to node");

            }

            System.out.println("refresh send to nodes: " + cnt);

            /* Delete any content on this node that this node is not one of the K-Closest nodes to */
            try {
                if (!closestNodes.contains(this.localNode.getNode())) {
                    this.dht.remove(e);
                    System.out.println("removed dht entry because closest nodes have this entry!: " + e);
                }
            } catch (ContentNotFoundException cnfe) {
                /* It would be weird if the content is not found here */
                System.err.println("ContentRefreshOperation: Removing content from local node, content not found... Message: " + cnfe.getMessage());
            }
        }

    }

    private void checkSpeedAndWait() {


        sleep = (int) ((double) bytesSend / ((double) (1000)));
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        double l = ((double) bytesSend / ((double) (System.currentTimeMillis() - timestart)));
//
//
//        if (l > 80) {
//            sleep += 200;
//        }
//
//        if (l < 50) {
//            sleep -= 10;
//        }
//
//        if (sleep < 0) {
//            sleep = 0;
//        }
//
//
        bytesSend = 0;
        timestart = System.currentTimeMillis();
//        if (Math.random() < 0.10) {
        System.out.println("kbsec: " + l);
//        }
    }
}
