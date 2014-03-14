/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.Main;

/**
 *
 * @author robin
 */
public class ConnectionHandlerConnect extends Thread {

    private static Selector selector;

    static {
        try {
            selector = Selector.open();
        } catch (IOException ex) {
            Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        while (!Main.shutdown) {
            try {
                int readyChannels = selector.select();

                System.out.println("rdy chans: " + readyChannels);
                
                Set<SelectionKey> selectedKeys = selector.selectedKeys();

                if (readyChannels == 0 && selectedKeys.isEmpty()) {
                    System.out.print(".");

                    try {
                        sleep(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    continue;
                }


                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {

                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();
                    if (!key.isValid()) {
                        System.out.println("hmmm");
                        key.cancel();
                        //keyIterator.remove();
                        continue;
                    }

                    Peer peer = (Peer) key.attachment();

                    if (key.isConnectable()) {

                        System.out.println("try to finsih connection");
                        boolean connected = false;
                        System.out.println("finish con");
                        try {
                            connected = peer.getSocketChannel().finishConnect();
                        } catch (IOException e) {
                        }
                        System.out.println("finished!");

                        if (!connected) {
                            System.out.println("connection could not be established...");
                            key.cancel();
                            peer.setConnected(false);
                            peer.isConnecting = false;
                            try {
                                peer.getSocketChannel().close();
                            } catch (IOException ex) {
                                Logger.getLogger(ConnectionHandlerConnect.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            continue;
                        }

                        //System.out.println("Connection established...");

                        key.cancel();
                        peer.isConnecting = false;
                        peer.setConnected(true);


                        // a connection was established with a remote server.
                    }

                }


            } catch (IOException ex) {
                Logger.getLogger(ConnectionHandlerConnect.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }
}