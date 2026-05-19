package org.example;

import com.google.gson.Gson;
import org.example.crypto.MessageCipher;
import org.example.protocol.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PacketDecoder {
    private final MessageCipher cipher;
    private final Gson gson = new Gson();

    public PacketDecoder(MessageCipher cipher) {
        this.cipher = cipher;
    }

    public <T> Packet<T> decode(byte[] data, Class<T> payloadClass) throws Exception {
        if (data.length < ProtocolConstants.PACKET_OVERHEAD) {
            throw new InvalidPacketException("Packet too short: " + data.length);
        }

        byte bMagic = data[0];
        if (bMagic != ProtocolConstants.MAGIC) {
            throw new InvalidPacketException("Invalid magic byte: 0x" + Integer.toHexString(bMagic & 0xFF));
        }

        ByteBuffer buf = ByteBuffer.wrap(data);

        int computedHdrCrc = Crc16.calculate(data, 0, ProtocolConstants.HEADER_CRC_COVERED);
        int storedHdrCrc = buf.getShort(14) & 0xFFFF;
        if (computedHdrCrc != storedHdrCrc) {
            throw new InvalidPacketException("Header CRC mismatch");
        }

        int wLen = buf.getInt(10);
        if (wLen < ProtocolConstants.MSG_HEADER_SIZE) {
            throw new InvalidPacketException("wLen too small: " + wLen);
        }
        if (data.length != wLen + ProtocolConstants.PACKET_OVERHEAD) {
            throw new InvalidPacketException("Length mismatch: expected " + (wLen + ProtocolConstants.PACKET_OVERHEAD) + ", got " + data.length);
        }

        int computedMsgCrc = Crc16.calculate(data, ProtocolConstants.HEADER_SIZE, wLen);
        int storedMsgCrc = buf.getShort(ProtocolConstants.HEADER_SIZE + wLen) & 0xFFFF;
        if (computedMsgCrc != storedMsgCrc) {
            throw new InvalidPacketException("Message CRC mismatch");
        }

        byte bSrc = data[1];
        long bPktId = buf.getLong(2);
        int cType = buf.getInt(16);
        int bUserId = buf.getInt(20);

        int payloadStart = ProtocolConstants.HEADER_SIZE + ProtocolConstants.MSG_HEADER_SIZE;
        int payloadLen = wLen - ProtocolConstants.MSG_HEADER_SIZE;
        byte[] encryptedPayload = new byte[payloadLen];
        System.arraycopy(data, payloadStart, encryptedPayload, 0, payloadLen);

        byte[] jsonBytes = cipher.decrypt(encryptedPayload);
        T payload = gson.fromJson(new String(jsonBytes, StandardCharsets.UTF_8), payloadClass);

        return new Packet<>(bSrc, bPktId, new Message<>(cType, bUserId, payload));
    }
}
