/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rflohr
 */
public class HashCash {

    public static String[] generate(String content, int minDiff) {

        String regex = "(.*)";

        if (minDiff == 1) {
            regex = "0(.*)";
        } else if (minDiff == 2) {
            regex = "00(.*)";
        }

        int a = 0;

        StringBuffer sb = new StringBuffer();

        while (true) {
            try {
                a++;

                if (a == 0) {
                    System.out.println("keinen hash gefunden!");
                    break;
                }





                String toHash = a + content;

                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(toHash.getBytes());

                byte byteData[] = md.digest();

                //convert the byte to hex format method 1

                for (int i = 0; i < byteData.length; i++) {
                    sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
                }



                if (sb.toString().matches(regex)) {
                    System.out.println("Hex format : " + sb.toString());
                    System.out.println("a : " + a);
                    break;
                }
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(HashCash.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        String[] out = new String[2];

        out[0] = sb.toString();
        out[1] = Integer.toString(a);

        return out;
        
    }
}
