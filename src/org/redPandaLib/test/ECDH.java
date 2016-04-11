/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.KeyAgreement;
import org.redPandaLib.crypt.AESCrypt;
import org.redPandaLib.crypt.ECKey;
import static org.redPandaLib.crypt.ECKey.CURVE;
import org.redPandaLib.crypt.Sha256Hash;
import org.redPandaLib.crypt.Utils;
import org.spongycastle.crypto.agreement.ECDHBasicAgreement;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;

/**
 *
 * @author rflohr
 */
public class ECDH {

    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException, Exception {

//        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
//
//        ECGenParameterSpec ecParamSpec = new ECGenParameterSpec("secp224k1");
//        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDH", "SC");
//        kpg.initialize(ecParamSpec);
//
//        KeyPair kpA = kpg.generateKeyPair();
//        KeyPair kpB = kpg.generateKeyPair();
//        
//        System.out.println("" + kpA.getPublic().getEncoded().length + " || " + Utils.bytesToHexString(kpA.getPublic().getEncoded()));
//        System.out.println("" + kpA.getPrivate().getEncoded().length + " || " + Utils.bytesToHexString(kpA.getPrivate().getEncoded()));
//        
//        KeyAgreement aKeyAgreement = KeyAgreement.getInstance("ECDH", "SC");
//        aKeyAgreement.init(kpA.getPrivate());
//        aKeyAgreement.doPhase(kpB.getPublic(), true);
//
//       
//        byte[] sharedKeyA = aKeyAgreement.generateSecret();
//
//        System.out.println("" + Utils.bytesToHexString(sharedKeyA));
//        ECKey a = new ECKey();
//        ECKey b = new ECKey();
//
//        BigInteger deffiehelman = a.deffiehelman(b.getPubKey());
//
//        BigInteger deffiehelman2 = b.deffiehelman(a.getPubKey());
//
//        System.out.println("asd " + Utils.bytesToHexString(Utils.bigIntegerToBytes(deffiehelman, 32)));
//        System.out.println("asd " + Utils.bytesToHexString(Utils.bigIntegerToBytes(deffiehelman2, 32)));
//
//        Sha256Hash create = Sha256Hash.create(Utils.bigIntegerToBytes(deffiehelman2, 32));
//        byte[] bytes = create.getBytes();
//
//        byte[] by = a.getPubKey();
//
//        long value = 0;
//        for (int i = 0; i < by.length; i++) {
//            value += ((long) by[i] & 0xffL) << (8 * i);
//        }
//
//        System.out.println("long: " + value);
//
//        System.out.println("" + a.getPubKey().length);
//
//
        byte[] pass = new byte[32];

        new SecureRandom().nextBytes(pass);

        byte[] encodeCTR = AESCrypt.encodeCTR("te".getBytes(), pass, 0L, false);

        byte[] var2 = new byte[2];

        ByteBuffer wrap = ByteBuffer.wrap(var2);

        wrap.put(AESCrypt.encodeCTR("t".getBytes(), pass, 0L, false));
        wrap.put(AESCrypt.encodeCTR("e".getBytes(), pass, 0L, false));

        System.out.println("" + encodeCTR.length + " " + Utils.bytesToHexString(encodeCTR));

        byte[] plain = AESCrypt.decodeCTR(encodeCTR, pass, 0L);

        byte[] var2plain = AESCrypt.decodeCTR(var2, pass, 0L);

        System.out.println(":" + new String(plain));
        System.out.println(":" + new String(var2plain));

    }

}
