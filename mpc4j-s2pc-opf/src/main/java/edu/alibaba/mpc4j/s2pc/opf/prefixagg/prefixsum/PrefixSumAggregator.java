package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum;

import edu.alibaba.mpc4j.common.circuit.prefix.PrefixOp;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.AbstractGroupAggregator;
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
public abstract class PrefixSumAggregator extends AbstractGroupAggregator implements PrefixOp, PrefixAggParty {

    protected PrefixSumAggregator(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    @Override
    protected PrefixAggNode[] vectorOp(PrefixAggNode[] input1, PrefixAggNode[] input2) throws MpcAbortException {
        int num = input1.length;
        // 两种方式。1.矩阵转置+or 2.b2a再相减，后使用加法器/millioare取最高位。这里使用第一种方法
        byte[][] groupIn1 = Arrays.stream(input1).map(PrefixAggNode::getGroupShare).toArray(byte[][]::new);
        byte[][] groupIn2 = Arrays.stream(input2).map(PrefixAggNode::getGroupShare).toArray(byte[][]::new);
        // transpose to feed into circuit
        ZlDatabase zlDatabase1 = ZlDatabase.create(zl.getL(), groupIn1);
        ZlDatabase zlDatabase2 = ZlDatabase.create(zl.getL(), groupIn2);
        // create z2 shares of grouping
        SquareZ2Vector[] groupIn1Bc = Arrays.stream(zlDatabase1.bitPartition(EnvType.STANDARD, true))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] groupIn2Bc = Arrays.stream(zlDatabase2.bitPartition(EnvType.STANDARD, true))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        // create zl shares of sum
        SquareZlVector sumIn1Ac = SquareZlVector.create(zl, Arrays.stream(input1).map(PrefixAggNode::getAggShare).toArray(BigInteger[]::new), false);
        SquareZlVector sumIn2Ac = SquareZlVector.create(zl, Arrays.stream(input2).map(PrefixAggNode::getAggShare).toArray(BigInteger[]::new), false);
        // group_out = group_in1
        byte[][] groupingOut = Arrays.stream(input1).map(PrefixAggNode::getGroupShare).toArray(byte[][]::new);
        // group_indicator = (group_in1 ?= group_in2)
        SquareZ2Vector groupIndicator = (SquareZ2Vector) z2IntegerCircuit.eq(groupIn1Bc, groupIn2Bc);
        // sum_out = ((group_in1 ?= group_in2) ? sum_in2 : 0) + sum_in1
        SquareZlVector sumOut = zlcParty.add(zlMuxParty.mux(groupIndicator, sumIn2Ac), sumIn1Ac);

        return IntStream.range(0, num).mapToObj(i ->
            new PrefixAggNode(groupingOut[i], sumOut.getZlVector().getElement(i))).toArray(PrefixAggNode[]::new);
    }

}
