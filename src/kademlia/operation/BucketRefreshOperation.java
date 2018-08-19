package kademlia.operation;

import java.io.IOException;

import kademlia.KadConfiguration;
import kademlia.KadServer;
import kademlia.KademliaNode;
import kademlia.node.KademliaId;
import org.redPandaLib.core.ConnectionHandler;

/**
 * At each time interval t, nodes need to refresh their K-Buckets
 * This operation takes care of refreshing this node's K-Buckets
 *
 * @author Joshua Kissoon
 * @created 20140224
 */
public class BucketRefreshOperation implements Operation {

    private final KadServer server;
    private final KademliaNode localNode;
    private final KadConfiguration config;

    public BucketRefreshOperation(KadServer server, KademliaNode localNode, KadConfiguration config) {
        this.server = server;
        this.localNode = localNode;
        this.config = config;
    }

    /**
     * Each bucket need to be refreshed at every time interval t.
     * Find an identifier in each bucket's range, use it to look for nodes closest to this identifier
     * allowing the bucket to be refreshed.
     * <p>
     * Then Do a NodeLookupOperation for each of the generated NodeIds,
     * This will find the K-Closest nodes to that ID, and update the necessary K-Bucket
     *
     * @throws java.io.IOException
     */
    @Override
    public synchronized void execute() throws IOException {
        for (int i = 1; i < KademliaId.ID_LENGTH; i++) {
            /* Construct a NodeId that is i bits away from the current node Id */
            final KademliaId current = this.localNode.getNode().getNodeId().generateNodeIdByDistance(i);

            /* Run the Node Lookup Operation, each in a different thread to speed up things */
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        new NodeLookupOperation(server, localNode, current, BucketRefreshOperation.this.config).execute();
                    } catch (IOException e) {
                        //System.err.println("Bucket Refresh Operation Failed. Msg: " + e.getMessage());
                    }
                }
            };


            ConnectionHandler.threadPool.execute(runnable);

        }
    }
}
