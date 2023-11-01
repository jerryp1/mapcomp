package edu.alibaba.mpc4j.s2pc.aby.operator.agg.prefixmax;

import edu.alibaba.mpc4j.common.circuit.prefix.PrefixNode;
import edu.alibaba.mpc4j.common.circuit.prefix.PrefixOp;
import edu.alibaba.mpc4j.common.circuit.prefix.PrefixTree;
import edu.alibaba.mpc4j.common.circuit.prefix.PrefixTreeFactory;
import edu.alibaba.mpc4j.common.circuit.prefix.PrefixTreeFactory.PrefixTreeTypes;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;
import java.util.stream.IntStream;

/**
 * Prefix sum aggregator.
 * @author Li Peng
 * @date 2023/10/30
 */
public class PrefixMaxAggregator implements PrefixOp {
    /**
     * z2 circuit party.
     */
    Z2cParty z2cParty;
    /**
     * zl circuit party.
     */
    ZlcParty zlcParty;
    /**
     * Zl greater party;
     */
    ZlGreaterParty zlGreaterParty;
    /**
     * Zl mux party;
     */
    ZlMuxParty zlMuxParty;
    /**
     * Z2 integer circuit.
     */
    Z2IntegerCircuit z2IntegerCircuit;
    /**
     * Prefix sum tree used for addition.
     */
    PrefixTree prefixTree;
    /**
     * Prefix sum nodes.
     */
    private PrefixMaxNode[] nodes;
    /**
     * Number of elements.
     */
    private int num;
    /**
     * Zl
     */
    private Zl zl;

    public PrefixMaxAggregator(Z2cParty z2cParty, ZlcParty zlcParty, ZlGreaterParty zlGreaterParty, ZlMuxParty zlMuxParty, PrefixTreeTypes type)    {
        this.z2cParty = z2cParty;
        this.zlcParty = zlcParty;
        this.zlGreaterParty = zlGreaterParty;
        this.zlMuxParty = zlMuxParty;
        z2IntegerCircuit = new Z2IntegerCircuit(z2cParty);
        prefixTree = PrefixTreeFactory.createPrefixSumTree(type, this);
        zl = zlcParty.getZl();
    }

