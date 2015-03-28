/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import org.redPandaLib.crypt.AddressFormatException;
import org.redPandaLib.crypt.Base58;
import org.redPandaLib.crypt.ECKey;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.Main;
import org.redPandaLib.crypt.Sha256Hash;

/**
 *
 * @author rflohr
 */
public class Channel implements Serializable, Comparable<Channel> {

    private static final long serialVersionUID = -4056734674983489374L;
    protected int id = -1;
    protected ECKey key;
    protected String securityHash;
    protected String name;
    protected double diffuculty = 0;
    public long displayPriority = 0;

    public String getName() {
        return name;
    }
    boolean pub = false;

    public Channel() {
    }

    public Channel(ECKey key, String name) {
        this.key = key;
        this.name = name;
    }

    public Channel(ECKey key, String name, int id) {
        this(key, name);
        this.id = id;
    }

    public static Channel generateNew(String name) {
        Channel newIdentity = new Channel();

        newIdentity.key = new ECKey();

        System.out.println("priv key: " + byte2String(newIdentity.key.getPrivKeyBytes()));
        System.out.println("pub key: " + byte2String(newIdentity.key.getPubKey()));

        newIdentity.name = name;

        newIdentity.addToList();

        return newIdentity;

    }

    public static Channel generateNewPublic(String name) {
        //        Channel newIdentity = new Channel();
        //
        //
        //        while (true) {
        //            newIdentity.key = new ECKey();
        //
        //            //System.out.println("kkk");
        //
        //            boolean wrong = false;
        //
        //            //System.out.println("" + byte2String(newIdentity.key.getPubKeyHash()));
        //
        //            //System.out.println("" + newIdentity.key.getPubKey()[1]);
        //
        ////            for (int i = 1; i < 2; i++) {
        ////
        ////
        ////
        ////                if (newIdentity.key.getPubKey()[i] != (byte) 0) {
        ////                    wrong = true;
        ////                }
        ////            }
        //
        //            if (!byte2String(newIdentity.key.getPubKey()).matches("pub(.*)")) {
        //                wrong = true;
        //            }
        //
        //            if (!wrong) {
        //                break;
        //            }
        //
        //        }
        //
        //
        //
        //        System.out.println("priv key: " + byte2String(newIdentity.key.getPrivKeyBytes()));
        //        System.out.println("pub key: " + byte2String(newIdentity.key.getPubKey()));
        //
        //        newIdentity.name = name;

        Channel generateNew = generateNew(name);
        generateNew.pub = true;
        return generateNew;

    }

    public String stringIdentity() {
        return byte2String(key.getPubKey());
    }

    public String getPrivateKey() {
        return byte2String(key.getPrivKeyBytes());
    }

