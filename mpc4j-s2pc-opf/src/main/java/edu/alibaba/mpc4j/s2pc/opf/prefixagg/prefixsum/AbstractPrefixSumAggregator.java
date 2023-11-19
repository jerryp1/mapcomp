package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum;

import edu.alibaba.mpc4j.common.circuit.prefix.PrefixOp;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.AbstractPrefixGroupAggregator;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggNode;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggParty;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Prefix sum aggregator.
 *
 * @author Li Peng
 * @date 2023/10/30
 */
public abstract class AbstractPrefixSumAggregator extends AbstractPrefixGroupAggregator implements PrefixOp, PrefixAggParty {

    protected AbstractPrefixSumAggregator(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    @Override
    protected PrefixAggNode[] vectorOp(PrefixAggNode[] input1, PrefixAggNode[] input2) throws MpcAbortException {
        int num = input1.length;
        // 两种方式。1.矩阵转置+or 2.b2a再相减，后使用加法器/millionaire取最高位。这里使用第一种方法
        // create zl shares of sum
        SquareZlVector sumIn1Ac = SquareZlVector.create(zl, Arrays.stream(input1).map(PrefixAggNode::getAggShare).toArray(BigInteger[]::new), false);
        SquareZlVector sumIn2Ac = SquareZlVector.create(zl, Arrays.stream(input2).map(PrefixAggNode::getAggShare).toArray(BigInteger[]::new), false);
        // group_indicator = (group_in1 ?= group_in2)
        SquareZ2Vector groupIndicator1bc = SquareZ2Vector.createZeros(input1.length, false);
        IntStream.range(0, input1.length).forEach(i -> groupIndicator1bc.getBitVector().set(i, input1[i].isGroupIndicator()));
        SquareZ2Vector groupIndicator2bc = SquareZ2Vector.createZeros(input2.length, false);
        IntStream.range(0, input2.length).forEach(i -> groupIndicator2bc.getBitVector().set(i, input2[i].isGroupIndicator()));
        // sum_out = ((group_in1 ?= group_in2) ? sum_in2 : 0) + sum_in1
        SquareZlVector sumOut = zlcParty.add(zlMuxParty.mux(groupIndicator1bc, sumIn2Ac), sumIn1Ac);
        // group_indicator_out
        SquareZ2Vector groupIndicatorOut = z2cParty.and(groupIndicator1bc, groupIndicator2bc);
        return IntStream.range(0, num).mapToObj(i ->
            new PrefixAggNode(sumOut.getZlVector().getElement(i), groupIndicatorOut.getBitVector().get(i))).toArray(PrefixAggNode[]::new);
    }

    @Override
    public PrefixAggTypes getAggType() {
        return PrefixAggTypes.SUM;
    }
}
