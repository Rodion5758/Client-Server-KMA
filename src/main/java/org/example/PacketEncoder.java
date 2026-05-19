package org.example;

import com.google.gson.Gson;
import org.example.crypto.MessageCipher;
import org.example.protocol.Crc16;
import org.example.protocol.Message;
import org.example.protocol.ProtocolConstants;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

public class PacketEncoder {
    private final MessageCipher cipher;
    private final Gson gson = new Gson();
    private final AtomicLong pktIdCounter = new AtomicLong(1);

    public PacketEncoder(MessageCipher cipher) {
        this.cipher = cipher;
    }

    public <T> byte[] encode(byte bSrc, Message<T> message) throws Exception {
        byte[] jsonBytes = gson.toJson(message.getPayload()).getBytes(StandardCharsets.UTF_8);
        byte[] encryptedPayload = cipher.encrypt(jsonBytes);

        int wLen = ProtocolConstants.MSG_HEADER_SIZE + encryptedPayload.length;
        ByteBuffer bMsgBuf = ByteBuffer.allocate(wLen);
        bMsgBuf.putInt(message.getCType());
        bMsgBuf.putInt(message.getBUserId());
        bMsgBuf.put(encryptedPayload);
        byte[] bMsg = bMsgBuf.array();

        long pktId = pktIdCounter.getAndIncrement();
        ByteBuffer headerBuf = ByteBuffer.allocate(ProtocolConstants.HEADER_CRC_COVERED);
        headerBuf.put(ProtocolConstants.MAGIC);
        headerBuf.put(bSrc);
        headerBuf.putLong(pktId);
        headerBuf.putInt(wLen);
        byte[] header = headerBuf.array();

        int headerCrc = Crc16.calculate(header, 0, header.length);
        int msgCrc = Crc16.calculate(bMsg, 0, bMsg.length);

        ByteBuffer packet = ByteBuffer.allocate(ProtocolConstants.HEADER_SIZE + wLen + 2);
        packet.put(header);
        packet.putShort((short) headerCrc);
        packet.put(bMsg);
        packet.putShort((short) msgCrc);
        return packet.array();
    }
}
