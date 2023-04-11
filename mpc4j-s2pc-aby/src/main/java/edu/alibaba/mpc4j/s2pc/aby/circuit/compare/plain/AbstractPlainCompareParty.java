package edu.alibaba.mpc4j.s2pc.aby.circuit.compare.plain;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

/**
 * 汉明距离协议参与方。
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/11
 */
public abstract class AbstractPlainCompareParty extends AbstractTwoPartyPto implements PlainCompareParty {
    /**
     * 配置项
     */
    private final PlainCompareConfig config;
    /**
     * 最大比特数量
     */
    protected int maxBitNum;
    /**
     * 当前比特数量
     */
    protected int bitNum;

    public AbstractPlainCompareParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PlainCompareConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
        maxBitNum = 0;
        bitNum = 0;
    }

    protected void setInitInput(int maxBitNum) {
        MathPreconditions.checkPositiveInRangeClosed("maxBitNum", maxBitNum, config.maxAllowBitNum());
        this.maxBitNum = maxBitNum;
        initState();
    }

    protected void setPtoInput(SquareShareZ2Vector xi) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("xi.bitNum", xi.getNum(), maxBitNum);
        bitNum = xi.getNum();
        extraInfo++;
    }
}
