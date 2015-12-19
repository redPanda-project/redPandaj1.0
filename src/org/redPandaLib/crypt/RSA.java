/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.crypt;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 *
 * @author rflohr
 */
public class RSA {

    public static final String Algorithm = "RSA";
    //AES/CBC/PKCS5Padding
    private static KeyPair keyPair;
    private static String privateString = "3Z2KzWG796gAE2zUz9Tuy3mBvA2sDfdSwPUqgwzS8pqnYdYrh2jY6f6dTqYGcaBAkX8nW2ZbahuW165ZsjrszkKhY1gTrMT3HrdnQXqxb5Y6b5CWi1rDEv8qFKFKkkduapoqR5Y3eYTdV32MdZ7vaqvaCdwCC6Tm9dx1DHsM8LSemMGYAniMq2bNchtg8SyGx4d4Mq3mp8EPkFCtmb4ZRDh9N2tqTFqDb7jtMErycYhg5XkNCVJBT14aJerYbYMp1FmbEkQ8Mgct6cSehqfo6PMJRC34xj1iJWoayWnu2cAHrBishLFRR3dYRqF72zm9sfyUZ5jtziMAJbM7QU4ZjeidYJAvc7tuf1KTgkteteVCH4nJByNNohLb39S29MuAWhyMHHtZXseYAK6aDM3C8aUcdpXUE7UsgHh7bNsjfC7MSfcTMYKZTNYzxT8oqT2qiC2H5G9FkvbrF6K97Ka5fz7Umz2hFpCQTnKs4yvSHgvxRBqDLL1WPFrQpZGzQsLhreJT4QAqTTkHriFCzJ3rbcb2hnd6Sm84yoV3wZzFq9JjhRYtb5epUzJhUmEMfSo5Tt6PUP9KmQrP5Tw9L5qZ94cmRiUKxFn4W26mUYWW6BUQ6Tx68BwjszXJqV9CFtSJ9e6UyBsjqDNQZousoiktfXFpgizq5nvFhswsWF6YH1qWfDQZLnd4xw3RE8ENGw73VXpxEXjtPzW9Ax9774RCw7kSteFXBMxRtvrYMXK1YDczHcTMqf5VSVZqhednHB4DTgyPm7Zpxum2nmoZavN12UtJVtWAwq3Ypqz5JAUKEpNLfd5zrmbdEipkVfzsc2VfUEBkAGcn6J1QGsYMFUEREyXtM42WxJqRbMjc9MnZ4gZC754HFuehFp7e8PpE4EZay73bYoayme1iECJLiQZ51TNV4n3mMiSV7bkVF8LVkmkdnx3xgqwZUqZYTLGWk9AhyqbG56rFf3GgbHsfAzq4wTcg4QMtpSUzRcgajk4hjPdwngYEjBePUyLXfefUPpaiyLwCngm536v7DVdMn9NGirhh3V5VhBPnsBoYP5GMcZs5Hju4hDGpyYYkZubeGXDJh5vSJsSA6KuQSDK5NSsacsihxoADTvZq1de2MEmwLiaFqXzqrevSuq274yYEBjj5hPwFsLh6Nbk1C3YGSfDkW4zkSBsg5rgYqeTWWChFbWkGLAos8SzhpLiD5qFcboconysv6kpFEuneu4YbWrb1Tj2ii3P2Xbcawr8fbqJMeA1r6qTjEsxZj2o69dNeMnqs5Hf5QLgP7ivbbLguEv8LWzeUF2WWRufbkRPk711a5hLZRUJeMvmPDj1qphx3UvVFEVZhuAdEHkpijHL1nAm5qBXJrYcWdBPAQkBjceEQnZpfcTr5iiAXQcioDNfokuDLKG3uYB1EZVrrDKg5aKQ428KokitVu28QwfBAuksEjHLLh97akpCjjbPEQ6ZEAP9f5gZP5bLcrbG4BK1CkYB3Ns4oyW3R3nvA4r4VnBtCj6d5XuoLGWatCQceqTE6hdhjNgEDaVQ16WV7Yv2aqHHzwHpcwRZgP7XrFHQTb6jmo9hVTBHYw6csWRp1rPfogeXu7nnaBoyjfWZkZ7xrCJPYNEtVw28Zd33rYY1snaiExbrMd3TSZY6cLGaHEdcUn";
    private static String publicString = "2TuPVgMCHJy5atawrsADEzjP7MCVbyyCA89UW6Wvjp9Hr9zt9GFeQZXo3gy5q7Nb2FfsjBJak45W766vwyALkWXWEhAae5HWT3iTPVZLBPUVrrRKbnTgWCNFhpHzSSLEkVu6Wqsr6ZEfChGVa3e2wbE5eS26pfs3WdUxqwBkydQuGdFFtFsqYU8TBgAijTGYtMLUfdTCtWiEN6eft6kHoMrj7dzwacruBwhYnPaP2dNLmZf4NSmcCxD7xKUEPdPPWmrX27LHyXb4zC1CkWo6wdkCVqXzNmS1dBqKbjbFAKrSUp8KwmLiAwx3u68cKjou7ASXBgknVvDcYFzRZyqAfVod9USq2CgyJTFqBd5KdQSy55RpfpooSJmMr6Ng9fDG86Qc3Z4pSj1oiZcSJC";

