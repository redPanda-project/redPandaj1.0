/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.crypt;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import static main.redanda.crypt.RSA.Algorithm;

/**
 *
 * @author robin
 */
public class RSAKey {

    public static final int KEY_LENGTH = 3072;
    PublicKey publicKey;
    PrivateKey privateKey;

    public RSAKey(PublicKey publicKey, PrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public static RSAKey newInstace() {
        KeyPair keyPair;
        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance(Algorithm);

            kpg.initialize(KEY_LENGTH);
            keyPair = kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    "Error generating RSA key pair: "
                    + e.getMessage());
        }

        return new RSAKey(keyPair.getPublic(), keyPair.getPrivate());

    }

    public byte[] getPublicKeyBytes() {
        return publicKey.getEncoded();

    }

    public byte[] getPrivateKeyBytes() {
        return privateKey.getEncoded();

    }

    public String getPublicKeyEncoded() {
        return Base58.encode(publicKey.getEncoded());

    }

    public String getPrivateKeyEncoded() {
        return Base58.encode(privateKey.getEncoded());

    }

    public static RSAKey newInstance(String encodedPublicKey, String encodedprivateKey) {
        try {
            PublicKey publicKey = null;
            if (encodedPublicKey != null) {

                byte[] publicKeyBytes = Base58.decode(encodedPublicKey);
                publicKey = KeyFactory.getInstance(Algorithm).generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            }

            PrivateKey privateKey = null;
            if (encodedprivateKey != null) {
                byte[] privateKeyBytes = Base58.decode(encodedprivateKey);
                privateKey = KeyFactory.getInstance(Algorithm).generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            }

            return new RSAKey(publicKey, privateKey);
        } catch (AddressFormatException ex) {
            Logger.getLogger(RSAKey.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(RSAKey.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(RSAKey.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static void main(String[] args) {
//        RSAKey newInstace = RSAKey.newInstace();
//        System.out.println("PRIV: " + Base58.encode(newInstace.getPrivateKeyBytes()));
//        System.out.println("PUB: " + Base58.encode(newInstace.getPublicKeyBytes()));
        ECKey ecKey = new ECKey();
        System.out.println("" + ecKey.getPrivKeyBytes().length);
        System.out.println("" + Base58.encode(ecKey.getPrivKeyBytes()));

    }
}
