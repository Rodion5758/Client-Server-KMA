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
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class StoreClientUDP implements AutoCloseable {
    private static final byte CLIENT_SRC = 0x02;
    private static final int DEFAULT_TIMEOUT_MS = 2_000;
    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final int MAX_PACKET_SIZE = 65507;

    private final InetAddress serverAddr;
    private final int serverPort;
    private final int userId;
    private final int timeoutMs;
    private final int maxRetries;
    private final PacketEncoder encoder;
    private final PacketDecoder decoder;
    private final DatagramSocket socket;

    public StoreClientUDP(String host, int port, int userId, MessageCipher cipher)
            throws IOException {
        this(host, port, userId, cipher, DEFAULT_TIMEOUT_MS, DEFAULT_MAX_RETRIES);
    }

    public StoreClientUDP(String host, int port, int userId, MessageCipher cipher,
                          int timeoutMs, int maxRetries) throws IOException {
        this.serverAddr = InetAddress.getByName(host);
        this.serverPort = port;
        this.userId = userId;
        this.timeoutMs = timeoutMs;
        this.maxRetries = maxRetries;
        this.encoder = new PacketEncoder(cipher);
        this.decoder = new PacketDecoder(cipher);
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(timeoutMs);
    }

    public ResponsePayload send(int cType, CommandPayload payload) throws Exception {
        Message<CommandPayload> msg = new Message<>(cType, userId, payload);
        byte[] encoded = encoder.encode(CLIENT_SRC, msg);

        long sentPktId = ByteBuffer.wrap(encoded).getLong(2);

        DatagramPacket sendPkt = new DatagramPacket(encoded, encoded.length, serverAddr, serverPort);
        byte[] recvBuf = new byte[MAX_PACKET_SIZE];
        DatagramPacket recvPkt = new DatagramPacket(recvBuf, recvBuf.length);

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            socket.send(sendPkt);
            System.out.printf("[UDP-Client user=%d] Sent pktId=%d cType=%d (attempt %d)%n",
                    userId, sentPktId, cType, attempt + 1);

            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                recvPkt.setLength(recvBuf.length);
                try {
                    socket.receive(recvPkt);
                } catch (SocketTimeoutException e) {
                    break;
                }

                if (!serverAddr.equals(recvPkt.getAddress()) || serverPort != recvPkt.getPort()) {
                    continue;
                }

                byte[] raw = new byte[recvPkt.getLength()];
                System.arraycopy(recvBuf, 0, raw, 0, raw.length);

                try {
                    Packet<ResponsePayload> reply = decoder.decode(raw, ResponsePayload.class);
                    return reply.getMessage().getPayload();
                } catch (Exception e) {
                    System.err.println("[UDP-Client] Bad reply: " + e.getMessage());
                }
            }

            if (attempt < maxRetries) {
                System.out.printf("[UDP-Client user=%d] Timeout waiting for pktId=%d, retransmitting...%n",
                        userId, sentPktId);
            }
        }

        throw new IOException("No reply after " + maxRetries + " retransmits for pktId=" + sentPktId);
    }

    @Override
    public void close() {
        socket.close();
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9091;
        int userId = args.length > 2 ? Integer.parseInt(args[2]) : 1;

        byte[] rawKey = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        SecretKey key = MessageCipher.keyFrom(rawKey);
        MessageCipher cipher = new MessageCipher(key);

        try (StoreClientUDP client = new StoreClientUDP(host, port, userId, cipher)) {
            String[] products = {"grechka", "milk", "bread", "sugar", "salt"};
            java.util.Random rnd = new java.util.Random();

            for (int i = 0; i < 10; i++) {
                String product = products[rnd.nextInt(products.length)];
                CommandPayload payload = new CommandPayload(product, null, rnd.nextInt(5) + 1, null);
                ResponsePayload reply = client.send(CommandType.ADD, payload);
                System.out.printf("[UDP-Client user=%d] ADD %s → ok=%b msg=%s%n",
                        userId, product, reply.isOk(), reply.getMessage());
                Thread.sleep(200);
            }

            ResponsePayload qty = client.send(CommandType.GET_QUANTITY,
                    new CommandPayload("grechka", null, null, null));
            System.out.printf("[UDP-Client user=%d] GET grechka → %s%n", userId, qty.getMessage());
        }
    }
}
