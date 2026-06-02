package org.example.protocol;

public final class ProtocolConstants {
    public static final byte MAGIC = 0x13;
    public static final int HEADER_CRC_COVERED = 14;
    public static final int HEADER_SIZE = 16;
    public static final int MSG_HEADER_SIZE = 8;
    public static final int PACKET_OVERHEAD = 18;

    private ProtocolConstants() {}
}
