package org.example.net;

import org.example.protocol.ProtocolConstants;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class PacketFramer {

    private PacketFramer() {}

    public static byte[] readPacket(DataInputStream in) throws IOException {
        byte[] header = new byte[ProtocolConstants.HEADER_SIZE];
        in.readFully(header);

        int wLen = ByteBuffer.wrap(header).getInt(10);
        if (wLen < ProtocolConstants.MSG_HEADER_SIZE) {
            throw new IOException("Invalid wLen in packet header: " + wLen);
        }

        byte[] rest = new byte[wLen + 2];
        in.readFully(rest);

        byte[] full = new byte[ProtocolConstants.HEADER_SIZE + wLen + 2];
        System.arraycopy(header, 0, full, 0, ProtocolConstants.HEADER_SIZE);
        System.arraycopy(rest, 0, full, ProtocolConstants.HEADER_SIZE, rest.length);
        return full;
    }
}
