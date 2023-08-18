package edu.alibaba.mpc4j.s2pc.opf.oprf.psz14;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSenderOutput;

public class Psz14OriOprfSenderOutput implements OprfSenderOutput {

    /**
     * 批处理数量
     */
    private final int batchSize;
    /**
     * 伪随机函数输出字节长度
     */
    private final int prfByteLength;
    /**
     * H_1: {0,1}^* → {0,1}^{l}
     */
    private final Hash h1;
    /**
     * 为了避免ote中的线性关系对PRF结果随机性的影响，需要对ot结果先hash消除线性关系再xor
     */
    private final Hash h2;
    /**
     * h1 output length
     */
    private final int l;
    /**
     * LotSenderOutput
     */
    private final LcotSenderOutput lcotSenderOutput;

    Psz14OriOprfSenderOutput(EnvType envType, int batchSize, int l, LcotSenderOutput lotSenderOutput) {
        assert batchSize > 0 : "BatchSize must be greater than 0: " + batchSize;
        this.batchSize = batchSize;
        this.prfByteLength = lotSenderOutput.getOutputByteLength();
        this.l = l;
        h1 = HashFactory.createInstance(envType, l /Byte.SIZE);
        h2 = HashFactory.createInstance(envType, lotSenderOutput.getOutputByteLength());
        this.lcotSenderOutput = lotSenderOutput;
    }

    @Override
    public byte[] getPrf(int index, byte[] input) {
        byte[] hashedInput = h1.digestToBytes(input);
        byte[] hashedInputByte = new byte[1];
        System.arraycopy(hashedInput, 0, hashedInputByte, 0, 1);
        byte[] prf = lcotSenderOutput.getRb(index * l / Byte.SIZE, hashedInputByte);
        // 为了避免ote中的线性关系对PRF结果随机性的影响，需要对ot结果先hash消除线性关系再xor
        prf = h2.digestToBytes(prf);
        for (int i = 1; i < l / Byte.SIZE; i++) {
            System.arraycopy(hashedInput, i, hashedInputByte, 0, 1);
            BytesUtils.xori(prf, h2.digestToBytes(lcotSenderOutput.getRb(index * l / Byte.SIZE + i, hashedInputByte)));
        }
        return prf;
    }

    @Override
    public int getPrfByteLength() {
        return prfByteLength;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

}