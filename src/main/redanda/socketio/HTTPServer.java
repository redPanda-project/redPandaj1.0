package main.redanda.socketio;


import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HTTPServer extends Thread {

    private int PORT;

    public HTTPServer(int PORT) {
        this.PORT = PORT;
    }

    public static void main(String[] args) throws Exception {
        new HTTPServer(8080).start();
    }

    @Override
    public void run() {
        try {
            System.out.println("starting HTTP server...");
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/android.apk", new HHandler());
            //server.setExecutor(null); // creates a default executor
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class HHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Headers h = t.getResponseHeaders();


            Path path = Paths.get("android.apk");
            byte[] data = Files.readAllBytes(path);


//            h.add("Content-Type", "application/json");
            h.add("Content-Type", "application/octet-stream");

            t.sendResponseHeaders(200, data.length);
            OutputStream os = t.getResponseBody();
            os.write(data);
            os.close();
        }
    }
}

