package org.example;

import org.example.crypto.MessageCipher;
import org.example.protocol.InvalidPacketException;
import org.example.protocol.Message;
import org.example.protocol.Packet;
import org.example.protocol.ProtocolConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PacketRoundTripTest {
    private static final byte[] RAW_KEY = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);

    private PacketEncoder encoder;
    private PacketDecoder decoder;

    @BeforeEach
    void setUp() {
        SecretKey key = MessageCipher.keyFrom(RAW_KEY);
        MessageCipher cipher = new MessageCipher(key);
        encoder = new PacketEncoder(cipher);
        decoder = new PacketDecoder(cipher);
    }

    @Test
    void roundTrip() throws Exception {
        ExamplePayload original = new ExamplePayload("Hello, World!", 1716124800L);
        Message<ExamplePayload> msg = new Message<>(0x0001, 42, original);

        byte[] encoded = encoder.encode((byte) 1, msg);
        Packet<ExamplePayload> decoded = decoder.decode(encoded, ExamplePayload.class);

        assertEquals((byte) 1, decoded.getBSrc());
        assertEquals(0x0001, decoded.getMessage().getCType());
        assertEquals(42, decoded.getMessage().getBUserId());
        assertEquals(original, decoded.getMessage().getPayload());
    }

    @Test
    void headerCrcCorruption() throws Exception {
        byte[] packet = encoder.encode((byte) 1, new Message<>(1, 1, new ExamplePayload("test", 0)));
        packet[5] ^= (byte) 0xFF;
        assertThrows(InvalidPacketException.class, () -> decoder.decode(packet, ExamplePayload.class));
    }

    @Test
    void messageCrcCorruption() throws Exception {
        byte[] packet = encoder.encode((byte) 1, new Message<>(1, 1, new ExamplePayload("test", 0)));
        packet[20] ^= (byte) 0xFF;
        assertThrows(InvalidPacketException.class, () -> decoder.decode(packet, ExamplePayload.class));
    }

    @Test
    void badMagicByte() throws Exception {
        byte[] packet = encoder.encode((byte) 1, new Message<>(1, 1, new ExamplePayload("test", 0)));
        packet[0] = 0x00;
        assertThrows(InvalidPacketException.class, () -> decoder.decode(packet, ExamplePayload.class));
    }

    @Test
    void payloadIsEncrypted() throws Exception {
        String secret = "TOP_SECRET_COMMERCIAL_DATA";
        byte[] encoded = encoder.encode((byte) 1, new Message<>(1, 1, new ExamplePayload(secret, 999L)));
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        assertFalse(containsSubarray(encoded, secretBytes), "Plaintext must not appear in packet bytes");
    }

    @Test
    void pktIdIncrements() throws Exception {
        ExamplePayload p = new ExamplePayload("x", 0);
        byte[] pkt1 = encoder.encode((byte) 1, new Message<>(1, 1, p));
        byte[] pkt2 = encoder.encode((byte) 1, new Message<>(1, 1, p));
        long pktId1 = ByteBuffer.wrap(pkt1, 2, 8).getLong();
        long pktId2 = ByteBuffer.wrap(pkt2, 2, 8).getLong();
        assertEquals(pktId1 + 1, pktId2);
    }

    @Test
    void wLenConsistency() throws Exception {
        byte[] encoded = encoder.encode((byte) 1, new Message<>(1, 1, new ExamplePayload("test data", 12345L)));
        int wLen = ByteBuffer.wrap(encoded, 10, 4).getInt();
        assertEquals(encoded.length, wLen + ProtocolConstants.PACKET_OVERHEAD);
    }

    private boolean containsSubarray(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }
}
