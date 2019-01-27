/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.test;

import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JTextArea;

import main.redanda.NewMessageListener;
import main.redanda.core.ConnectionHandler;
import main.redanda.core.Log;
import main.redanda.core.MessageHolder;
import main.redanda.core.Peer;
import main.redanda.core.PeerTrustData;
import main.redanda.core.Saver;
import main.redanda.core.Settings;
import main.redanda.core.Test;
import static main.redanda.core.Test.NAT_OPEN;
import static main.redanda.core.Test.getClonedPeerList;
import static main.redanda.core.Test.inBytes;
import static main.redanda.core.Test.messagesToSync;
import static main.redanda.core.Test.outBytes;
import static main.redanda.core.Test.peerList;
import static main.redanda.core.Test.peerTrusts;
import main.redanda.core.messages.TextMessageContent;
import main.redanda.services.MessageDownloader;
import main.redanda.services.MessageVerifierHsqlDb;

/**
 *
 * @author rflohr
 */
public class StatsGUI {

    public static void main(String[] args) {

//        StyleContext sc = new StyleContext();
        //StyleConstants.setFontFamily(sc.getStyle(StyleContext.DEFAULT_STYLE), "Monospaced");
//        StyleConstants.setFontFamily(sc.getStyle(StyleContext.DEFAULT_STYLE), "Lucida Console");
        JFrame meinFrame = new JFrame("redPanda");
        meinFrame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        meinFrame.setSize(1800, 800);
        final JMultilineLabel jLabel = new JMultilineLabel("loading...");
        meinFrame.add(jLabel);
        meinFrame.setVisible(true);

        new Thread() {

            @Override
            public void run() {

                while (true) {

                    try {
                        sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(StatsGUI.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    if (peerList == null) {
                        continue;
                    }

                    String format = String.format("Status listenPort: " + Test.MY_PORT + " NONCE: " + Test.NONCE + "\n");

                    int actCons = 0;

                    ArrayList<Peer> list = getClonedPeerList();

                    try {
                        Collections.sort(list, new Comparator<Peer>() {

                            @Override
                            public int compare(Peer t, Peer t1) {
                                if (t.peerTrustData == null) {
                                    return 1000;
                                }

                                if (t1.peerTrustData == null) {
                                    return -1000;
                                }

                                return (int) (t1.peerTrustData.rating - t.peerTrustData.rating);
                            }
                        });
                    } catch (IllegalArgumentException e) {
                        //System.out.println("konnte nicht sortieren!!");
                    }

//                    System.out.println("IP:PORT \t\t\t\t\t\t Nonce \t\t\t Last Answer \t Alive \t retries \t LoadedMsgs \t Ping \t Authed \t PMSG\n");
                    format += String.format("%50s %22s %12s %12s %7s %8s %10s %10s %10s %10s %8s %10s %10s %10s %10s\n", "[IP]:PORT", "nonce", "last answer", "conntected", "retries", "ping", "loaded Msg", "pendingMsg", "bytes out", "bytes in", "bad Msg", "ToSyncM", "intrMsgs", "RSM", "BackSyncdT");
                    for (Peer peer : list) {

                        if (peer.isConnected() && peer.authed && peer.writeBufferCrypted != null) {
                            actCons++;
                        }

                        //System.out.println("Peer: " + InetAddress.getByName(peer.ip) + ":" + peer.port + " Nonce: " + peer.nonce + " Last Answer: " + (System.currentTimeMillis() - peer.lastActionOnConnection) + " Alive: " + peer.isConnected() + " LastGetAllMsgs: " + peer.lastAllMsgsQuerried + " retries: " + peer.retries + " LoadedMsgs: " + peer.loadedMsgs + " ping: " + (Math.round(peer.ping * 100) / 100.));
                        String c;
                        if (peer.lastActionOnConnection != 0) {
                            c = "" + (System.currentTimeMillis() - peer.lastActionOnConnection);
                        } else {
                            c = "-";
                        }

                        if (peer.getPeerTrustData() == null) {
                            format += String.format("%50s %22d %12s %12s %7d %8s %10s %10s %10d %10d %10d\n", "[" + peer.ip + "]:" + peer.port, 1556, c, "" + peer.isConnected() + "/" + (peer.authed && peer.writeBufferCrypted != null), peer.retries, (Math.round(peer.ping * 100) / 100.), "-", "-", peer.sendBytes, peer.receivedBytes, peer.removedSendMessages.size());
                        } else {
                            //format += String.format("%50s %22d %12s %12s %7d %8s %10d %10d %10d %8s %10d %10d %10s\n", "[" + peer.ip + "]:" + peer.port, peer.nonce, c, "" + peer.isConnected() + "/" + (peer.authed && peer.writeBufferCrypted != null), peer.retries, (Math.round(peer.ping * 100) / 100.), peer.getPeerTrustData().loadedMsgs.size(), peer.sendBytes, peer.receivedBytes, peer.getPeerTrustData().badMessages, messagesToSync(peer.peerTrustData.internalId), peer.removedSendMessages.size(), formatInterval(System.currentTimeMillis() - peer.peerTrustData.backSyncedTill));
                            format += String.format("%50s %22d %12s %12s %7d %8s %10d %10s %10d %10d %8s %10d %10d %10s %10s %10s %10s\n", "[" + peer.ip + "]:" + peer.port, 1556, c, "" + peer.isConnected() + "/" + (peer.authed && peer.writeBufferCrypted != null), peer.retries, (Math.round(peer.ping * 100) / 100.), peer.getPeerTrustData().getMessageLoadedCount(), peer.getPeerTrustData().pendingMessages.size() + " " + peer.getPeerTrustData().pendingMessagesPublic.size() + " " + peer.getPeerTrustData().pendingMessagesTimedOut.size(), peer.sendBytes, peer.receivedBytes, peer.getPeerTrustData().badMessages, messagesToSync(peer.peerTrustData.internalId), peer.peerTrustData.rating, peer.removedSendMessages.size(),
                                    peer.peerTrustData.backSyncedTill == Long.MAX_VALUE ? "-" : formatInterval(System.currentTimeMillis() - peer.peerTrustData.backSyncedTill),
                                    peer.peerTrustData.pendingMessagesTimedOut.size(), peer.peerTrustData.pendingMessagesTimedOut.size());
                        }

//                        while (c.length() < 15) {
//                            c += " \t";
//                        }
//                        if (peer.getPeerTrustData() == null) {
//                            output += "" + a + " \t " + b + "\t " + c + "\t " +  + "\t " + peer.retries + "\t " + "--" + " \t " + (Math.round(peer.ping * 100) / 100.) + "\t " + peer.authed + "\t " + "--" + " \t" + peer.requestedMsgs + " \t" + "--" + "\n";
//                        } else {
//                            output += "" + a + " \t " + b + "\t " + c + "\t " + peer.isConnected() + "\t " + peer.retries + "\t " + peer.getLoadedMsgs().size() + " \t " + (Math.round(peer.ping * 100) / 100.) + "\t " + peer.authed + "\t " + peer.getPendingMessages().size() + " \t" + peer.requestedMsgs + " \t" + peer.getPeerTrustData().synchronizedMessages + "\n";
//                        }
                    }

                    format += String.format("\nNot connected trust data:\n");

                    ArrayList<Peer> clonedPeerList = getClonedPeerList();

                    format += String.format("%12s %25s %12s %12s\n", "ID", "Last Seen", "SyncedMsgs", "ToSync");

                    ArrayList<PeerTrustData> peerTrustsCloned = (ArrayList<PeerTrustData>) peerTrusts.clone();

                    Collections.sort(peerTrustsCloned, new Comparator<PeerTrustData>() {

                        @Override
                        public int compare(PeerTrustData o1, PeerTrustData o2) {
                            return (int) (o2.lastSeen - o1.lastSeen);
                        }
                    });

                    for (PeerTrustData ptd : peerTrustsCloned) {

                        boolean found = false;
                        for (Peer p : clonedPeerList) {
                            if (p.isFullConnected() && p.peerTrustData == ptd) {
                                found = true;
                                break;
                            }
                        }

                        if (found) {
                            continue;
                        }

                        int messagesToSync = messagesToSync(ptd.internalId);

                        format += String.format("%12d %25s %12d %12d\n", ptd.internalId, formatInterval(System.currentTimeMillis() - ptd.lastSeen), ptd.synchronizedMessages, messagesToSync);

                    }

                    format += String.format("\n\nConnected to " + actCons + " peers. (NAT type: " + (NAT_OPEN ? "open" : "closed") + ")\n");
                    format += String.format("Traffic: " + inBytes / 1024. + " kb / " + outBytes / 1024. + " kb.\n");

                    format += String.format("\nServices last run: ConnectionHandler: " + Math.round((System.currentTimeMillis() - ConnectionHandler.lastRun) / 1000.) + " MessageDownloader: " + Math.round((System.currentTimeMillis() - MessageDownloader.lastRun) / 1000.) + " MessageVerifierHsqlDb: " + Math.round((System.currentTimeMillis() - MessageVerifierHsqlDb.lastRun) / 1000.) + "\n");

                    //System.out.println("Processed messages: " + msgs.size());
//                    int unverifiedMsgs = 0;
//
//                    for (RawMsg m : MessageHolder.getAllNotVerifiedMessages()) {
//                        if (m.verified) {
//                            continue;
//                        }
//                        unverifiedMsgs++;
//                    }
//
                    //System.out.println("Saved Sockets: " + ConnectionHandler.allSockets.size());
//                            try {
//                                Test.messageStore.getConnection().close();
//                            } catch (SQLException ex) {
//                                Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
//                            }
//                            Main.useHsqlDatabase();
                    format += String.format("Processed messages: " + MessageHolder.getMessageCount() + " - Queue to verify: " + MessageHolder.getMessageCountToVerify());
                    format += String.format("\nLRU Cache size: " + MessageDownloader.channelIdToLatestBlockTime.size());
                    //MessageVerifierHsqlDb.sem.release();
                    //String replaceAll = format.replaceAll("\n", "<br/>");
//                    jLabel.setText("<html>" + replaceAll + "</html>");
                    jLabel.setText(format);

                }

            }

        }.start();

        System.out.println(
                "delay: " + Settings.pingDelay + " timeout: " + Settings.pingTimeout);
        Saver saver = new Saver();

        main.redanda.Main.addListener(
                new NewMessageListener() {

            @Override
            public void newMessage(TextMessageContent msg) {

                System.out.println("###################\n# Neue Nachricht [" + msg.channel.getName() + " -- " + msg.identity + "]\n#   " + msg.text + "\n###################");

            }
        });

        Settings.readGeneralDotDat();

        //org.redPandaLib.Main.useMysqlDatabase();
        main.redanda.Main.useHsqlDatabase();

        Log.LEVEL = 1000;
        Settings.lightClient = false;
        Settings.SUPERNODE = true;
        Settings.REDUCE_TRAFFIC = false;
        Settings.MIN_CONNECTIONS = 10;
        Settings.MAX_CONNECTIONS = 12;


        try {
            main.redanda.Main.startUp(
                    true, saver);
        } catch (IOException ex) {
            Logger.getLogger(StatsGUI.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static String formatInterval(final long l) {
        final long hr = TimeUnit.MILLISECONDS.toHours(l);
        final long min = TimeUnit.MILLISECONDS.toMinutes(l - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        final long ms = TimeUnit.MILLISECONDS.toMillis(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
        return String.format("%02d:%02d:%02d.%03d", hr, min, sec, ms);
    }

    public static class JMultilineLabel extends JTextArea {

        private static final long serialVersionUID = 1L;

        public JMultilineLabel(String text) {
            super(text);
            setEditable(false);
            setCursor(null);
            setOpaque(false);
            setFocusable(false);
            //setFont("Monospaced");
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            setWrapStyleWord(true);
            setLineWrap(true);
        }
    }
}
