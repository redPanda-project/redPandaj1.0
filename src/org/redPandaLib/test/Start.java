/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.test;

import java.io.IOException;
import java.security.SignatureException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.redPandaLib.NewMessageListener;
import org.redPandaLib.core.Log;
import org.redPandaLib.core.Saver;
import org.redPandaLib.core.Settings;
import org.redPandaLib.core.messages.TextMessageContent;
import org.redPandaLib.crypt.Main;
import org.redPandaLib.database.HsqlConnection;
import org.redPandaLib.kademlia.Kad;

/**
 * @author rflohr
 */
public class Start {

    public static void main(String[] args) throws IOException, SignatureException {
        // TODO code application logic here

//        Settings.lightClient = true;
//        new Thread() {
//
//            long lastUpdateChecked = 0;
//
//            @Override
//            public void run() {
//
//                while (true) {
//                    try {
//                        sleep(6000);
//                    } catch (InterruptedException ex) {
//                        Logger.getLogger(BitchatjUse.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//
//                    if (System.currentTimeMillis() - lastUpdateChecked < 1000 * 60 * 2) {
//                        continue;
//                    }
//
//                    //System.out.println("looking for an update...");
//
//                    lastUpdateChecked = System.currentTimeMillis();
//                    lastUpdateChecked -= new Random().nextInt(20000);
//
//
//                    //System.out.println("looking for files to update...");
//
//
//                    try {
//
//                        String myVersions = "";
//
//                        try {
//                            File file = new File("versions");
//                            FileInputStream fileInputStream = new FileInputStream(file);
//                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
//
//
//
//                            String line;
//                            while ((line = bufferedReader.readLine()) != null) {
//                                myVersions += line + "\n";
//                            }
//
//                            bufferedReader.close();
//                            fileInputStream.close();
//
//                        } catch (FileNotFoundException e) {
//                            System.out.println("no versions found, downloading all...");
//                        }
//
//
//                        // Create a URL for the desired page
//                        URL url = new URL("http://redpanda.hopto.org/redpanda/versions");
//                        // Read all the text returned by the server
//                        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
//                        String str;
//                        boolean needUpdate = false;
//                        while ((str = in.readLine()) != null) {
//
//                            String fileName = str.split(" ")[0];
//                            String timestamp = str.split(" ")[1];
//
//                            if (myVersions.split(fileName).length == 1) {
//                                //new File
//
//                                System.out.println("New File: " + fileName);
//
//                                needUpdate = true;
//
//                                //System.out.println("done...");
//
//                            } else {
//
//                                String mVersion = myVersions.split(fileName)[1].split("\n")[0];
//
//                                //System.out.println("File: " + fileName + "Meine Version: " + mVersion + " Server: " + timestamp);
//
//                                if (Long.parseLong(timestamp) > Long.parseLong(mVersion.substring(1))) {
//
//                                    System.out.println("Need to update file: " + fileName);
//
//                                    needUpdate = true;
//
//                                    System.out.println("done...");
//
//
//                                }
//
//
//                            }
//
//
//
//
//                        }
//                        in.close();
//
//                        if (needUpdate) {
//                            File file = new File(".restart");
//                            if (!file.exists()) {
//                                file.createNewFile();
//                            }
//                            System.exit(0);
//                        }
//
//
//
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    } catch (NumberFormatException e) {
//                        e.printStackTrace();
//                    }
//
//
//                }
//            }
//        }.start();
//        ArrayList<Channel> channels = org.redPandaLib.Main.getChannels();
//        
//        Channel[] f = new Channel[10];
//        Channel[] toArray = channels.toArray(f);
//
//        System.out.println("" + toArray);
//        
        System.out.println(
                "delay: " + Settings.pingDelay + " timeout: " + Settings.pingTimeout);
        Saver saver = new Saver();

        org.redPandaLib.Main.addListener(
                new NewMessageListener() {

                    @Override
                    public void newMessage(TextMessageContent msg) {

                        System.out.println("###################\n# Neue Nachricht [" + msg.channel.getName() + " -- " + msg.identity + "]\n#   " + msg.text + "\n###################");

                    }
                });

        org.redPandaLib.Main.useHsqlDatabase();

        Settings.readGeneralDotDat();

        Log.LEVEL = -1;
        Settings.lightClient = true;
        Settings.SUPERNODE = false;
        Settings.REDUCE_TRAFFIC = false;
        Settings.MIN_CONNECTIONS = 30;
        Settings.MAX_CONNECTIONS = 30;

        Settings.pingDelay = 30;

//        Kad.startAsync();

        org.redPandaLib.Main.startUp(
                true, saver);




    }
}
