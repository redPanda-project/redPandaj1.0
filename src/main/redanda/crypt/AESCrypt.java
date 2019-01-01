/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.crypt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.Key;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author robin
 */
public class AESCrypt {

    private static final int AES_BLOCK_SIZE = 16;

    public static void main(String[] args) {
        try {
            ECKey ecKey = new ECKey();

            String msg = "hihi";

            byte[] pass = ecKey.getPrivKeyBytes();
            IvParameterSpec iv = new IvParameterSpec(pass, 0, 16);

            ByteArrayOutputStream encodedBytes = new ByteArrayOutputStream();
            encode(msg.getBytes(), encodedBytes, pass, iv);

            System.out.println("encrypted: " + Utils.bytesToHexString(encodedBytes.toByteArray()));

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(encodedBytes.toByteArray());
            String string = new String(decode(byteArrayInputStream, pass, iv));

            System.out.println("" + string);

        } catch (Exception ex) {
            Logger.getLogger(TestMain.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    //ToDo: change IV to not inlcude pass but other 8 'random' bytes?
    public static byte[] encode(byte[] pass, long timestamp, byte[] toEncode) {
        try {
            byte[] ivBytes = new byte[16];
            ByteBuffer buffer = ByteBuffer.wrap(ivBytes);
            buffer.putLong(timestamp);
            buffer.put(pass, 0, 8);

            IvParameterSpec iv = new IvParameterSpec(ivBytes, 0, 16);

            //iv = new IvParameterSpec(new byte[16]);
            ByteArrayOutputStream encodedBytes = new ByteArrayOutputStream();
            encode(toEncode, encodedBytes, pass, iv);

            return encodedBytes.toByteArray();
        } catch (Exception ex) {
            Logger.getLogger(AESCrypt.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;

    }

    public static byte[] encode(byte[] key, byte[] toEncode) {
        try {
            byte[] pass = key;
            IvParameterSpec iv = new IvParameterSpec(pass, 0, 16);
            ByteArrayOutputStream encodedBytes = new ByteArrayOutputStream();
            encode(toEncode, encodedBytes, pass, iv);

            return encodedBytes.toByteArray();
        } catch (Exception ex) {
            Logger.getLogger(AESCrypt.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;

    }

    public static void encode(byte[] bytes, OutputStream out, byte[] pass, IvParameterSpec iv) throws Exception {

        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        Key k = new SecretKeySpec(pass, "AES");
        c.init(Cipher.ENCRYPT_MODE, k, iv);

//        AlgorithmParameters params = c.getParameters();
        //byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        OutputStream cos = new CipherOutputStream(out, c);
        cos.write(bytes);
        cos.close();
    }

    public static byte[] decode(byte[] pass, long timestamp, byte[] toDecode) {
        try {
            byte[] ivBytes = new byte[16];
            ByteBuffer buffer = ByteBuffer.wrap(ivBytes);
            buffer.putLong(timestamp);
            buffer.put(pass, 0, 8);
            IvParameterSpec iv = new IvParameterSpec(ivBytes, 0, 16);

            //iv = new IvParameterSpec(new byte[16]);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(toDecode);
            byte[] decode = decode(byteArrayInputStream, pass, iv);

            //System.out.println("dnjwadhwad " + toDecode.length + " " + decode.length);
            return decode;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static byte[] decode(byte[] key, byte[] toDecode) {
        try {
            byte[] pass = key;
            IvParameterSpec iv = new IvParameterSpec(pass, 0, 16);

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(toDecode);
            byte[] decode = decode(byteArrayInputStream, pass, iv);

            return decode;

        } catch (Exception ex) {
            Logger.getLogger(AESCrypt.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static byte[] decode(InputStream is, byte[] pass, IvParameterSpec iv) throws Exception {

        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        Key k = new SecretKeySpec(pass, "AES");
        c.init(Cipher.DECRYPT_MODE, k, iv);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CipherInputStream cis = new CipherInputStream(is, c);

        for (int b; (b = cis.read()) != -1;) {
            bos.write(b);
        }

        cis.close();

        return bos.toByteArray();
    }

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
