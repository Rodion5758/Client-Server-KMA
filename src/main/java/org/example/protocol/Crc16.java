package org.example.protocol;

public final class Crc16 {
    private static final int POLYNOMIAL = 0x1021;

    public static int calculate(byte[] data, int offset, int length) {
        int crc = 0x0000;
        for (int i = offset; i < offset + length; i++) {
            crc ^= (data[i] & 0xFF) << 8;
            for (int j = 0; j < 8; j++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ POLYNOMIAL : crc << 1;
            }
            crc &= 0xFFFF;
        }
        return crc;
    }

    private Crc16() {}
}