    public PrefixMaxAggregator(Rpc ownRpc, Party otherParty, Zl zl, PrefixTreeTypes type) {
        // create parties
        this.zlcParty = otherParty.getPartyId() == 0 ?
            ZlcFactory.createSender(ownRpc, otherParty, ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST,zl))
        :ZlcFactory.createReceiver(ownRpc, otherParty, ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST,zl));
        this.zlGreaterParty = otherParty.getPartyId() == 0 ?
            ZlGreaterFactory.createSender(ownRpc, otherParty, ZlGreaterFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true, zl))
            :ZlGreaterFactory.createReceiver(ownRpc, otherParty, ZlGreaterFactory.createDefaultConfig(SecurityModel.SEMI_HONEST,true, zl));
        this.zlMuxParty = otherParty.getPartyId() == 0 ?
            ZlMuxFactory.createSender(ownRpc, otherParty, ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true))
            :ZlMuxFactory.createReceiver(ownRpc, otherParty, ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST,true));
        Z2cParty z2cParty =  otherParty.getPartyId() == 0 ?
            Z2cFactory.createSender(ownRpc, otherParty, Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST,true))
            :Z2cFactory.createReceiver(ownRpc, otherParty, Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST,true));
        z2IntegerCircuit = new Z2IntegerCircuit(z2cParty);
        prefixTree = PrefixTreeFactory.createPrefixSumTree(type, this);
        this.zl = zlcParty.getZl();
    }

    public SquareZlVector sum(Vector<byte[]> groupField, SquareZlVector sumField) throws MpcAbortException {
        checkInputs(groupField, sumField);
        // generate prefix sum nodes.
        genNodes(groupField,sumField);
        // prefix-summation
        prefixTree.addPrefix(num);
        // obtain sum fields
        BigInteger[] sums = Arrays.stream(nodes).map(PrefixMaxNode::getSumShare).toArray(BigInteger[]::new);
        return SquareZlVector.create(zl, sums, false);
    }

    private void genNodes(Vector<byte[]> groupField, SquareZlVector sumField) {
        nodes = IntStream.range(0, num).mapToObj(i -> new PrefixMaxNode(groupField.elementAt(i),
            sumField.getZlVector().getElement(i))).toArray(PrefixMaxNode[]::new);
    }

    @Override
    public PrefixNode[] getPrefixSumNodes() {
        return nodes;
    }

    @Override
    public void operateAndUpdate(PrefixNode[] x, PrefixNode[] y, int[] outputIndexes) throws MpcAbortException {
        PrefixMaxNode[] xTuples = Arrays.stream(x).map(v -> (PrefixMaxNode) v).toArray(PrefixMaxNode[]::new);
        PrefixMaxNode[] yTuples = Arrays.stream(y).map(v -> (PrefixMaxNode) v).toArray(PrefixMaxNode[]::new);
        PrefixMaxNode[] result = vectorOp(xTuples, yTuples);
        // update nodes.
        IntStream.range(0, result.length).forEach(i -> nodes[outputIndexes[i]] = result[i]);
    }

    private PrefixMaxNode[] vectorOp(PrefixMaxNode[] input1, PrefixMaxNode[] input2) throws MpcAbortException {
        byte[][] groupIn1 = Arrays.stream(input1).map(PrefixMaxNode::getGroupShare).toArray(byte[][]::new);
        byte[][] groupIn2 = Arrays.stream(input2).map(PrefixMaxNode::getGroupShare).toArray(byte[][]::new);
        // transpose
        ZlDatabase zlDatabase1 = ZlDatabase.create(zl.getL(),groupIn1);
        ZlDatabase zlDatabase2 = ZlDatabase.create(zl.getL(),groupIn2);
        // create z2 shares of grouping
        SquareZ2Vector[] groupIn1bc = Arrays.stream(zlDatabase1.bitPartition(EnvType.STANDARD, true))
            .map(v-> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] groupIn2bc = Arrays.stream(zlDatabase2.bitPartition(EnvType.STANDARD, true))
            .map(v-> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        // create zl shares of aggregation fields
        SquareZlVector aggIn1ac = SquareZlVector.create(zl,Arrays.stream(input1).map(PrefixMaxNode::getSumShare).toArray(BigInteger[]::new),false);
        SquareZlVector sumIn2ac = SquareZlVector.create(zl,Arrays.stream(input2).map(PrefixMaxNode::getSumShare).toArray(BigInteger[]::new),false);
        // group_oup = group_in1
        byte[][] groupingOut = Arrays.stream(input1).map(PrefixMaxNode::getGroupShare).toArray(byte[][]::new);
        // sum_out = mux((group_in1 ?= group_in2), greater(agg_1, agg_2)) + mux(not(group_in1 ?= group_in2), agg_1)
        SquareZlVector aggOut = zlcParty.add(zlMuxParty.mux((SquareZ2Vector)z2IntegerCircuit.eq(groupIn1bc, groupIn2bc), zlGreaterParty.gt(aggIn1ac, sumIn2ac)),
            zlMuxParty.mux(z2cParty.not(z2IntegerCircuit.eq(groupIn1bc, groupIn2bc)), aggIn1ac));
        return IntStream.range(0, num).mapToObj(i -> new PrefixMaxNode(groupingOut[i], aggOut.getZlVector().getElement(i))).toArray(PrefixMaxNode[]::new);
    }

    private void checkInputs(Vector<byte[]> groupField, SquareZlVector sumField) {
        num = groupField.size();
        // check equal.
        MathPreconditions.checkEqual("size of groupField", "size of sumField", groupField.size(), sumField.getNum());
    }

}
