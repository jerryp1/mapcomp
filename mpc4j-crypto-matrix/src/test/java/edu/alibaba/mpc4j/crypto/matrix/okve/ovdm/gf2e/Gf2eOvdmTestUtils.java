package edu.alibaba.mpc4j.crypto.matrix.okve.ovdm.gf2e;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * GF(2^l)-OVDM测试工具类。
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
class Gf2eOvdmTestUtils {
    /**
     * 私有构造函数
     */
    private Gf2eOvdmTestUtils() {
        // empty
    }

    /**
     * 默认有限域比特长度
     */
    static final int DEFAULT_L = 64;
    /**
     * 默认L字节长度
     */
    static final int DEFAULT_L_BYTE_LENGTH = CommonUtils.getByteLength(DEFAULT_L);
    /**
     * 随机状态
     */
    static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static Map<ByteBuffer, byte[]> randomKeyValueMap(int size) {
        Map<ByteBuffer, byte[]> keyValueMap = new HashMap<>();
        IntStream.range(0, size).forEach(index -> {
            byte[] keyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(keyBytes);
            byte[] valueBytes = new byte[DEFAULT_L_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(valueBytes);
            keyValueMap.put(ByteBuffer.wrap(keyBytes), valueBytes);
        });
        return keyValueMap;
    }
}
