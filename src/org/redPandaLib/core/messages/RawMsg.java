/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core.messages;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.core.Channel;
import org.redPandaLib.crypt.AESCrypt;
import org.redPandaLib.crypt.ECKey;
import org.redPandaLib.crypt.ECKey.ECDSASignature;
import org.redPandaLib.crypt.Sha256Hash;

/**
 *
 * @author robin
 */
public class RawMsg implements Serializable, Comparable<RawMsg> {

    int nichts = 2;
    public static final int SIGNATURE_LENGRTH = 72;
    //head
    public ECKey key;
    public long timestamp;
    public int nonce;
    public byte public_type = -1;
    //content
    public byte[] signature;
    public byte[] content;
    //extras
    public boolean verified = false;
    public boolean verifying = false;
    //decrypted content
    public boolean readable = false;
    public byte[] decryptedContent;
    protected Channel channel;
    public int database_Id = -1;

    public RawMsg(ECKey key, long timestamp, int nonce) {
        this.key = key;
        this.timestamp = timestamp;
        this.nonce = nonce;
    }

    public RawMsg(ECKey key, long timestamp, int nonce, byte[] signature, byte[] content, byte[] decryptedContent, Channel channel, boolean verified, boolean readable, int database_Id) {
        this.key = key;
        this.timestamp = timestamp;
        this.nonce = nonce;
        this.signature = signature;
        this.content = content;
        this.decryptedContent = decryptedContent;
        this.channel = channel;
        this.verified = verified;
        this.readable = readable;
        this.database_Id = database_Id;
    }

