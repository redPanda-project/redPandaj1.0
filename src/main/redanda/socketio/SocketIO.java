package main.redanda.socketio;

import java.io.*;
import java.net.Socket;
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

public class SocketIO {


    public static final String keyStoreFile = "webcert.jks";
    public static int PORT;

    public static void main(String[] args) throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, InterruptedException {


        Configuration config = new Configuration();
        config.setHostname("0.0.0.0");
//        config.setPort(10443 + new Random().nextInt(3));
        config.setPort(10443);


        //ToDo: create and save password in localSettings?
        char[] password = "password".toCharArray();

        /**
         * if we have to create a new keystore, lets save it in the correct file
         */
        if (!new File(keyStoreFile).exists()) {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());


            ks.load(null, password);

            // Store away the keystore.
            FileOutputStream fos = new FileOutputStream(keyStoreFile);
            ks.store(fos, password);
            fos.close();

        }
//        config.setKeyStorePassword(new String(password));
////        InputStream stream = SocketIO.class.getResourceAsStream("asdgf");
//        File initialFile = new File(keyStoreFile);
//        InputStream targetStream = new FileInputStream(initialFile);
//        config.setKeyStore(targetStream);

        final SocketIOServer server = new SocketIOServer(config);
        server.addEventListener("chatevent", ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
                server.getBroadcastOperations().sendEvent("chatevent", data);
            }
        });

        SIOCommands.init(server);

//        server.addEventListener("set-nickname", ChatObject.class, new DataListener<ChatObject>() {
//            @Override
//            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
//                System.out.println("" + data.getUserName() + " " + data.getMessage());
//            }
//        });

        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                System.out.println("new connection: " + client.getTransport());
            }
        });

        server.start();


        Thread.sleep(Integer.MAX_VALUE);

        server.stop();
    }


    public static void startServer(final int myPort) {

        Configuration config = new Configuration();
        config.setHostname("0.0.0.0");
        PORT = myPort + 100;
        config.setPort(PORT);


        final SocketIOServer server = new SocketIOServer(config);


        SIOCommands.init(server);


        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                System.out.println("New socketio connection: " + client.getTransport());
            }
        });

        server.startAsync();

        new Thread() {
            @Override
            public void run() {

                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (hostAvailabilityCheck()) {
                    System.out.println("webserver available");
                } else {
                    server.stop();
                    System.out.println("webserver stopped, because it was unresponsive!");
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("webserver restart");
                    startServer(myPort);
                }
            }
        }.start();

    }


    public static boolean hostAvailabilityCheck() {
        try (Socket s = new Socket("localhost", PORT)) {
            return true;
        } catch (IOException ex) {
            /* ignore */
        }
        return false;
    }
}
