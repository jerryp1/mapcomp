package edu.alibaba.mpc4j.s2pc.aby.bc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * Abstract Boolean circuit party.
 *
 * @author Weiran Liu
 * @date 2022/02/13
 */
public abstract class AbstractBcParty extends AbstractSecureTwoPartyPto implements BcParty {
    /**
     * protocol configuration
     */
    private final BcConfig config;
    /**
     * maximum number of bits in round.
     */
    protected int maxRoundBitNum;
    /**
     * total number of bits for updates.
     */
    protected long maxUpdateBitNum;
    /**
     * current number of bits.
     */
    protected int bitNum;
    /**
     * the number of input bits
     */
    protected long inputBitNum;
    /**
     * the number of AND gates.
     */
    protected long andGateNum;
    /**
     * the number of XOR gates.
     */
    protected long xorGateNum;
    /**
     * the number of output bits
     */
    protected long outputBitNum;

    public AbstractBcParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, BcConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
        andGateNum = 0;
        xorGateNum = 0;
    }

    @Override
    public BcFactory.BcType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxRoundBitNum, int updateBitNum) {
        assert maxRoundBitNum > 0 && maxRoundBitNum <= config.maxBaseNum()
            : "maxRoundBitNum must be in range (0, " + config.maxBaseNum() + "]";
        this.maxRoundBitNum = maxRoundBitNum;
        assert updateBitNum >= maxRoundBitNum : "updateBitNum must be greater or equal to maxRoundBitNum";
        this.maxUpdateBitNum = updateBitNum;
        initialized = false;
    }

    protected void setShareOwnInput(BitVector bitVector) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert bitVector.bitNum() <= maxRoundBitNum
            : "the number of bits must be less than or equal to " + maxRoundBitNum + ": " + bitVector.bitNum();
        bitNum = bitVector.bitNum();
        inputBitNum += bitNum;
    }

    protected void setShareOtherInput(int bitNum) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert bitNum <= maxRoundBitNum
            : "the number of bits must be less than or equal to " + maxRoundBitNum + ": " + bitNum;
        this.bitNum = bitNum;
        inputBitNum += bitNum;
    }

    protected void setAndInput(SquareSbitVector xi, SquareSbitVector yi) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert xi.bitNum() == yi.bitNum()
            : "two BitVector must have the same number of bits (" + xi.bitNum() + " : " + yi.bitNum() + ")";
        assert xi.bitNum() <= maxRoundBitNum
            : "the number of bits must be less than or equal to " + maxRoundBitNum + ": " + xi.bitNum();
        // the number of AND gates is added during the protocol execution.
        bitNum = xi.bitNum();
    }

    protected void setXorInput(SquareSbitVector xi, SquareSbitVector yi) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert xi.bitNum() == yi.bitNum()
            : "two BitVector must have the same number of bits (" + xi.bitNum() + " : " + yi.bitNum() + ")";
        assert xi.bitNum() <= maxRoundBitNum
            : "the number of bits must be less than or equal to " + maxRoundBitNum + ": " + xi.bitNum();
        // the number of XOR gates is added during the protocol execution.
        bitNum = xi.bitNum();
    }

    protected void setRevealOwnInput(SquareSbitVector xi) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert xi.bitNum() <= maxRoundBitNum
            : "the number of bits must be less than or equal to " + maxRoundBitNum + ": " + xi.bitNum();
        // the number of output bits is added during the protocol execution.
        bitNum = xi.bitNum();
    }

    protected void setRevealOtherInput(SquareSbitVector xi) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert xi.bitNum() <= maxRoundBitNum
            : "the number of bits must be less than or equal to " + maxRoundBitNum + ": " + xi.bitNum();
        // the number of output bits is added during the protocol execution.
        bitNum = xi.bitNum();
    }

    @Override
    public long inputBitNum(boolean reset) {
        long result = inputBitNum;
        inputBitNum = reset ? 0L : inputBitNum;
        return result;
    }

    @Override
    public long andGateNum(boolean reset) {
        long result = andGateNum;
        andGateNum = reset ? 0L : andGateNum;
        return result;
    }

    @Override
    public long xorGateNum(boolean reset) {
        long result = xorGateNum;
        xorGateNum = reset ? 0L : xorGateNum;
        return result;
    }
    @Override
    public long outputBitNum(boolean reset) {
        long result = outputBitNum;
        outputBitNum = reset ? 0L : outputBitNum;
        return result;
    }
}