    public RawMsg(long timestamp, int nonce, byte[] signature, byte[] content, boolean verified) {
        this.timestamp = timestamp;
        this.nonce = nonce;
        this.signature = signature;
        this.content = content;
        this.verified = verified;
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof RawMsg)) {
            return false;
        }

        RawMsg m2 = (RawMsg) obj;

        if (timestamp == m2.timestamp && key.equals(m2.key) && public_type == m2.public_type && nonce == m2.nonce) {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + (this.key != null ? this.key.hashCode() : 0);
        hash = 67 * hash + (int) (this.timestamp ^ (this.timestamp >>> 32));
        hash = 67 * hash + this.nonce;
        return hash;
    }

    public byte[] getContent() {
        return content;
    }

    public ECKey getKey() {
        return key;
    }

    public int getNonce() {
        return nonce;
    }

    public byte[] getSignature() {
        return signature;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void sign() {
        verified = true;
        //String toSign = "" + timestamp + "" + nonce + "" + new String(content, "UTF-8");

        byte[] toSign = new byte[1 + 8 + 4 + content.length];
        ByteBuffer wrap = ByteBuffer.wrap(toSign);
        wrap.put(public_type);
        wrap.putLong(timestamp);
        wrap.putInt(nonce);
        wrap.put(content);

        Sha256Hash hash = Sha256Hash.create(toSign);
        ECDSASignature sign = key.sign(hash);

        byte[] encodeToDER = new byte[SIGNATURE_LENGRTH];
        byte[] sigBytes = sign.encodeToDER();
        System.arraycopy(sigBytes, 0, encodeToDER, 0, sigBytes.length);


        byte[] newBytes = new byte[encodeToDER.length];


        int index = encodeToDER.length - 1;
        while (true) {
            System.arraycopy(encodeToDER, 0, newBytes, 0, index + 1);
            if (newBytes[index] == (byte) 0) {
                newBytes = new byte[index];

                index--;
            } else {
                break;
            }

        }


        //System.out.println("Sigbytes len: " + sigBytes.length + " " + Utils.bytesToHexString(encodeToDER));
        //System.out.println("Sigbytes len: " + sigBytes.length + " " + Utils.bytesToHexString(newBytes));

        signature = encodeToDER;

        //System.out.println("len: " + encodeToDER.length);

        //System.out.println("len: " + encodeToDER.length);


        //        Sha256Hash hash = Sha256Hash.create(toSign.getBytes());
        //        ECDSASignature sign = key.sign(hash);
        //        
        //        signature = sign.encodeToDER();

        //forces 72 bytes...
        //signature = new byte[SIGNATURE_LENGRTH];
        //byte[] sigBytes = sign.encodeToDER();
        //        signature = sign.encodeToDER();

        //System.out.println("Signature1 length : " + Utils.bytesToHexString(signature));
        //System.arraycopy(sigBytes, 0, signature, 0, sigBytes.length);


        //System.out.println("Signed: contentlen:" + content.length + " key: " + Utils.bytesToHexString(content) +new String(Base64.encode(key.getPubKey())) + " signature: " + new String(Base64.encode(signature)) + " time: " + timestamp + " nonce: " + nonce + " content: " + new String(Base64.encode(content)));
        //        System.out.println("Signed: contentlen:" + content.length + " key: " + Base58.encode(key.getPubKey()) + " signature: " + Utils.bytesToHexString(signature) + " time: " + timestamp + " nonce: " + nonce + " content: " + Utils.bytesToHexString(content));

        //        System.out.println("Signed: contentlen:" + content.length + " key: " + Base58.encode(key.getPubKey()) + " signature: " + Utils.bytesToHexString(signature) + " time: " + timestamp + " nonce: " + nonce + " content: " + Utils.bytesToHexString(content));


        //        ECKey asdf = new ECKey(null, key.getPubKey(), true);
        //        try {
        //            asdf.verifyMessage(toSign, new String(Base64.encode(signature)));
        //        } catch (SignatureException ex) {
        //            System.out.println("dbhjadgwjgewzj");
        //            Logger.getLogger(RawMsg.class.getName()).log(Level.SEVERE, null, ex);
        //        }

        //        System.out.println("laenge::: " + signature.length);
        //        System.out.println("test...");
        //        try {
        //            //key.verifyMessage(toSign, new String(Base64.encode(Base64.decode(signMessage))));
        //            key.verifyMessage("" + timestamp + "" + nonce + "" + content, new String(Base64.encode(signature)));
        //        } catch (SignatureException ex) {
        //            ex.printStackTrace();
        //        }



    }

    public boolean verify() {
        byte[] encodeToDER = signature.clone();
        byte[] newBytes = new byte[encodeToDER.length];
        int index = encodeToDER.length - 1;
        while (true) {
            System.arraycopy(encodeToDER, 0, newBytes, 0, index + 1);
            if (newBytes[index] == (byte) 0) {
                newBytes = new byte[index];

                index--;
            } else {
                break;
            }

        }


        //String toVerify = "" + timestamp + "" + nonce + "" + new String(content, "UTF-8");
        //boolean verify = key.verify(content, signature);

        byte[] toVerify = new byte[1 + 8 + 4 + content.length];
        ByteBuffer wrap = ByteBuffer.wrap(toVerify);
        wrap.put(public_type);
        wrap.putLong(timestamp);
        wrap.putInt(nonce);
        wrap.put(content);

        Sha256Hash hash2 = Sha256Hash.create(toVerify);
        //boolean verify = key.verify(hash2.getBytes(), newBytes);
        boolean verify = key.verify(hash2.getBytes(), encodeToDER);

        //System.out.println("signature len: " + newBytes.length + " " + Utils.bytesToHexString(newBytes));

        //System.out.println("" + verify);

        //System.out.println("" + verify);
        //
        //        boolean verify = false;
        //        try {
        //            verify = key.verify(toVerify.getBytes(), signature);
        //        } catch (RuntimeException e) {
        //            e.printStackTrace();
        //        }

        return verify;
    }

    public RawMsg toSpecificMsgType() {
        getChannel();//searches channel

        if (channel == null) {
            return this;
        }

        decrypt();


        //Wrong key?
        if (decryptedContent == null || decryptedContent.length == 0) {
            System.out.println("Wrong crypt key for channel '" + channel.getName() + "'.");
            return this;
            //throw new RuntimeException("Wrong crypt key for channel");
        }

        //Check for plain text
        if (decryptedContent[0] == TextMsg.BYTE) {
            TextMsg textMsg = new TextMsg(key, timestamp, nonce, signature, content, decryptedContent, channel, verified, readable, database_Id);
            return textMsg;
        } else //Check for deliveredMsg
        if (decryptedContent[0] == DeliveredMsg.BYTE) {
            DeliveredMsg deliveredMsg = new DeliveredMsg(key, timestamp, nonce, signature, content, decryptedContent, channel, verified, readable, database_Id);
            return deliveredMsg;
        } else //Check for ControlMsg
        if (decryptedContent[0] == ControlMsg.BYTE) {
            ControlMsg controlMsg = new ControlMsg(key, timestamp, nonce, signature, content, decryptedContent, channel, verified, readable, database_Id);
            return controlMsg;
        }

        return this;

    }

    public void encrypt() {
        content = AESCrypt.encode(key, timestamp, decryptedContent);
    }

    public void decrypt() {
        decryptedContent = AESCrypt.decode(key, timestamp, content);
    }

    public Channel getChannel() {
        if (channel != null) {
            return channel;
        }

        Channel findBytePublicKey = Channel.findBytePublicKey(key);
        channel = findBytePublicKey;

        if (channel != null) {
            //Update to new found key, now we should have the private in it!
            key = channel.getKey();
            readable = true;
        }


        return channel;

    }

    @Override
    public int compareTo(RawMsg o) {
        return (int) (timestamp - o.timestamp);
    }

    @Deprecated
    public RawMsg clone() throws CloneNotSupportedException {
        RawMsg rawMsg = new RawMsg(key, timestamp, nonce, signature, content, decryptedContent, channel, verified, readable, database_Id);
        rawMsg.public_type = public_type;
        return rawMsg;
    }
}
