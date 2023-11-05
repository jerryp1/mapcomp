package edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * abstract plain bit mux party.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
public abstract class AbstractPlainBitMuxParty extends AbstractTwoPartyPto implements PlainBitMuxParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPlainBitMuxParty.class);
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
    protected BitVector inputBits;
    /**
     * input
     */
    protected SquareZlVector inputPayloads;

    public AbstractPlainBitMuxParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PlainBitMuxConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(BitVector xi, SquareZlVector yi) {
        if (xi != null) {
            MathPreconditions.checkEqual("xi.num", "yi.num", xi.bitNum(), yi.getNum());
        }
        num = yi.getNum();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        byteL = zl.getByteL();
        inputBits = xi;
        inputPayloads = yi;
    }
}
