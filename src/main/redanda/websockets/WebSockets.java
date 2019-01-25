package main.redanda.websockets;


import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import main.redanda.core.Stats;
import main.redanda.socketio.ChatObject;
import main.redanda.socketio.SIOCommands;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class WebSockets extends WebSocketServer {


    public static final String keyStoreFile = "webcert.jks";
    public static int PORT;

    public static void main(String[] args) throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, InterruptedException {

        startServer(59558);

    }


    public static void startServer(final int myPort) {

        try {
            WebSockets ws = new WebSockets(myPort + 100);
            ws.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

//    public static void startServer(final int myPort) {
//
//        Configuration config = new Configuration();
//        config.setHostname("0.0.0.0");
//        PORT = myPort + 100;
//        config.setPort(PORT);
//
//
//        final SocketIOServer server = new SocketIOServer(config);
//
//
//        SIOCommands.init(server);
//
//
//        server.addConnectListener(new ConnectListener() {
//            @Override
//            public void onConnect(SocketIOClient client) {
//                System.out.println("New socketio connection: " + client.getTransport());
//
//                Stats.incSocketioConnectionsLiveTime();
//
////                new Thread() {
////                    @Override
////                    public void run() {
////                        while (true) {
////                            try {
////                                sleep(200);
////                            } catch (InterruptedException e) {
////                                e.printStackTrace();
////                            }
////                            client.sendEvent(
////                                    "test", "f74jzrz47w8jz4w75z8wjz587wz587w75adsj743f74jzrz47w8jz4w75z8wjz587wz587w75adsj743f74jzrz47w8jz4w75z8wjz587wz587w75adsj743f74jzrz47w8jz4w75z8wjz587wz587w75adsj743f74jzrz47w8jz4w75z8wjz587wz587w75adsj743f74jzrz47w8jz4w75z8wjz587wz587w75adsj743f74jzrz47w8jz4w75z8wjz587wz587w75adsj743f74jzrz47w8jz4w75z8wjz587wz587w75adsj743".getBytes()
////                            );
////
////                            System.out.println("send ");
////                        }
////                    }
////                }.start();
//
//
//            }
//        });
//
//        server.startAsync();
//
//        new Thread() {
//            @Override
//            public void run() {
//
//                try {
//                    sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                if (hostAvailabilityCheck()) {
//                    System.out.println("webserver available");
//                } else {
//                    server.stop();
//                    System.out.println("webserver stopped, because it was unresponsive!");
//                    try {
//                        sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    System.out.println("webserver restart");
//                    startServer(myPort);
//                }
//            }
//        }.start();
//
//    }
//
//
//    public static boolean hostAvailabilityCheck() {
//        try (Socket s = new Socket("localhost", PORT)) {
//            return true;
//        } catch (IOException ex) {
//            /* ignore */
//        }
//        return false;
//    }

    public WebSockets(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
    }

    public WebSockets(InetSocketAddress address) {
        super(address);
    }


    @Override
    public void onOpen(WebSocket conn, ClientHandshake clientHandshake) {
        System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {

    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        System.out.println("got message: " + s);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        WSParser.parseAsync(conn, message);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {

    }

    @Override
    public void onStart() {
        System.out.println("Server started!");
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }
}