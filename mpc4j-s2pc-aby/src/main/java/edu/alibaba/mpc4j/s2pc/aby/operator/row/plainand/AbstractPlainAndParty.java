package edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * Abstract plain and party.
 *
 * @author Li Peng
 * @date 2023/11/8
 */
public abstract class AbstractPlainAndParty extends AbstractTwoPartyPto implements PlainAndParty {
    /**
     * max num
     */
    protected int maxNum;
    /**
     * num
     */
    protected int num;
    /**
     * l in bytes
     */
    protected int byteL;
    /**
     * input
     */
    protected BitVector input;

    public AbstractPlainAndParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PlainAndConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(BitVector x) {
        num = x.bitNum();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        input = x;
    }
}
