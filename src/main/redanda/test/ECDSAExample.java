/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import javax.crypto.spec.SecretKeySpec;

public class ECDSAExample {

    public static void main(String[] args) throws Exception {
        /*
         * Generate an ECDSA signature
         */

        /*
         * Generate a key pair
         */
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

        keyGen.initialize(256, random);

        KeyPair pair = keyGen.generateKeyPair();
        PrivateKey priv = pair.getPrivate();
        PublicKey pub = pair.getPublic();

        /*
         * Create a Signature object and initialize it with the private key
         */
        Signature dsa = Signature.getInstance("SHA256withECDSA");

        dsa.initSign(priv);

        String str = "This is string to sign";
        byte[] strByte = str.getBytes("UTF-8");
        dsa.update(strByte);

        /*
         * Now that all the data to be signed has been read in, generate a
         * signature for it
         */
        byte[] realSig = dsa.sign();
        System.out.println("Signature: " + new BigInteger(1, realSig).toString(16));

        System.out.println("pub byte len: " + pub.getEncoded().length);
        
        SecretKeySpec secretKeySpec = new SecretKeySpec(pub.getEncoded(), "SHA256withECDSA");
        
        for (int cnt = 0; cnt < 1000; cnt++) {
            dsa.initVerify(pub);
            dsa.update(strByte);
            dsa.verify(realSig);
            //System.out.println(dsa.verify(realSig));
        }

    }
}
