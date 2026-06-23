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
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

public class StoreServerUDP {
    private static final byte SERVER_SRC = 0x01;
    private static final int MAX_PACKET_SIZE = 65507;
    private static final int DEDUP_CACHE_SIZE = 1024;

    private final int port;
    private final ProductService warehouse;
    private final MessageCipher cipher;

    private volatile Predicate<String> dropReplyPredicate;

    private DatagramSocket socket;
    private Thread receiveThread;
    private ExecutorService workers;
    private volatile boolean running;

    private final Map<String, byte[]> dedupCache = Collections.synchronizedMap(
            new LinkedHashMap<>(DEDUP_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                    return size() > DEDUP_CACHE_SIZE;
                }
            }
    );

    public StoreServerUDP(int port, ProductService warehouse, MessageCipher cipher) {
        this.port = port;
        this.warehouse = warehouse;
        this.cipher = cipher;
    }

    public void setDropReplyPredicate(Predicate<String> pred) {
        this.dropReplyPredicate = pred;
    }

    public void start() throws IOException {
        socket = new DatagramSocket(port);
        workers = Executors.newCachedThreadPool();
        running = true;
        receiveThread = new Thread(this::receiveLoop, "UDP-receive");
        receiveThread.start();
        System.out.println("[UDP-Server] Listening on port " + socket.getLocalPort());
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }

    public void stop() throws InterruptedException {
        running = false;
        socket.close();
        workers.shutdownNow();
        receiveThread.join();
    }

    private void receiveLoop() {
        byte[] buf = new byte[MAX_PACKET_SIZE];
        while (running) {
            DatagramPacket dgram = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(dgram);
                byte[] data = Arrays.copyOf(dgram.getData(), dgram.getLength());
                InetAddress addr = dgram.getAddress();
                int clientPort = dgram.getPort();
                workers.submit(() -> handleDatagram(data, addr, clientPort));
            } catch (IOException e) {
                if (running) System.err.println("[UDP-Server] Receive error: " + e.getMessage());
            }
        }
    }

    private void handleDatagram(byte[] data, InetAddress addr, int clientPort) {
        PacketDecoder decoder = new PacketDecoder(cipher);
        PacketEncoder encoder = new PacketEncoder(cipher);
        CommandHandler handler = new CommandHandler(warehouse);

        try {
            Packet<CommandPayload> pkt = decoder.decode(data, CommandPayload.class);
            String key = addr.getHostAddress() + ":" + clientPort + ":" + pkt.getBPktId();

            byte[] cached = dedupCache.get(key);
            if (cached != null) {
                System.out.println("[UDP-Server] Duplicate request " + key + " → resending cached reply");
                sendReply(cached, addr, clientPort);
                return;
            }

            Message<ResponsePayload> response = handler.handle(pkt.getMessage());
            byte[] encoded = encoder.encode(SERVER_SRC, response);

            dedupCache.put(key, encoded);

            Predicate<String> drop = dropReplyPredicate;
            if (drop != null && drop.test(key)) {
                System.out.println("[UDP-Server] [TEST] Dropping reply for " + key);
                dropReplyPredicate = null;
                return;
            }

            sendReply(encoded, addr, clientPort);
        } catch (Exception e) {
            System.err.println("[UDP-Server] Error handling datagram from " + addr + ":" + clientPort + ": " + e.getMessage());
        }
    }

    private void sendReply(byte[] data, InetAddress addr, int port) throws IOException {
        DatagramPacket reply = new DatagramPacket(data, data.length, addr, port);
        socket.send(reply);
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9091;
        byte[] rawKey = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        SecretKey key = MessageCipher.keyFrom(rawKey);
        MessageCipher cipher = new MessageCipher(key);

        StoreServerUDP server = new StoreServerUDP(port, new ProductService(), cipher);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { server.stop(); } catch (Exception ignored) {}
        }));

        Thread.currentThread().join();
    }
}
