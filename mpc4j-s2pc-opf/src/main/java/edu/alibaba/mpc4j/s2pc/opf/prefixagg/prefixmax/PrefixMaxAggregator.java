package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterParty;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.AbstractGroupAggregator;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggNode;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Prefix max aggregator.
 *
 * @author Li Peng
 * @date 2023/11/1
 */
public abstract class PrefixMaxAggregator extends AbstractGroupAggregator {
    /**
     * Zl greater party;
     */
    protected ZlGreaterParty zlGreaterParty;

    protected PrefixMaxAggregator(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    @Override
    protected PrefixAggNode[] vectorOp(PrefixAggNode[] input1, PrefixAggNode[] input2) throws MpcAbortException {
        int num = input1.length;
        byte[][] groupIn1 = Arrays.stream(input1).map(PrefixAggNode::getGroupShare).toArray(byte[][]::new);
        byte[][] groupIn2 = Arrays.stream(input2).map(PrefixAggNode::getGroupShare).toArray(byte[][]::new);
        // transpose
        ZlDatabase zlDatabase1 = ZlDatabase.create(zl.getL(), groupIn1);
        ZlDatabase zlDatabase2 = ZlDatabase.create(zl.getL(), groupIn2);
        // create z2 shares of grouping
        SquareZ2Vector[] groupIn1bc = Arrays.stream(zlDatabase1.bitPartition(EnvType.STANDARD, true))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] groupIn2bc = Arrays.stream(zlDatabase2.bitPartition(EnvType.STANDARD, true))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        // create zl shares of aggregation fields
        SquareZlVector aggIn1ac = SquareZlVector.create(zl, Arrays.stream(input1).map(PrefixAggNode::getAggShare).toArray(BigInteger[]::new), false);
        SquareZlVector sumIn2ac = SquareZlVector.create(zl, Arrays.stream(input2).map(PrefixAggNode::getAggShare).toArray(BigInteger[]::new), false);
        // group_out = group_in1
        byte[][] groupingOut = Arrays.stream(input1).map(PrefixAggNode::getGroupShare).toArray(byte[][]::new);
        // group_indicator = (group_in1 ?= group_in2)
        SquareZ2Vector groupIndicatorOut = (SquareZ2Vector) z2IntegerCircuit.eq(groupIn1bc, groupIn2bc);
        // agg_out = mux((group_in1 ?= group_in2), greater(agg_1, agg_2)) + mux(not(group_in1 ?= group_in2), agg_1)
        SquareZlVector aggOut = zlcParty.add(zlMuxParty.mux(groupIndicatorOut, zlGreaterParty.gt(aggIn1ac, sumIn2ac)),
            zlMuxParty.mux(z2cParty.not(groupIndicatorOut), aggIn1ac));
        return IntStream.range(0, num).mapToObj(i -> new PrefixAggNode(groupingOut[i], aggOut.getZlVector().getElement(i))).toArray(PrefixAggNode[]::new);
    }
}
