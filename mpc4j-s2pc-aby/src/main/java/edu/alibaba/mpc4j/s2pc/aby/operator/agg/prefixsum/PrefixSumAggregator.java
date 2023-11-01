package edu.alibaba.mpc4j.s2pc.aby.operator.agg.prefixsum;

import edu.alibaba.mpc4j.common.circuit.prefix.PrefixNode;
import edu.alibaba.mpc4j.common.circuit.prefix.PrefixOp;
import edu.alibaba.mpc4j.common.circuit.prefix.PrefixTree;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Vector;
import java.util.stream.IntStream;

/**
 * Prefix sum aggregator.
 *
 * @author Li Peng
 * @date 2023/10/30
 */
public abstract class PrefixSumAggregator extends AbstractTwoPartyPto implements PrefixOp, PrefixSumParty {
    /**
     * z2 circuit party.
     */
    protected Z2cParty z2cParty;

    /**
     * zl circuit party.
     */
    protected ZlcParty zlcParty;
    /**
     * Zl mux party;
     */
    protected ZlMuxParty zlMuxParty;
    /**
     * Z2 integer circuit.
     */
    protected Z2IntegerCircuit z2IntegerCircuit;
    /**
     * Prefix sum tree used for addition.
     */
    protected PrefixTree prefixTree;
    /**
     * Prefix sum nodes.
     */
    protected PrefixSumNode[] nodes;
    /**
     * Number of elements.
     */
    protected int num;
    /**
     * Zl
     */
    protected Zl zl;

    protected PrefixSumAggregator(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    @Override
    public SquareZlVector sum(Vector<byte[]> groupField, SquareZlVector sumField) throws MpcAbortException {
        checkInputs(groupField, sumField);
        // generate prefix sum nodes.
        genNodes(groupField, sumField);
        // prefix-summation
        prefixTree.addPrefix(num);
        // obtain sum fields
        SquareZlVector sums = SquareZlVector.create(zl, Arrays.stream(nodes).map(PrefixSumNode::getSumShare).toArray(BigInteger[]::new), false);
        return sums;
    }

    private void genNodes(Vector<byte[]> groupField, SquareZlVector sumField) {
        nodes = IntStream.range(0, num).mapToObj(i -> new PrefixSumNode(groupField.elementAt(i),
            sumField.getZlVector().getElement(i), false)).toArray(PrefixSumNode[]::new);
    }

    @Override
    public PrefixNode[] getPrefixSumNodes() {
        return nodes;
    }

    @Override
    public void operateAndUpdate(PrefixNode[] x, PrefixNode[] y, int[] outputIndexes) throws MpcAbortException {
        PrefixSumNode[] xTuples = Arrays.stream(x).map(v -> (PrefixSumNode) v).toArray(PrefixSumNode[]::new);
        PrefixSumNode[] yTuples = Arrays.stream(y).map(v -> (PrefixSumNode) v).toArray(PrefixSumNode[]::new);
        PrefixSumNode[] result = vectorOp(xTuples, yTuples);
        // update nodes.
        IntStream.range(0, result.length).forEach(i -> nodes[outputIndexes[i]] = result[i]);
    }

    private PrefixSumNode[] vectorOp(PrefixSumNode[] input1, PrefixSumNode[] input2) throws MpcAbortException {
        int num = input1.length;
        // 两种方式。1.矩阵转置+or 2.b2a再相减，后使用加法器/millioare取最高位。这里使用第一种方法
        byte[][] groupIn1 = Arrays.stream(input1).map(PrefixSumNode::getGroupShare).toArray(byte[][]::new);
        byte[][] groupIn2 = Arrays.stream(input2).map(PrefixSumNode::getGroupShare).toArray(byte[][]::new);
        // transpose
        ZlDatabase zlDatabase1 = ZlDatabase.create(zl.getL(), groupIn1);
        ZlDatabase zlDatabase2 = ZlDatabase.create(zl.getL(), groupIn2);
        // create z2 shares of grouping
        SquareZ2Vector[] groupIn1bc = Arrays.stream(zlDatabase1.bitPartition(EnvType.STANDARD, true))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] groupIn2bc = Arrays.stream(zlDatabase2.bitPartition(EnvType.STANDARD, true))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        // create zl shares of sum
        SquareZlVector sumIn1ac = SquareZlVector.create(zl, Arrays.stream(input1).map(PrefixSumNode::getSumShare).toArray(BigInteger[]::new), false);
        SquareZlVector sumIn2ac = SquareZlVector.create(zl, Arrays.stream(input2).map(PrefixSumNode::getSumShare).toArray(BigInteger[]::new), false);
        // group_out = group_in1
        byte[][] groupingOut = Arrays.stream(input1).map(PrefixSumNode::getGroupShare).toArray(byte[][]::new);
        // group_indicator = (group_in1 ?= group_in2)
        SquareZ2Vector groupIndicatorOut = (SquareZ2Vector) z2IntegerCircuit.eq(groupIn1bc, groupIn2bc);
        // sum_out = ((group_in1 ?= group_in2) ? sum_in2 : 0) + sum_in1
        SquareZlVector sumOut = zlcParty.add(zlMuxParty.mux(groupIndicatorOut, sumIn2ac), sumIn1ac);

        return IntStream.range(0, num).mapToObj(i ->
            new PrefixSumNode(groupingOut[i], sumOut.getZlVector().getElement(i), groupIndicatorOut.getBitVector().get(i))).toArray(PrefixSumNode[]::new);
    }

    private void checkInputs(Vector<byte[]> groupField, SquareZlVector sumField) {
        num = groupField.size();
        // check equal.
        MathPreconditions.checkEqual("size of groupField", "size of sumField", groupField.size(), sumField.getNum());
    }
}
