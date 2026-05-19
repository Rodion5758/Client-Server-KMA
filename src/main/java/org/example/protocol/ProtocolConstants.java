package org.example.protocol;

public final class ProtocolConstants {
    public static final byte MAGIC = 0x13;
    public static final int HEADER_CRC_COVERED = 14;  // bytes 0-13
    public static final int HEADER_SIZE = 16;          // 14 bytes + 2-byte CRC
    public static final int MSG_HEADER_SIZE = 8;       // cType(4) + bUserId(4)
    public static final int PACKET_OVERHEAD = 18;      // HEADER_SIZE(16) + msg CRC(2)

    private ProtocolConstants() {}
}