    /**
     * Erzeugt ein RSA Schluesselpaar
     *
     * @return RSA Schluesselpaar
     * @throws Exception
     */
    public static KeyPair getKeyPair() throws Exception {
        if (keyPair == null) {
            KeyPairGenerator kpg;
            try {
                kpg = KeyPairGenerator.getInstance(Algorithm);
                kpg.initialize(3072);
                keyPair = kpg.generateKeyPair();
            } catch (NoSuchAlgorithmException e) {
                throw new Exception(
                        "Fehler beim Erzeugen des Schluesselpaars: "
                        + e.getMessage());
            }
        }

        return keyPair;
    }

    public static void main(String[] args) throws Exception {

//        for (Provider provider : Security.getProviders()) {
//            System.out.println(provider.getName());
//            for (String key : provider.stringPropertyNames()) {
//                System.out.println("\t" + key + "\t" + provider.getProperty(key));
//            }
//        }

        String toEnc = "ich bin ein sehr toller schluessel";
        KeyPair keyPair1 = getKeyPair();
        byte[] pub = keyPair1.getPublic().getEncoded();
        byte[] priv = keyPair1.getPrivate().getEncoded();


//
//        byte[] pub = Base58.decode(publicString);
//        byte[] priv = Base58.decode(privateString);

//        String encode = Base64.encode(pub);

//        System.out.println("PRIVATE: " + Base58.encode(priv));
//        System.out.println("PUBLIC:  " + Base58.encode(pub));

        //System.out.println("" + encode);

        System.out.println("LEN: " + Base58.encode(pub).length());
        System.out.println("enc: " + Base58.encode(pub));
        System.out.println("LEN: " + Base58.encode(priv).length());
        System.out.println("enc: " + Base58.encode(priv));

        PublicKey publicKey = KeyFactory.getInstance(Algorithm).generatePublic(new X509EncodedKeySpec(pub));

//        Cipher cipher = Cipher.getInstance("RSA");
//        cipher.init(Cipher.ENCRYPT_MODE, keyPair1.getPublic());
//        byte[] update = cipher.doFinal(toEnc.getBytes());
//        cipher.
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        CipherOutputStream cipherOutputStream = new CipherOutputStream(byteArrayOutputStream, cipher);
//        cipherOutputStream.write(toEnc.getBytes());

//        System.out.println("" + update.length);
//
//        System.out.println("enc: " + Base64.encode(update));


        Cipher cipher2 = Cipher.getInstance(Algorithm);
        cipher2.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] update2 = cipher2.doFinal(toEnc.getBytes());

        //System.out.println("enc: " + Base64.encode(update2));


        System.out.println("encrypted: " + Base58.encode(update2));


        update2 = Base58.decode(Base58.encode(update2));

        System.out.println("" + update2.length);


        String a = "4xuCTgb2LwFCgEdBDb3N7bvQTEsSKFk4HuYL1QXVo63gKb5uqRaksJ2AuKExgGdpA3tvTNN3xPosDebQrH8uujf9fyJB4C6S3PSugTLc1CZ6V8RqBmSp6WP2TvyPfQrfPBXC1GykoZVjfsH5PGTzsNM4mdGwBsFJv51g3J51dFFR1dqoh5X7pAWpXSQXchLfvVPqzsKg6nFUwao7HJunyRxtUPAVDrTDxReW3wBuvEhUw3XkGvHJam1JXcm6yBK6QHL2GXbKBqjzKw9TdZ5dS4rK4kmJmLoTd5SMwwb1S4XCU17G86YVihNe3UaGvX3sUL5nrtCmgAS82V3TSeazDpmiLi72EL";

        System.out.println("" + encodeForServer("ich bin ein sehr toller schluessel".getBytes("UTF-8")));

        System.out.println("d: " + decode(Base58.decode(a)));


//        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(priv));
//
//        Cipher cipher3 = Cipher.getInstance("RSA");
//        cipher3.init(Cipher.DECRYPT_MODE, privateKey);
//        byte[] update3 = cipher3.doFinal(update2);
//
//        System.out.println("enc: " + new String(update3));



    }

    public static String decode(byte[] bytes) throws AddressFormatException {
        try {
            byte[] priv = Base58.decode(privateString);
            PrivateKey privateKey = KeyFactory.getInstance(Algorithm).generatePrivate(new PKCS8EncodedKeySpec(priv));

            Cipher cipher3 = Cipher.getInstance(Algorithm);
            cipher3.init(Cipher.DECRYPT_MODE, privateKey);

            System.out.println("Byte lentgth: " + bytes.length);

            byte[] update3 = cipher3.doFinal(bytes);

            return new String(update3);
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, ex);
        }


        return null;
    }

    public static String encodeForServer(byte[] byteToEncode) throws AddressFormatException {

        byte[] pub = Base58.decode(publicString);

        System.out.println("public key len: " + pub.length);
        
        try {
            PublicKey publicKey = KeyFactory.getInstance(Algorithm).generatePublic(new X509EncodedKeySpec(pub));

            //        Cipher cipher = Cipher.getInstance("RSA");
            //        cipher.init(Cipher.ENCRYPT_MODE, keyPair1.getPublic());
            //        byte[] update = cipher.doFinal(toEnc.getBytes());
            //        cipher.
            //        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            //        CipherOutputStream cipherOutputStream = new CipherOutputStream(byteArrayOutputStream, cipher);
            //        cipherOutputStream.write(toEnc.getBytes());

            //        System.out.println("" + update.length);
            //
            //        System.out.println("enc: " + Base64.encode(update));


            Cipher cipher2 = Cipher.getInstance(Algorithm);
            cipher2.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] update2 = cipher2.doFinal(byteToEncode);


            return Base58.encode(update2);

        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, ex);
        }


        return null;

    }
}
