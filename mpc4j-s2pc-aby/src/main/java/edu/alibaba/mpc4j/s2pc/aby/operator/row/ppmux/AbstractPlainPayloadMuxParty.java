package edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * Abstract plain mux party.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
public abstract class AbstractPlainPayloadMuxParty extends AbstractTwoPartyPto implements PlainPayloadMuxParty {
    /**
     * max num
     */
    protected int maxNum;
    /**
     * num
     */
    protected int num;
    /**
     * Zl instance
     */
    protected Zl zl;
    /**
     * l in bytes
     */
    protected int byteL;
    /**
     * input bits
     */
    protected SquareZ2Vector inputBits;
    /**
     * input
     */
    protected long[] inputPayloads;

    public AbstractPlainPayloadMuxParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PlainPayloadMuxConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(SquareZ2Vector xi, long[] yi, int validBitLen) {
        assert validBitLen >= Long.SIZE;
        zl = ZlFactory.createInstance(envType, validBitLen);
        assert zl.getL() >= Long.SIZE;
        if (yi != null) {
            MathPreconditions.checkEqual("xi.num", "yi.num", xi.getNum(), yi.length);
        }
        num = xi.getNum();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        byteL = zl.getByteL();
        inputBits = xi;
        inputPayloads = yi;
    }

    protected void setPtoInput(SquareZ2Vector xi, BitVector[] yi, int validBitLen) {
        zl = ZlFactory.createInstance(envType, validBitLen);
        if (yi != null) {
            MathPreconditions.checkEqual("xi.num", "yi[0].num", xi.getNum(), yi[0].bitNum());
        }
        num = xi.getNum();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        byteL = zl.getByteL();
        inputBits = xi;
    }
}
