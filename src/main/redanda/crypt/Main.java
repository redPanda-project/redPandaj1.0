/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.crypt;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Random;

/**
 *
 * @author rflohr
 */
public class Main {

    public static void main(String[] args) throws SignatureException, IOException {

//        System.out.println("" + System.currentTimeMillis());
//        
//        
//        ECKey ecKey = new ECKey();
//        
//        System.out.println("pub: " + Base58.encode(ecKey.getPubKey()));
//        System.out.println("priv: " + Base58.encode(ecKey.getPrivKeyBytes()));
//        
//        
//        String b = "jwbhfihwbfwyebgfwehfbvweufgvweufgvweufgvwefgvweufvweufnxyugnwxeufygmusfmuwugnwxeufygmusfmuwugnwxeufygmusfmuw";
//        
//        long start = System.currentTimeMillis();
//        
//        long bytes = 0;
//        String signMessage = ecKey.signMessage(b);
//        
//        
//        
//        while (bytes < 35000) {
//            
//            
//            
//            bytes += b.length();
//            //byte[] string2Byte = Identity.string2Byte(b);
//            
//            //Identity.byte2String(string2Byte);
//            
//            ecKey.verifyMessage(b, signMessage);
//            
//            System.out.println("bytes: " + bytes + " avg.: " + (bytes / ((System.currentTimeMillis() - start)*1.0))*1000/1024);
//        }


        byte[] key = new byte[32];
        Random r = new SecureRandom();
        r.nextBytes(key);


        byte[] text = "asddwdwdwd".getBytes();
        byte[] encode = AESCrypt.encode(key, text);
        
        System.out.println(Utils.bytesToHexString(encode) + " " + encode.length);
        
        byte[] toDecode = new byte[encode.length * 2];
        
        System.arraycopy(encode, 0, toDecode, 0, 16);
        System.arraycopy(encode, 0, toDecode, 16, 16);
        
        System.out.println("decodeysize_: " + toDecode.length + " " + Utils.bytesToHexString(toDecode));
        
        String string = new String(AESCrypt.decode(key, toDecode));
        
        System.out.println(string + " " + string.length());
        
        

    }
}
