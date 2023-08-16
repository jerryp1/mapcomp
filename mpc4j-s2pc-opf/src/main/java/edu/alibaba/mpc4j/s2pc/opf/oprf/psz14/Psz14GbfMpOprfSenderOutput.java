package edu.alibaba.mpc4j.s2pc.opf.oprf.psz14;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;


public class Psz14GbfMpOprfSenderOutput implements MpOprfSenderOutput {

    /**
     * 批处理数量
     */
    private final int batchSize;
    /**
     * 关联密钥
     */
    private final byte[][] q1;
    /**
     * OKVS
     */
    private final Gf2eDokvs<byte[]> okvs;
    /**
     * 伪随机函数输出字节长度
     */
    private final int prfByteLength;

    Psz14GbfMpOprfSenderOutput(EnvType envType, int batchSize, byte[][] q1, byte[][] keys, Gf2eDokvsType okvsType) {
        assert batchSize > 0 : "BatchSize must be greater than 0: " + batchSize;
        this.batchSize = batchSize;
        this.prfByteLength = CommonConstants.BLOCK_BIT_LENGTH / Byte.SIZE;
        this.q1 = q1;
        // OKVS init
        this.okvs = Gf2eDokvsFactory.createBinaryInstance(envType, okvsType, batchSize, CommonConstants.BLOCK_BIT_LENGTH, keys);
    }

    @Override
    public byte[] getPrf(byte[] input) { return this.okvs.decode(q1, input); }

    @Override
    public int getPrfByteLength() {
        return prfByteLength;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

}
