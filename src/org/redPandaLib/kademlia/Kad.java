package org.redPandaLib.kademlia;

import crypt.Utils;
import kademlia.JKademliaNode;
import kademlia.exceptions.ContentNotFoundException;
import kademlia.node.KademliaId;
import kademlia.node.Node;
import org.redPandaLib.Main;
import org.redPandaLib.services.IpChecker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.security.SecureRandom;

public class Kad {

    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();
    static int udpPort = -1;

    public static JKademliaNode node;

    public static void startAsync() {
        new Thread() {

            @Override
            public void run() {
                Kad.start();
            }
        }.start();
    }

    public static void start() {

        boolean reset = false;

        System.out.println("starting KAD node");

        KadConfig kadConfig = new KadConfig();


        if (reset) {
            createNewInstance(kadConfig);
        } else {

            try {
                node = JKademliaNode.loadFromFile("main", kadConfig);
            } catch (FileNotFoundException e) {
//            e.printStackTrace();
                System.out.println("no KAD save state found, generating new kad instance...");
                createNewInstance(kadConfig);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }


        try {
            String ip = IpChecker.getIp();
            System.out.println("my ip: " + ip);
            node.getNode().setInetAddress(InetAddress.getByName(ip));
        } catch (Exception e) {
            e.printStackTrace();
        }

//        if (node.getPort() != -1 && node.getPort() != 12050) {

//        } else {
//            System.out.println("seed node for dht");
//        }

//        System.out.println(node.getNode().getNodeId().toString());
//        System.out.println(node.getNode().getNodeId().toString());


        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    int cnt = 0;
                    while (!Main.shutdown) {


                        try {
                            KadContentUpdate.checkForUpdate();
                            try {
                                if (cnt < 5) {
                                    cnt++;
                                    Node bootstrapnode = new Node(new KademliaId(Utils.parseAsHexOrBase58("0E3D9A1C26528192602B326B356275E4548B4D61")), InetAddress.getByName("91.250.113.186"), 12050);
                                    if (!bootstrapnode.equals(node.getNode())) {
                                        System.out.println("bootstrap...");
                                        Kad.node.bootstrap(bootstrapnode);
                                    } else {
                                        System.out.println("dont bootstrap...");
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Thread.sleep(60000);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ContentNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }.start();


    }

    private static void createNewInstance(KadConfig kadConfig) {

        boolean success = false;
        for (int i = 0; i < 50; i++) {
            if (success) {
                break;
            }
            udpPort = 12050 + i;
            try {


                String ip = IpChecker.getIp();
                System.out.println("my ip: " + ip);

                node = new JKademliaNode("main", new Node(new KademliaId(), InetAddress.getByName(ip), udpPort), udpPort, kadConfig);
                System.out.println("successful bound to udp port for dht: " + udpPort);
                success = true;
            } catch (BindException e) {
                e.printStackTrace();
                System.out.println("could not bind to udp port for dht: " + udpPort);

            } catch (IOException e2) {
                e2.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }


    public static void shutdown() {

        if (node != null) {
            try {
                node.shutdown(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    static String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        }
        return sb.toString();
    }

}
