package com.elvarg.world.collision;

public class XTEA {

    public static void decrypt(byte[] data, int[] keys, int start, int end) {
        int num_blocks = (end - start) / 8;
        int index = start;

        for (int block = 0; block < num_blocks; block++) {
            int v0 = readInt(data, index);
            int v1 = readInt(data, index + 4);
            int sum = 0;
            int num_rounds = 32;
            int delta = 0x61C88647;

            while (num_rounds-- > 0) {
                v1 -= (((v0 << 4) ^ (v0 >>> 5)) + v0) ^ (sum + keys[(sum >>> 11) & 3]);
                sum -= delta;
                v0 -= (((v1 << 4) ^ (v1 >>> 5)) + v1) ^ (sum + keys[sum & 3]);
            }

            writeInt(data, index, v0);
            writeInt(data, index + 4, v1);
            index += 8;
        }
    }

    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16) | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
    }

    private static void writeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >> 24);
        data[offset + 1] = (byte) (value >> 16);
        data[offset + 2] = (byte) (value >> 8);
        data[offset + 3] = (byte) value;
    }
}