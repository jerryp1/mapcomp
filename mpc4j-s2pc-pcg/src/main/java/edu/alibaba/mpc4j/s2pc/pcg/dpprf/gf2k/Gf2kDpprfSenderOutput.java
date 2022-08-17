package edu.alibaba.mpc4j.s2pc.pcg.dpprf.gf2k;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.Arrays;

/**
 * GF2K-DPPRF协议发送方输出。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class Gf2kDpprfSenderOutput {
    /**
     * α上界
     */
    private final int alphaBound;
    /**
     * α比特长度
     */
    private final int l;
    /**
     * 批处理数量个PRF密钥，每个密钥都有α上界个元素
     */
    private final byte[][][] prfKeys;
    /**
     * 批处理数量
     */
    private final int batchNum;

    public Gf2kDpprfSenderOutput(int alphaBound, byte[][][] prfKeys) {
        // 批处理数量设置
        batchNum = prfKeys.length;
        assert batchNum > 0 : "Batch Num must be greater than 0: " + batchNum;
        // α设置
        assert alphaBound > 0 : "AlphaBound must be greater than 0: " + alphaBound;
        this.alphaBound = alphaBound;
        l = LongUtils.ceilLog2(alphaBound);
        this.prfKeys = Arrays.stream(prfKeys)
            .peek(prfKey -> {
                // 每一个PRF密钥都有α上界个
                assert prfKey.length == alphaBound : "PrfKey length should be " + alphaBound + ": " + prfKey.length;
            })
            .toArray(byte[][][]::new);
    }

    /**
     * 返回l。
     *
     * @return l。
     */
    public int getL() {
        return l;
    }

    /**
     * 返回α上界。
     *
     * @return α上界。
     */
    public int getAlphaBound() {
        return alphaBound;
    }

    /**
     * 返回批处理数量。
     *
     * @return 批处理数量。
     */
    public int getBatchNum() {
        return batchNum;
    }

    /**
     * 返回批处理索引下所有PRF取值。
     *
     * @param batchIndex 批处理索引。
     * @return 批处理索引下所有PRF取值。
     */
    public byte[][] getPrfKey(int batchIndex) {
        return prfKeys[batchIndex];
    }
}
