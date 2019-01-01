/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.core;

import java.security.MessageDigest;

/**
 *
 * @author rflohr
 */
public class HashCashBsp {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        // TODO code application logic here



        //Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        //Key k = new SecretKeySpec("s", "AES");
        //c.init(Cipher.ENCRYPT_MODE, k, iv);
        
        int c = 0;

        for (int b = 0; b < 10; b++) {

            int a = 0;

            while (true) {

                a++;

                if (a == 0) {
                    System.out.println("keinen hash gefunden!");
                    break;
                }

                String text = "kurzer text, etwas laenger, asdasdsad" + b;



                String toHash = a + text;

                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(toHash.getBytes());

                byte byteData[] = md.digest();

                //convert the byte to hex format method 1
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < byteData.length; i++) {
                    sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
                }



                if (sb.toString().matches("0(.*)")) {
                    System.out.println("Hex format : " + sb.toString());
                    System.out.println("a : " + a);
                    c += a;
                    break;
                }
            }
        }
        
        
        System.out.println("avg.: " + c/10);
        //1100000 - 5
        //64000 - 4
        //4300 - 3
        //280 - 2
        //18 -1

    }
}
