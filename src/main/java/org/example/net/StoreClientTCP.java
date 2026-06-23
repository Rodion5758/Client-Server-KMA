package org.example.net;

import org.example.PacketDecoder;
import org.example.PacketEncoder;
import org.example.crypto.MessageCipher;
import org.example.domain.CommandPayload;
import org.example.domain.CommandType;
import org.example.domain.ResponsePayload;
import org.example.protocol.Message;
import org.example.protocol.Packet;

import javax.crypto.SecretKey;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class StoreClientTCP implements AutoCloseable {
    private static final byte CLIENT_SRC = 0x02;
    private static final long RECONNECT_DELAY_MS = 500;
    private static final long MAX_RECONNECT_DELAY_MS = 5_000;

    private final String host;
    private final int port;
    private final int userId;
    private final PacketEncoder encoder;
    private final PacketDecoder decoder;

    private Socket socket;
    private DataInputStream in;
    private OutputStream out;

    public StoreClientTCP(String host, int port, int userId, MessageCipher cipher) {
        this.host = host;
        this.port = port;
        this.userId = userId;
        this.encoder = new PacketEncoder(cipher);
        this.decoder = new PacketDecoder(cipher);
    }

    public ResponsePayload send(int cType, CommandPayload payload) throws InterruptedException {
        Message<CommandPayload> msg = new Message<>(cType, userId, payload);

        while (true) {
            ensureConnected();
            try {
                byte[] encoded = encoder.encode(CLIENT_SRC, msg);
                out.write(encoded);
                out.flush();

                byte[] raw = PacketFramer.readPacket(in);
                Packet<ResponsePayload> reply = decoder.decode(raw, ResponsePayload.class);
                return reply.getMessage().getPayload();
            } catch (IOException e) {
                System.out.println("[TCP-Client] Connection lost: " + e.getMessage() + " — reconnecting...");
                closeQuietly();
            } catch (Exception e) {
                System.err.println("[TCP-Client] Protocol error: " + e.getMessage());
                closeQuietly();
            }
        }
    }

    private void ensureConnected() throws InterruptedException {
        if (socket != null && socket.isConnected() && !socket.isClosed()) return;
        connectWithRetry();
    }

    private void connectWithRetry() throws InterruptedException {
        long delay = RECONNECT_DELAY_MS;
        while (true) {
            try {
                socket = new Socket(host, port);
                in = new DataInputStream(socket.getInputStream());
                out = socket.getOutputStream();
                System.out.println("[TCP-Client] Connected to " + host + ":" + port);
                return;
            } catch (IOException e) {
                System.out.println("[TCP-Client] Server unavailable, retrying in " + delay + "ms...");
                Thread.sleep(delay);
                delay = Math.min(delay * 2, MAX_RECONNECT_DELAY_MS);
            }
        }
    }

    private void closeQuietly() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null;
        in = null;
        out = null;
    }

    @Override
    public void close() {
        closeQuietly();
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9090;
        int userId = args.length > 2 ? Integer.parseInt(args[2]) : 1;

        byte[] rawKey = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        SecretKey key = MessageCipher.keyFrom(rawKey);
        MessageCipher cipher = new MessageCipher(key);

        try (StoreClientTCP client = new StoreClientTCP(host, port, userId, cipher)) {
            String[] products = {"grechka", "milk", "bread", "sugar", "salt"};
            java.util.Random rnd = new java.util.Random();

            for (int i = 0; i < 20; i++) {
                String product = products[rnd.nextInt(products.length)];
                CommandPayload payload = new CommandPayload(product, null, rnd.nextInt(5) + 1, null);
                ResponsePayload reply = client.send(CommandType.ADD, payload);
                System.out.printf("[TCP-Client user=%d] ADD %s → ok=%b msg=%s%n",
                        userId, product, reply.isOk(), reply.getMessage());
                Thread.sleep(300);
            }

            ResponsePayload qty = client.send(CommandType.GET_QUANTITY,
                    new CommandPayload("grechka", null, null, null));
            System.out.printf("[TCP-Client user=%d] GET grechka → %s%n", userId, qty.getMessage());
        }
    }
}