    public static Channel getInstanceByPublicKey(String publicKey) {
        try {
            return getInstanceByPublicKey(string2Byte(publicKey));
        } catch (IOException ex) {
            Logger.getLogger(Channel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static Channel getInstanceByPublicKey(byte[] publicKey) {
        Channel channel = new Channel();
        channel.key = new ECKey(BigInteger.ZERO, publicKey, false);
        channel.name = "unknown";

        for (Channel c : Test.getChannels()) {

            if (channel.equals(c)) {
                return c;
            }

        }

        return channel;
    }

    public static Channel getInstaceByPrivateKey(String privKeyBase58, String name, int id) {
        try {
            ECKey eCKey = new ECKey(new BigInteger(1, Base58.decode(privKeyBase58)), null, true);

            return new Channel(eCKey, name, id);
        } catch (AddressFormatException ex) {
        }

        return null;
    }

    public static String byte2String(byte[] b) {
//        BASE64Encoder basE64Encoder = new BASE64Encoder();
//        String encode = basE64Encoder.encode(b);
//        String replaceAll = encode.replaceAll("\n", "");
//        return replaceAll;

        //return Base64.encodeToString(b, false);
        return Base58.encode(b);

    }

    public static byte[] string2Byte(String in) throws IOException {
        try {
            //        BASE64Decoder basE64Decoder = new BASE64Decoder();
            //        return decodeBuffer;
            //        return decodeBuffer;

            //return Base64.decode(in);
            return Base58.decode(in);
        } catch (AddressFormatException ex) {
            Logger.getLogger(Channel.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;

    }

    public String sign(String str) {
        return key.signMessage(str);
    }

    public boolean verifyString(String str, String signature) {
        try {
            key.verifyMessage(str, signature);
            return true;
        } catch (SignatureException ex) {
        }
        return false;
    }

    public String encrypt(String in) {
        String out = null;
        return out;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Channel) {
            Channel o = (Channel) obj;
            return key.equals(o.key);
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + (this.key != null ? this.key.hashCode() : 0);
        return hash;
    }

    public ECKey getKey() {
        return key;
    }

    @Override
    public String toString() {
        return name;
    }

    public int getId() {
        return id;
    }

    public static Channel getChannelById(int id) {
        for (Channel c : Test.getChannels()) {

            if (c.id == id) {
                return c;
            }

        }
        return null;
    }

    public static int getNextId() {
        LocalSettings localSettings = Test.getLocalSettings();

        int out;

        while (true) {

            localSettings.channelIdCounter++;
            out = localSettings.channelIdCounter;

            boolean inUse = false;
            for (Channel c : Main.getChannels()) {
                if (c.id == out) {
                    inUse = true;
                    break;
                }
            }

            if (!inUse) {
                break;
            }

        }

        localSettings.save();
        return out;
    }

    public void setName(String name) {
        this.name = name;
        Test.saver.saveIdentities(Test.channels);
    }

    public String exportForHumans() {

        String prefix = "";

        byte[] mainBytes;

        if (pub) {
            prefix = "pu";
            mainBytes = key.getPubKey();
        } else {
            prefix = "pr";
            mainBytes = key.getPrivKeyBytes();
        }

        ByteBuffer buffer = ByteBuffer.allocate(100);

        buffer.put(prefix.getBytes());
        buffer.put(mainBytes);
        buffer.flip();

        byte[] b = new byte[buffer.remaining()];

        buffer.get(b);
        Sha256Hash hash = Sha256Hash.createDouble(b);
        byte[] bytes = hash.getBytes();

        byte[] checksum = new byte[8];

        for (int i = 0; i < checksum.length; i++) {
            checksum[i] = bytes[i];
        }

        byte[] outBytes = new byte[mainBytes.length + checksum.length];

        System.arraycopy(mainBytes, 0, outBytes, 0, mainBytes.length);
        System.arraycopy(checksum, 0, outBytes, mainBytes.length, checksum.length);

        String out = prefix;

        out += Base58.encode(outBytes);
        //out += "-";
        //out += Base58.encode(checksum);

        return out;

    }

    public static Channel importFromHuman(String address, String name) throws AddressFormatException {

        if (address.length() < 4) {
            return null;
        }

        address = address.replaceAll(" ", "");

        String prefix = address.substring(0, 2);
        System.out.println("Prefix: " + prefix);

        boolean pub = (prefix.equals("pu"));

        String remaining = address.substring(2, address.length());
        System.out.println("rest: " + remaining);
        byte[] bytes = Base58.decode(remaining);

        if (pub) {

            byte[] publicKeyBytes = new byte[33];
            System.arraycopy(bytes, 0, publicKeyBytes, 0, publicKeyBytes.length);
            ECKey ecKey = new ECKey(null, publicKeyBytes);
            Channel channel = new Channel(ecKey, name);
            channel.pub = true;

            System.out.println("public channel: " + channel.exportForHumans());

            System.out.println("given:          " + address);

            if (!channel.exportForHumans().equals(address)) {
                return null;
            }

            return channel;

        } else {
            byte[] privateBytes = new byte[32];
            System.arraycopy(bytes, 0, privateBytes, 0, privateBytes.length);
            //ECKey ecKey = new ECKey(privateBytes, null);
            ECKey ecKey = new ECKey(new BigInteger(1, privateBytes), null, true);
            Channel channel = new Channel(ecKey, name);
            channel.pub = false;

            System.out.println("private channel: " + channel.exportForHumans());

            if (!channel.exportForHumans().equals(address)) {
                return null;
            }

            return channel;
        }

    }

    public static Channel findBytePublicKey(ECKey key) {

        for (Channel c : Test.channels) {
            if (c.key.equals(key)) {
                return c;
            }
        }

        return null;
    }

    public boolean addToList() {
        id = Channel.getNextId();

        new Thread() {

            @Override
            public void run() {
                if (Test.peerList != null) {
                    for (Peer p : Test.getClonedPeerList()) {
                        p.disconnect("added new channel");
                    }
                    Test.triggerOutboundthread();
                }
            }

        }.start();

        return Test.addChannel(this);
    }

    @Override
    public int compareTo(Channel o) {

        return (int) ((int) diffuculty - o.diffuculty);

    }

    public void setId(int id) {
        this.id = id;
    }
}
