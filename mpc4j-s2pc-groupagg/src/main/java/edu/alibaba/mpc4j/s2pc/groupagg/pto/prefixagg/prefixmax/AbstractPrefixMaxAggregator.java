package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixmax;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.AbstractPrefixGroupAggregator;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Prefix max aggregator.
 *
 */
public abstract class AbstractPrefixMaxAggregator extends AbstractPrefixGroupAggregator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPrefixMaxAggregator.class);
    /**
     * Zl greater party;
     */
    protected ZlGreaterParty zlGreaterParty;

    protected AbstractPrefixMaxAggregator(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    @Override
    protected PrefixAggNode[] vectorOp(PrefixAggNode[] input1, PrefixAggNode[] input2) throws MpcAbortException {
        int num = input1.length;
        // create zl shares of aggregation fields
        SquareZlVector aggIn1ac = SquareZlVector.create(zl, Arrays.stream(input1).map(PrefixAggNode::getAggShare).toArray(BigInteger[]::new), false);
        SquareZlVector sumIn2ac = SquareZlVector.create(zl, Arrays.stream(input2).map(PrefixAggNode::getAggShare).toArray(BigInteger[]::new), false);
        // group_indicator = (group_in1 ?= group_in2)
        SquareZ2Vector groupIndicator1bc = SquareZ2Vector.createZeros(input1.length, false);
        IntStream.range(0, input1.length).forEach(i -> groupIndicator1bc.getBitVector().set(i, input1[i].isGroupIndicator()));
        SquareZ2Vector groupIndicator2bc = SquareZ2Vector.createZeros(input2.length, false);
        IntStream.range(0, input2.length).forEach(i -> groupIndicator2bc.getBitVector().set(i, input2[i].isGroupIndicator()));
        // agg_out = mux((group_in1 == group_in2), greater(agg_1, agg_2)) + mux(not(group_in1 ?= group_in2), agg_1)
        SquareZlVector aggOut = zlcParty.add(aggIn1ac, zlMuxParty.mux(groupIndicator1bc, zlcParty.sub(zlGreaterParty.gt(aggIn1ac, sumIn2ac), aggIn1ac)));
        // group_indicator_out
        SquareZ2Vector groupIndicatorOut = z2cParty.and(groupIndicator1bc, groupIndicator2bc);
        return IntStream.range(0, num).mapToObj(i -> new PrefixAggNode(aggOut.getZlVector().getElement(i),
            groupIndicatorOut.getBitVector().get(i))).toArray(PrefixAggNode[]::new);
    }

    @Override
    public PrefixAggTypes getAggType() {
        return PrefixAggTypes.MAX;
    }

    @Override
    protected SquareZ2Vector[] aggWithIndicators(SquareZ2Vector groupIndicator1, SquareZ2Vector[] aggField) throws MpcAbortException {
        SquareZ2Vector groupIndicator2 = z2cParty.not(groupIndicator1);
        groupIndicator2.getBitVector().shiftLeftUnChangeNum(1);

        SquareZlVector data = b2aParty.b2a(aggField);
        // generate prefix sum nodes.
        genNodes(data, groupIndicator2);
        // prefix-computation
        prefixTree.addPrefix(num);
        // obtain agg fields
        SquareZlVector res1 = zlMuxParty.mux(groupIndicator1, SquareZlVector.create(zl, Arrays.stream(nodes)
            .map(PrefixAggNode::getAggShare).toArray(BigInteger[]::new), false));
        return a2bParty.a2b(res1);
    }
}
