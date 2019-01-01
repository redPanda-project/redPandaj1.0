/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.test;

import crypt.Utils;
import java.nio.ByteBuffer;

import static main.redanda.core.messages.RawMsg.SIGNATURE_LENGRTH;
import main.redanda.crypt.ECKey;
import main.redanda.crypt.Sha256Hash;

/**
 *
 * @author robin
 */
public class ECVerificationTest {

    public static void main(String[] args) {

        ECKey key = new ECKey();
        System.out.println("Key successful generated");

        byte[] toSign = new byte[20];
        ByteBuffer wrap = ByteBuffer.wrap(toSign);
        wrap.put("testest".getBytes());

        Sha256Hash hash = Sha256Hash.create(toSign);
        ECKey.ECDSASignature sign = key.sign(hash);

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
        byte[] signature = encodeToDER;

        System.out.println("signature: " + Utils.bytesToHexString(signature));

        newBytes = new byte[encodeToDER.length];
        index = encodeToDER.length - 1;
        while (true) {
            System.arraycopy(encodeToDER, 0, newBytes, 0, index + 1);
            if (newBytes[index] == (byte) 0) {
                newBytes = new byte[index];

                index--;
            } else {
                break;
            }

        }

        System.out.println("try to verify....");

        long cnt = 1;
        long starttime = System.currentTimeMillis();
        
        while (cnt < 50000) {
        
            if (cnt%500==0) {
                 System.out.println((double)cnt/(System.currentTimeMillis()-starttime)*1000 + " overall: " + cnt);
            }
            
        Sha256Hash hash2 = Sha256Hash.create(toSign);
        //boolean verify = key.verify(hash2.getBytes(), newBytes);
        boolean verify = key.verify(hash2.getBytes(), encodeToDER);
        cnt++;
            
            //System.out.println("" + cnt);
        
        }


    }

}
