package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * abstract hint for PIANO.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public abstract class AbstractPianoHint implements PianoHint {
    /**
     * environment
     */
    protected final EnvType envType;
    /**
     * chunk size
     */
    protected final int chunkSize;
    /**
     * chunk num
     */
    protected final int chunkNum;
    /**
     * parity bit length
     */
    protected final int l;
    /**
     * parity byte length
     */
    protected final int byteL;
    /**
     * PRF
     */
    protected final Prf prf;
    /**
     * parity
     */
    protected final byte[] parity;

    protected AbstractPianoHint(EnvType envType, int chunkSize, int chunkNum, int l) {
        this.envType = envType;
        MathPreconditions.checkPositive("chunkSize", chunkSize);
        this.chunkSize = chunkSize;
        MathPreconditions.checkPositive("chunkNum", chunkNum);
        this.chunkNum = chunkNum;
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        // initialize a PRF
        prf = PrfFactory.createInstance(envType, Integer.BYTES);
        // initialize the parity to zero
        parity = new byte[byteL];
    }

    @Override
    public byte[] getParity() {
        return parity;
    }

    @Override
    public void xori(byte[] otherParity) {
        Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(otherParity, byteL, l));
        BytesUtils.xori(parity, otherParity);
    }

    @Override
    public int getChunkSize() {
        return chunkSize;
    }

    @Override
    public int getChunkNum() {
        return chunkNum;
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public int getByteL() {
        return byteL;
    }
}
