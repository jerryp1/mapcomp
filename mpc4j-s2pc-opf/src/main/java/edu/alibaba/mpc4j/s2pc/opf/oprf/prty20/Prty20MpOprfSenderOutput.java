package edu.alibaba.mpc4j.s2pc.opf.oprf.prty20;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoder;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.BinaryGf2eDokvs;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSenderOutput;

import java.util.Arrays;

public class Prty20MpOprfSenderOutput implements MpOprfSenderOutput {

    /**
     * 批处理数量
     */
    private final int batchSize;
    /**
     * 编码器
     */
    private final LinearCoder linearCoder;
    /**
     * 关联密钥
     */
    private final byte[][] qs;
    /**
     * delta
     */
    private final byte[] delta;
    /**
     * OKVS
     * TODO 确认下是不是这个类型的
     */
    private final BinaryGf2eDokvs<byte[]> okvs;
    /**
     * H_1: {0,1}^* → {0,1}^{l1}
     */
    private final Hash h1;
    /**
     * 伪随机函数输出字节长度
     */
    private final int prfByteLength;

    Prty20MpOprfSenderOutput(EnvType envType, int batchSize, LcotSenderOutput lotSenderOutput, Gf2eDokvsType type, int n, int l, byte[][] keys) {
        assert batchSize > 0 : "BatchSize must be greater than 0: " + batchSize;
        this.batchSize = batchSize;
        this.linearCoder = lotSenderOutput.getLinearCoder();
        this.delta = lotSenderOutput.getDelta();
        this.prfByteLength = linearCoder.getCodewordByteLength();
        // OKVS init
        this.okvs = Gf2eDokvsFactory.createBinaryInstance(envType, type, n, linearCoder.getCodewordByteLength() * Byte.SIZE, keys);
        this.qs = Arrays.stream(lotSenderOutput.getQsArray())
                .peek(q -> {
                    assert q.length == linearCoder.getCodewordByteLength();
                })
                .map(BytesUtils::clone)
                .toArray(byte[][]::new);
        //
        h1 = HashFactory.createInstance(envType, CommonUtils.getByteLength(l));
    }

    @Override
    public byte[] getPrf(byte[] input) {
        // 计算encode = Decode(q_i) ⊕ (C(m_i) ⊙ Δ)
        byte[] encode = linearCoder.encode(BytesUtils.paddingByteArray(h1.digestToBytes(input),linearCoder.getDatawordByteLength()));
        BytesUtils.andi(encode, delta);
        byte[] decode = okvs.decode(qs,input);
        return BytesUtils.xor(encode, decode);

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
