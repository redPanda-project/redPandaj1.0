package main.redanda.jobs;

import kademlia.node.KademliaId;
import main.redanda.core.Command;
import main.redanda.kademlia.KadContent;
import main.redanda.websockets.WSParser;
import org.java_websocket.WebSocket;

import java.nio.ByteBuffer;

public class KademliaSearchJobAnswerWS extends KademliaSearchJob {

    private WebSocket conn;
    private int ackID;

    public KademliaSearchJobAnswerWS(KademliaId id, WebSocket conn, int ackID) {
        super(id);
        this.conn = conn;
        this.ackID = ackID;
    }

    @Override
    protected KadContent success() {

        KadContent kadContent = super.success();

        if (kadContent == null) {
            System.out.println("job failed, did not found an entry in time...");
            //todo: send fail to light client
            return null;
        }

        //send info to peer via WS

//        WSParser.write(conn, bb);

        System.out.println("we have to send the search answer to: " + conn.getRemoteSocketAddress());


        ByteBuffer bb = ByteBuffer.allocate(1 + 4 + KademliaId.ID_LENGTH + 8 + KadContent.PUBKEY_LEN + 4 + kadContent.getContent().length + KadContent.SIGNATURE_LEN); //rough estimate

        bb.put(Command.dhtSearch);
        bb.putInt(ackID);
        bb.put(kadContent.getId().getBytes());
        bb.putLong(kadContent.getTimestamp());
        bb.put(kadContent.getPubkey());
        bb.putInt(kadContent.getContent().length);
        bb.put(kadContent.getContent());
        bb.put(kadContent.getSignature());

        WSParser.write(conn, bb);

        System.out.println("wrote search answer to: " + conn.getRemoteSocketAddress());

        return kadContent;
    }
}
