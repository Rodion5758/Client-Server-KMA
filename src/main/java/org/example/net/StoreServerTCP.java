package org.example.net;

import org.example.PacketDecoder;
import org.example.PacketEncoder;
import org.example.crypto.MessageCipher;
import org.example.domain.CommandHandler;
import org.example.domain.CommandPayload;
import org.example.domain.ResponsePayload;
import org.example.domain.ProductService;
import org.example.protocol.Message;
import org.example.protocol.Packet;

import javax.crypto.SecretKey;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoreServerTCP {
    private static final byte SERVER_SRC = 0x01;

    private final int port;
    private final ProductService warehouse;
    private final MessageCipher cipher;

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private ExecutorService workers;
    private volatile boolean running;

    public StoreServerTCP(int port, ProductService warehouse, MessageCipher cipher) {
        this.port = port;
        this.warehouse = warehouse;
        this.cipher = cipher;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        workers = Executors.newCachedThreadPool();
        running = true;
        acceptThread = new Thread(this::acceptLoop, "TCP-accept");
        acceptThread.start();
        System.out.println("[TCP-Server] Listening on port " + serverSocket.getLocalPort());
    }

    public int getLocalPort() {
        return serverSocket.getLocalPort();
    }

    public void stop() throws InterruptedException, IOException {
        running = false;
        serverSocket.close();
        workers.shutdownNow();
        acceptThread.join();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                workers.submit(() -> handleClient(client));
            } catch (IOException e) {
                if (running) System.err.println("[TCP-Server] Accept error: " + e.getMessage());
            }
        }
    }

    private void handleClient(Socket socket) {
        String addr = socket.getRemoteSocketAddress().toString();
        System.out.println("[TCP-Server] Client connected: " + addr);

        PacketDecoder decoder = new PacketDecoder(cipher);
        PacketEncoder encoder = new PacketEncoder(cipher);
        CommandHandler handler = new CommandHandler(warehouse);

        try (socket;
             DataInputStream in = new DataInputStream(socket.getInputStream());
             OutputStream out = socket.getOutputStream()) {

            while (!Thread.currentThread().isInterrupted()) {
                byte[] raw = PacketFramer.readPacket(in);
                Packet<CommandPayload> pkt = decoder.decode(raw, CommandPayload.class);
                Message<ResponsePayload> response = handler.handle(pkt.getMessage());
                byte[] encoded = encoder.encode(SERVER_SRC, response);
                out.write(encoded);
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("[TCP-Server] Client disconnected: " + addr);
        } catch (Exception e) {
            System.err.println("[TCP-Server] Error handling " + addr + ": " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9090;
        byte[] rawKey = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        SecretKey key = MessageCipher.keyFrom(rawKey);
        MessageCipher cipher = new MessageCipher(key);

        StoreServerTCP server = new StoreServerTCP(port, new ProductService(), cipher);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { server.stop(); } catch (Exception ignored) {}
        }));

        Thread.currentThread().join();
    }
}
