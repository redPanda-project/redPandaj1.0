/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;

public class RSATest {

    public static void main(String[] args) throws Exception {
//        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
//
//        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "SC");
//
//        keyGen.initialize(2048, new SecureRandom());
//
//        KeyPair keyPair = keyGen.generateKeyPair();
//
//        System.out.println("priv key length: " + keyPair.getPrivate().getEncoded().length);
//        System.out.println("pub key length: " + keyPair.getPublic().getEncoded().length);
//
//        Signature signature = Signature.getInstance("SHA256withRSA", "SC");
//
//        signature.initSign(keyPair.getPrivate(), new SecureRandom());
//
//        byte[] message = "a".getBytes();
//        signature.update(message);
//
//        byte[] sigBytes = signature.sign();
//
//        for (int cnt = 0; cnt < 70000; cnt++) {
//            signature.initVerify(keyPair.getPublic());
//            signature.update(message);
//            signature.verify(sigBytes);
//            //System.out.println();
//        }
    }
}
