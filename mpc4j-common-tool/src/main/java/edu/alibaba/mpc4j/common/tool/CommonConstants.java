package edu.alibaba.mpc4j.common.tool;

import java.nio.charset.Charset;

/**
 * 定义常数。
 *
 * @author Weiran Liu
 * @date 2020/04/13
 */
public class CommonConstants {
    /**
     * 私有构造函数。
     */
    private CommonConstants() {
        // empty
    }

    /**
     * 字符串编码名称
     */
    public static final String DEFAULT_CHARSET_NAME = "UTF-8";
    /**
     * 字符串编码字符集
     */
    public static final Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_CHARSET_NAME);
    /**
     * 本地工具库名称
     */
    public static final String MPC4J_NATIVE_TOOL_NAME = "mpc4j-native-tool";
    /**
     * 本地全同态库名称
     */
    public static final String MPC4J_NATIVE_FHE_NAME = "mpc4j-native-fhe";
    /**
     * 分组比特长度，等价于密钥比特长度
     */
    public static final int BLOCK_BIT_LENGTH = 128;
    /**
     * 分组字节长度，等价于密钥字节长度
     */
    public static final int BLOCK_BYTE_LENGTH = 16;
    /**
     * 分组长整数长度，等价于密钥字节长度
     */
    public static final int BLOCK_LONG_LENGTH = 2;
    /**
     * 统计安全性比特长度
     */
    public static final int STATS_BIT_LENGTH = 40;
    /**
     * 统计安全性字节长度
     */
    public static final int STATS_BYTE_LENGTH = 5;

    public static final int[] ONLY_ONE_BIT_ONE = new int[]{
        0b00000001,
        0b00000010,
        0b00000100,
        0b00001000,
        0b00010000,
        0b00100000,
        0b01000000,
        0b10000000
    };
    public static final int[] ONLY_ONE_BIT_ZERO = new int[]{
        0b11111110,
        0b11111101,
        0b11111011,
        0b11110111,
        0b11101111,
        0b11011111,
        0b10111111,
        0b01111111
    };
}
