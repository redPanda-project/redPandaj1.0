/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.crypt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author rflohr
 */
public class AESstreamcipher {
    
    private byte[] pass;
    private byte[] iv;
    
    
    
    
      public static byte[] encodeCTR(byte[] bytes, byte[] pass, long ivbytes) throws Exception {

        byte[] ivBytes2 = new byte[16];
        ByteBuffer buffer = ByteBuffer.wrap(ivBytes2);
        buffer.putLong(12312L);
        buffer.putLong(ivbytes);

        IvParameterSpec iv = new IvParameterSpec(ivBytes2, 0, 16);

        System.out.println("iv: " + Utils.bytesToHexString(ivBytes2));

        Cipher c = Cipher.getInstance("AES/CTR/NoPadding");
        Key k = new SecretKeySpec(pass, "AES");
        c.init(Cipher.ENCRYPT_MODE, k, iv);

        ByteArrayOutputStream encodedBytes = new ByteArrayOutputStream();
        OutputStream cos = new CipherOutputStream(encodedBytes, c);
        cos.write(bytes);
        cos.close();

        System.out.println("new: " + Utils.bytesToHexString(c.getIV()));

        return encodedBytes.toByteArray();
    }

    public static byte[] decodeCTR(byte[] toDecode, byte[] pass, long ivbytes) throws Exception {

        byte[] ivBytes2 = new byte[16];
        ByteBuffer buffer = ByteBuffer.wrap(ivBytes2);
        buffer.putLong(12312L);
        buffer.putLong(ivbytes);

        IvParameterSpec iv = new IvParameterSpec(ivBytes2, 0, 16);

        Cipher c = Cipher.getInstance("AES/CTR/NoPadding");
        Key k = new SecretKeySpec(pass, "AES");
        c.init(Cipher.DECRYPT_MODE, k, iv);

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(toDecode);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CipherInputStream cis = new CipherInputStream(byteArrayInputStream, c);

        for (int b; (b = cis.read()) != -1;) {
            bos.write(b);
        }

        cis.close();

        return bos.toByteArray();
    }
    
}
