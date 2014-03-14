/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.crypt;

import org.spongycastle.crypto.params.ECDomainParameters;

/**
 *
 * @author robin
 */
public class TestMain {

    private static ECDomainParameters ecParams;

    public static void main(String[] args) {

//        String asd = "fwjgfhjewgfhjewf";
//        Sha256Hash sha256Hash = Sha256Hash.create(asd.getBytes());
//        
//        ECKey eCKey = new ECKey();
//        
//        X9ECParameters params = SECNamedCurves.getByName("secp256k1");
//        ecParams = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
//
//        ECDSASigner signer = new ECDSASigner();
//        ECPrivateKeyParameters privKey = new ECPrivateKeyParameters(new BigInteger(eCKey.getPrivKeyBytes()), ecParams);
//        signer.init(true, privKey);
//                
//        BigInteger[] sigs = signer.generateSignature(sha256Hash.getBytes());
//        
//        
//        
//        System.out.println(" " + Utils.bigIntegerToBytes(sigs[0], 32));

//        
//        String asd = "dasbdh:dwjbdwzhd\njsdas\ndwzhd\njsdas\ndwzhd\njsdas\ndwzhd\njsdas\n";
//        
//        System.out.println("" + Msg.uncleanContent(Msg.cleanContent(asd)));


//        String fix = Utils.bytesToHexString(new ECKey().getPubKey());
//
//        double min = 555555;
//        String mins = "";
//        
//        double mean = 0;
//
//        long a = System.currentTimeMillis();
//        for (int i = 1; i < 200; i++) {
//            String bytesToHexString = Utils.bytesToHexString(new ECKey().getPubKey());
//            double norm = norm(fix, bytesToHexString);
//
//            min = Math.min(min, norm);
//            if (min == norm) {
//                mins = bytesToHexString;
//            }
//
//            mean += norm;
//            System.out.println("" + min);
//            System.out.println("" + fix + " " + mins);
//            System.out.println("Norm: " + norm);
//
//        }
//
//        System.out.println("" + (System.currentTimeMillis() - a));
//        System.out.println("Mean: " + mean/200);


    }

//    public static double norm(String hex1, String hex2) {
//
//        double norm = 0;
//        for (int i = 0; i < hex1.length(); i++) {
//            int compare = Character.compare(hex1.charAt(i), hex2.charAt(i));
//            if (compare < 0) {
//                compare = -compare;
//            }
//            //norm += ((double) compare) / ((double) i + 1);
//            norm += Math.pow(compare,2);
//        }
//
//        return norm;
//
//
//    }
}
