/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package crypt;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
 * @author robin
 */
public class TestMain {

    public static void main(String[] args) {

        byte[] asdff = new byte[8];
        
        ByteBuffer asd = ByteBuffer.wrap(asdff);
        asd.putLong(88L);


        byte[] haus = new byte[7];
        
        ByteBuffer h = ByteBuffer.wrap(haus);
        
        System.out.println(" " + h.getLong());
        
        
        //        ECKey ecKey = new ECKey();
        //
        //        long start = System.currentTimeMillis();
        //
        //        long bytes = 0;
        //
        //        ECDSASigner signer = new ECDSASigner();
        //        ECPublicKeyParameters params = new ECPublicKeyParameters(ecKey.ecParams.getCurve().decodePoint(ecKey.pub), ecKey.ecParams);
        //        signer.init(false, params);
        //
        //        String a = "fjhfrejfhredfjhfrejfhredfjhfrejfhredfjhfrejfhredfjhfrejfhredfjhfrejfhredfjhfrejfhred";
        //        ECDSASignature sign = ecKey.sign(Sha256Hash.create(a.getBytes()));
        //        
        //        
        //        while (bytes < 200) {
        //
        //
        //
        //            bytes++;
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //            boolean verifySignature = false;
        //
        //            try {
        //                verifySignature = signer.verifySignature(Sha256Hash.create(a.getBytes()).getBytes(), sign.r, sign.s);
        //            } catch (NullPointerException e) {
        //                // Bouncy Castle contains a bug that can cause NPEs given specially crafted signatures. Those signatures
        //                // are inherently invalid/attack sigs so we just fail them here rather than crash the thread.
        //                System.err.println("Caught NPE inside bouncy castle: " + e);
        //                e.printStackTrace();
        //            }
        //
        //            //System.out.println("" + verifySignature);
        //
        //        System.out.println("bytes: " + bytes + " avg.: " + (bytes / ((System.currentTimeMillis() - start) * 1.0)) * 1000);
        //        System.out.println("bytes: " + bytes + " avg.: " + (bytes / ((System.currentTimeMillis() - start) * 1.0)) * 1000);

//        try {
//            ECKey ecKey = new ECKey();
//
//            String msg = "hihi";
//
//            byte[] pass = ecKey.getPrivKeyBytes();
//            IvParameterSpec iv = new IvParameterSpec(pass,0,16);
//
//            ByteArrayOutputStream encodedBytes = new ByteArrayOutputStream();
//            encode(msg.getBytes(), encodedBytes, pass, iv);
//
//
//            System.out.println("encrypted: " + Utils.bytesToHexString(encodedBytes.toByteArray()));
//
//            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(encodedBytes.toByteArray());
//            String string = new String(decode(byteArrayInputStream, pass, iv));
//
//            System.out.println("" + string);
//
//        } catch (Exception ex) {
//            Logger.getLogger(TestMain.class.getName()).log(Level.SEVERE, null, ex);
//        }

    }

    static void encode(byte[] bytes, OutputStream out, byte[] pass, IvParameterSpec iv) throws Exception {

        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        Key k = new SecretKeySpec(pass, "AES");
        c.init(Cipher.ENCRYPT_MODE, k, iv);


//        AlgorithmParameters params = c.getParameters();
        //byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();

        OutputStream cos = new CipherOutputStream(out, c);
        cos.write(bytes);
        cos.close();

    }

    static byte[] decode(InputStream is, byte[] pass, IvParameterSpec iv) throws Exception {

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
}
