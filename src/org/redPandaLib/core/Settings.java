/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author rflohr
 */
public class Settings {

    public static int STD_PORT = 59558;
    public static int MIN_CONNECTIONS = 12;
    public static int MAX_CONNECTIONS = 50;
    public static int pingTimeout = 180; //time in sec
    public static int pingDelay = 120; //time in sec
    public static int peerListRequestDelay = 60 * 60;//time in sec
    public static long till = 1370803752399L;
    public static boolean lightClient = false;
    public static long connectToNewClientsTill = Long.MAX_VALUE;
    public static boolean initFullNetworkSync = false; //can cause huge traffic!
    public static boolean SEND_DELIVERED_MSG = true; //only for non special channels
    public static boolean IPV6_ONLY = false;
    public static boolean IPV4_ONLY = false;
    public static String[] knownNodes = {"redpanda.ignorelist.com", "redpanda.ip-v6.eu", "redpanda-ipv6only.allowed.org", "xana.hopto.org", "5.35.243.99", "fabulous.h4ck.me", "2a01:488:66:1000:523:f363:0:1"};
    //public static String[] knownNodes = {"2a01:488:66:1000:523:f363:0:1", "2a02:908:d51a:1880:3d54:128f:a4ea:f765"};
    //"2a01:0488:0066:1000:0523:f363:0000:0001"
    //public static String[] knownNodes = {"5.35.243.99", "fabulous.h4ck.me"};
    public static int STARTPORT = 59558;//default value if not overwritten by general.dat
    public static int MAXPUBLICMSGS = 15000;//default value if not overwritten by general.dat
    public static boolean TESTNET = false;
    public static boolean SUPERNODE = false;
    public static boolean BROADCAST_MSGS_AFTER_VERIFICATION = false;

    private static void readGeneralDotDat() {
        try {
            File file = new File(Saver.SAVE_DIR + "/general.dat");
            FileInputStream fileInputStream = new FileInputStream(file);

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));


            String readLine = bufferedReader.readLine();
            if (readLine != null) {
                Settings.STARTPORT = Integer.parseInt(readLine);
            } else {
                Settings.STARTPORT = 59558;
            }

            readLine = bufferedReader.readLine();
            if (readLine != null) {
                MAXPUBLICMSGS = Integer.parseInt(readLine);
            }

            readLine = bufferedReader.readLine();
            if (readLine != null) {
                if (readLine.equals("testnet=true")) {
                    TESTNET = true;
                    Test.MAGIC = "test";
                    STD_PORT = 59888;
                    System.out.println("################################################################################\n\n\n WARNING you are running redPanda in TESTNET mode, you will only connect to other testnet nodes...\n\n\n################################################################################");
                }
            }

            readLine = bufferedReader.readLine();
            if (readLine != null) {
                if (readLine.equals("supernode=true")) {
                    SUPERNODE = true;
                    System.out.println("I am a supernode! :)");
                }
            }


            bufferedReader.close();
            fileInputStream.close();

        } catch (IOException ex) {
        } catch (NumberFormatException e) {
        }

    }

    public static int getStartPort() {
        readGeneralDotDat();
        return STD_PORT;
    }
}
