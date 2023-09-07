package edu.alibaba.mpc4j.s2pc.opf.oprf.psz14;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSenderOutput;

public class Psz14OptOprfSenderOutput implements OprfSenderOutput {

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
     * LotSenderOutput
     */
    private final LcotSenderOutput lcotSenderOutput;

    Psz14OptOprfSenderOutput(EnvType envType, int batchSize, int l, LcotSenderOutput lcotSenderOutput) {
        assert batchSize > 0 : "BatchSize must be greater than 0: " + batchSize;
        this.batchSize = batchSize;
        this.prfByteLength = lcotSenderOutput.getOutputByteLength();
        h1 = HashFactory.createInstance(envType, l /Byte.SIZE);
        this.lcotSenderOutput = lcotSenderOutput;
    }

    @Override
    public byte[] getPrf(int index, byte[] input) { return lcotSenderOutput.getRb(index, h1.digestToBytes(input)); }

    @Override
    public int getPrfByteLength() {
        return prfByteLength;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

}