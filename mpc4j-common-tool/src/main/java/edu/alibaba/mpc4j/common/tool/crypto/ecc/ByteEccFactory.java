package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.bc.*;

/**
 * 字节椭圆曲线工厂。
 *
 * @author Weiran Liu
 * @date 2022/9/1
 */
public class ByteEccFactory {
    /**
     * 私有构造函数。
     */
    private ByteEccFactory() {
        // empty
    }

    /**
     * 字节椭圆曲线枚举类
     */
    public enum ByteEccType {
        /**
         * libsodium实现的ED25519曲线
         */
        ED25519_LIBSODIUM,
        /**
         * BC实现的ED25519曲线
         */
        ED25519_BC,
    }

    /**
     * 创建字节椭圆曲线。
     *
     * @param byteEccType 字节椭圆曲线类型。
     * @return 字节椭圆曲线。
     */
    public static ByteEcc createInstance(ByteEccType byteEccType) {
        switch (byteEccType) {
            case ED25519_BC:
                return new Ed25519BcByteEcc();
            case ED25519_LIBSODIUM:
            default:
                throw new IllegalArgumentException(
                    "Invalid " + ByteEccType.class.getSimpleName() + ": " + byteEccType.name()
                );
        }
    }

    /**
     * 创建字节椭圆曲线。
     *
     * @param envType 环境类型。
     * @return 字节椭圆曲线。
     */
    public static ByteEcc createInstance(EnvType envType) {
        switch (envType) {
            case STANDARD:
            case STANDARD_JDK:
            case INLAND:
            case INLAND_JDK:
                return createInstance(ByteEccType.ED25519_BC);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
