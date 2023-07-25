package edu.alibaba.mpc4j.s2pc.opf.oprf.rs21;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2k.Gf2kDokvs;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2k.Gf2kDokvsFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * RS21-MP-OPRF sender output.
 *
 * @author Weiran Liu
 * @date 2023/7/24
 */
public class Rs21MpOprfSenderOutput implements MpOprfSenderOutput {
    /**
     * batch size
     */
    private final int batchSize;
    /**
     * GF2K instance
     */
    private final Gf2k gf2k;
    /**
     * H^F: {0,1}^* → {0,1}^λ
     */
    private final Prf hf;
    /**
     * H^out: {0,1}^* → {0,1}^{λ}
     */
    private final Prf hOut;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * w = w_s + w_r
     */
    private final byte[] w;
    /**
     * GF2K-DOKVS
     */
    private final Gf2kDokvs<ByteBuffer> dokvs;
    /**
     * vector K, i.e., masked OKVS storage
     */
    private final byte[][] vectorK;

    Rs21MpOprfSenderOutput(EnvType envType, int batchSize, byte[] delta, byte[] w,
                           Gf2kDokvsFactory.Gf2kDokvsType dokvsType, byte[][] dokvsKeys, byte[][] vectorK) {
        MathPreconditions.checkPositive("batchSize", batchSize);
        this.batchSize = batchSize;
        gf2k = Gf2kFactory.createInstance(envType);
        hf = PrfFactory.createInstance(envType, gf2k.getByteL());
        hf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        hOut = PrfFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        hOut.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        Preconditions.checkArgument(gf2k.validateElement(delta));
        this.delta = delta;
        Preconditions.checkArgument(gf2k.validateElement(w));
        this.w = BytesUtils.clone(w);
        dokvs = Gf2kDokvsFactory.createInstance(envType, dokvsType, batchSize, dokvsKeys);
        MathPreconditions.checkEqual("m", "k.length", dokvs.getM(), vectorK.length);
        this.vectorK = Arrays.stream(vectorK)
            .peek(ki -> Preconditions.checkArgument(gf2k.validateElement(ki)))
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
    }

    @Override
    public byte[] getPrf(byte[] input) {
        ByteBuffer inputByteBuffer = ByteBuffer.wrap(input);
        // Decode(K, y, r) - ΔH^F(y) + w
        byte[] y1 = dokvs.decode(vectorK, inputByteBuffer);
        gf2k.subi(y1, gf2k.mul(delta, hf.getBytes(input)));
        gf2k.addi(y1, w);
        // H(y1, y) = H(Decode(K, y, r) - ΔH^F(y) + w, y)
        byte[] y1y = ByteBuffer.allocate(gf2k.getByteL() + input.length)
            .put(y1)
            .put(input)
            .array();
        return hOut.getBytes(y1y);
    }

    @Override
    public int getPrfByteLength() {
        return CommonConstants.BLOCK_BYTE_LENGTH;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }
}
