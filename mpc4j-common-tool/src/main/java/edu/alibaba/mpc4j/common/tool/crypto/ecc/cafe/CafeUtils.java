package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

/**
 * Cafe utility functions.
 *
 * @author Weiran Liu
 * @date 2022/10/20
 */
final class CafeUtils {

    private CafeUtils() {
        // empty
    }

    /**
     * 将字节数组的3个字节解码为一个int。
     *
     * @param in     字节数组。
     * @param offset 偏移量。
     * @return 解码结果。
     */
    static int decode24(byte[] in, int offset) {
        int result = in[offset++] & 0xff;
        result |= (in[offset++] & 0xff) << 8;
        result |= (in[offset] & 0xff) << 16;
        return result;
    }

    /**
     * 将字节数组的4个字节解码为一个int。
     *
     * @param in     字节数组。
     * @param offset 偏移量。
     * @return 解码结果。
     */
    static long decode32(byte[] in, int offset) {
        int result = in[offset++] & 0xff;
        result |= (in[offset++] & 0xff) << 8;
        result |= (in[offset++] & 0xff) << 16;
        result |= in[offset] << 24;
        return ((long) result) & 0xffffffffL;
    }
}
