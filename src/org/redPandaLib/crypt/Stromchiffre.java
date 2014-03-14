/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.crypt;

import java.security.Provider;
import java.security.Security;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.spongycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author rflohr
 */
public class Stromchiffre {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {
        //Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());



        Provider[] providers = Security.getProviders();
        for (int i = 0; i < providers.length; i++) {
            System.out.println("Provider: " + providers[i].toString());
        }


        byte[] input = "input".getBytes();
        byte[] keyBytes = "input123".getBytes();

        SecretKeySpec key = new SecretKeySpec(keyBytes, "ARC4");

        Cipher cipher = Cipher.getInstance("AES", "SC");
        //Cipher cipher = Cipher.getInstance("ARC4");

        byte[] cipherText = new byte[input.length];

        cipher.init(Cipher.ENCRYPT_MODE, key);

        int ctLength = cipher.update(input, 0, input.length, cipherText, 0);

        ctLength += cipher.doFinal(cipherText, ctLength);

        System.out.println("cipher text: " + new String(cipherText));

        byte[] plainText = new byte[ctLength];

        cipher.init(Cipher.DECRYPT_MODE, key);

        int ptLength = cipher.update(cipherText, 0, ctLength, plainText, 0);

        ptLength += cipher.doFinal(plainText, ptLength);

        System.out.println("plain text : " + new String(plainText));
    }
}
