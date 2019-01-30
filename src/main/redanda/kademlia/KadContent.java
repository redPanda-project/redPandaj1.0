package main.redanda.kademlia;

import kademlia.node.KademliaId;
import main.redanda.crypt.ECKey;
import main.redanda.crypt.Sha256Hash;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class KadContent {


    private KademliaId id; //we store the ID duplicated because of performance reasons (new lookup in the hashmap costs more than a bit of memory)
    private long timestamp; //created at (or updated)
    private byte[] pubkey;
    private byte[] content;
    private byte[] signature;


    public KadContent(KademliaId id, long timestamp, byte[] pubkey, byte[] content) {
        this.id = id;
        this.timestamp = timestamp;
        this.pubkey = pubkey;
        this.content = content;
    }

    public KadContent(KademliaId id, byte[] pubkey, byte[] content) {
        this.id = id;
        this.timestamp = System.currentTimeMillis();
        this.pubkey = pubkey;
        this.content = content;
    }


    public byte[] getPubkey() {
        return pubkey;
    }

    public KademliaId getId() {
        return id;
    }

    public void setId(KademliaId id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public byte[] getSignature() {
        return signature;
    }

    public Sha256Hash createHash() {

        ByteBuffer buffer = ByteBuffer.allocate(8 + content.length);
        buffer.putLong(timestamp);
        buffer.put(content);

        Sha256Hash hash = Sha256Hash.create(buffer.array());
        return hash;
    }


    public void signWith(ECKey privateKey) {

        Sha256Hash hash = createHash();

        ECKey.ECDSASignature sign = privateKey.sign(hash);

        signature = sign.toBytes();
    }

    public boolean verify() {

        Sha256Hash hash = createHash();

//        ECKey ecKey = new ECKey(null, pubkey);
        ECKey ecKey = new ECKey(BigInteger.ZERO, pubkey, true);

        ECKey.ECDSASignature ecdsaSignature = ECKey.ECDSASignature.fromBytes(signature);

        return ecKey.verify(hash, ecdsaSignature);
    }
}
