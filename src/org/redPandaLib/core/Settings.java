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
 * @author rflohr
 */
public class Settings {

    public static int STD_PORT = 59558;
    public static int MIN_CONNECTIONS = 20;
    public static int MAX_CONNECTIONS = 50;
    public static int pingTimeout = 60 * 10; //time in sec
    public static int pingDelay = 60 * 9; //time in sec
    public static int peerListRequestDelay = 60 * 60;//time in sec
    public static long till = 0;//1397836192756L;
    public static boolean lightClient = false;
    public static long connectToNewClientsTill = Long.MAX_VALUE;
    public static boolean SEND_DELIVERED_MSG = false; //only for non special channels
    public static boolean IPV6_ONLY = false;
    public static boolean IPV4_ONLY = false;
    //    public static String[] knownNodes = {"redPanda.im", "91.250.113.186", "2a01:488:66:1000:5bfa:71ba:0:1", "fabulous.h4ck.me", "geeq.de", "redpanda.ignorelist.com", "redpanda.ip-v6.eu", "redpanda-ipv6only.allowed.org"};
    public static String[] knownNodes = {"127.0.0.1"};
    public static int STARTPORT = 59558;//default value if not overwritten by general.dat
    public static int MAXPUBLICMSGS = 15000;//default value if not overwritten by general.dat
    public static boolean TESTNET = false;
    public static boolean SUPERNODE = false;
    public static boolean BROADCAST_MSGS_AFTER_VERIFICATION = true;
    public static boolean REMOVE_OLD_MESSAGES = false;
    public static String EXTERNAL_DATABASE_LOGIN_CREDENTIALS = null; //format: user,dbname,password
    public static boolean REDUCE_TRAFFIC = false; //This is currenlty only a hack. This allows to not load images when mobile internet is used. (Messages will be introduced from all peers every time they reconnect to us!!!)
    public static boolean DONT_REMOVE_UNUSED_MESSAGES = false; //dont remove unused messages (messages which are doublicates within blocks)

    public static void readGeneralDotDat() {
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
                    //allow the other nodes to trigger the ping, shoud save batter on 3G usage for others.
                    pingTimeout *= 2;
                    pingDelay *= 2;
                    MIN_CONNECTIONS = 50;
                    MAX_CONNECTIONS = 400;
                    System.out.println("I am a supernode! :)");
                }
            }

            readLine = bufferedReader.readLine();
            if (readLine != null) {
                if (readLine.equals("lightClient=true")) {
                    lightClient = true;
                    System.out.println("I am a light client!");
                }
            }

            readLine = bufferedReader.readLine();
            if (readLine != null) {
                if (readLine.equals("removeOldMessages=true")) {
                    REMOVE_OLD_MESSAGES = true;
                    System.out.println("removing old messages automatically.");
                } else if (readLine.equals("removeOldMessages=false")) {
                    System.out.println("NOT removing old messages automatically.");
                }
            }

            //reads database logins if contains , on the right side.
            readLine = bufferedReader.readLine();
            if (readLine != null) {
                String[] split = readLine.split("=");
                if (split.length == 2) {
                    String key = split[0];

                    if (key.equals("external_database_login")) {

                        String value = split[1];
                        if (value.split(",").length == 3) {
                            EXTERNAL_DATABASE_LOGIN_CREDENTIALS = value;
                            System.out.println("added external database credentials");
                        } else {
                            System.out.println("No mysql login data found, using internal database.");
                        }
                    }
                }
            }

            readLine = bufferedReader.readLine();
            if (readLine != null) {
                if (readLine.equals("DontRemoveUnusedMessages=true")) {
                    DONT_REMOVE_UNUSED_MESSAGES = true;
                    System.out.println("dont remove unused messages (messages which are doublicates within blocks)");
                }
            }

            bufferedReader.close();
            fileInputStream.close();

            //current example cfg:
//59558
//15000
//testnet=false
//supernode=false
//lightClient=false
//removeOldMessages=true
//external_database_login=false (otherwise set equal to loginUser'comma symbol'loginPassword)
        } catch (IOException ex) {
        } catch (NumberFormatException e) {
        }

    }

    public static int getStartPort() {
        return STARTPORT;
    }
}
