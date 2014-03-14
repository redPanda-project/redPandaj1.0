/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class IpChecker {

    public static String getIp() throws Exception {
        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = null;
        InputStream openStream = null;
        InputStreamReader inputStreamReader = null;
        try {
            openStream = whatismyip.openStream();
            inputStreamReader = new InputStreamReader(openStream);
            in = new BufferedReader(inputStreamReader);
            String ip = in.readLine();
            return ip;
        } finally {
            if (openStream != null) {
                try {
                    openStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }



            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
