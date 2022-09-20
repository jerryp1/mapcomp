package edu.alibaba.mpc4j.common.tool.crypto.hash;

/**
 * JDK的SHA3-256哈希函数。
 *
 * @author Weiran Liu
 * @date 2022/9/14
 */
public class JdkSha3Series256Hash extends AbstractJdkHash {
    /**
     * JDK的SHA256哈希函数算法名称
     */
    private static final String JDK_HASH_NAME = "SHA3-256";
    /**
     * 单位输出长度
     */
    static final int DIGEST_BYTE_LENGTH = 32;

    JdkSha3Series256Hash(int outputByteLength) {
        super(HashFactory.HashType.JDK_SHA3_256, JDK_HASH_NAME, DIGEST_BYTE_LENGTH, outputByteLength);
    }
}
