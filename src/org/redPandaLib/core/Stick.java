/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import crypt.Utils;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.SpecialChannels;
import org.redPandaLib.crypt.Sha256Hash;

/**
 *
 * @author rflohr
 */
public class Stick {

    public static final double DIFFICULTY = 2.4;
    byte[] pubkey;
    long timestamp;
    int nonce;
    //int difficulty;

    public Stick(byte[] pubkey, long timestamp, int nonce) {
        this.pubkey = pubkey;
        this.timestamp = timestamp;
        this.nonce = nonce;
    }

    public static void main(String[] args) throws Exception {
        generate(SpecialChannels.MAIN.key.getPubKey(), 0);
    }

    public static Stick generate(byte[] pubkeyBytes, int startNonce) {
        try {
            long timestamp = System.currentTimeMillis();

            String pubkey = new String(pubkeyBytes, "UTF-8");

            String toHash;
            boolean correkt;

            int cnt = 0;

            int toCheckBytes = (int) Math.floor(DIFFICULTY);
            int nextByteMin = (int) Math.ceil((1 - (DIFFICULTY - toCheckBytes)) * 256.);

//            System.out.println("toCheckBytes:" + toCheckBytes);
//            System.out.println("NEXTBYTESIZE: " + nextByteMin);

            int nonce = startNonce;
            while (true) {
                nonce++;
                toHash = pubkey + timestamp + nonce;
                Sha256Hash createDouble = Sha256Hash.createDouble(toHash.getBytes("UTF-8"));
                byte[] bytes = createDouble.getBytes();



                correkt = true;
                for (int i = 0; i < toCheckBytes; i++) {
                    if (bytes[i] != 0) {
                        correkt = false;
                        break;
                    }
                }

                int nextIndex = (bytes[toCheckBytes] & 0xFF);

                if (nextIndex > nextByteMin) {
                    correkt = false;
                }

                cnt++;









                if (correkt) {
                    Stick stick = new Stick(pubkeyBytes, timestamp, nonce);
//                    System.out.println(cnt + " " + correkt + " " + Utils.bytesToHexString(bytes) + " " + nextIndex + " difficulty: " + stick.getDifficulty());
                    return stick;
                }


                if (cnt % 500000 == 0) {
                    timestamp = System.currentTimeMillis();
//                    System.out.println("cnt: " + cnt);
                }


            }
        } catch (UnsupportedEncodingException ex) {
            new RuntimeException(ex);
        }

        return null;
    }

    public double getDifficulty() {



        byte[] hash = getHash();


        int diffOffset = 0;
        while (true) {
            if (hash[diffOffset] != 0) {
                break;
            }
            diffOffset++;
        }

        int nextIndex = (hash[diffOffset] & 0xFF);

//        System.out.println("hash: " + Utils.bytesToHexString(hash));


        return diffOffset + (1 - nextIndex / 256.);

    }

    public byte[] getHash() {
        try {
            String toHash = new String(pubkey, "UTF-8");
            toHash += timestamp;
            toHash += nonce;

            byte[] hash = Sha256Hash.createDouble(toHash.getBytes("UTF-8")).getBytes();
            return hash;
        } catch (UnsupportedEncodingException ex) {
            new RuntimeException(ex);
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }


        if (!(obj instanceof Stick)) {
            return false;
        }

        Stick otherStick = (Stick) obj;

        return (Arrays.equals(pubkey, otherStick.pubkey) && timestamp == otherStick.timestamp && nonce == otherStick.nonce);


    }
}
