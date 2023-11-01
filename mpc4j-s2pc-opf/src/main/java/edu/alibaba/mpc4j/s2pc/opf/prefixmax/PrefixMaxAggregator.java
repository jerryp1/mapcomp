package edu.alibaba.mpc4j.s2pc.opf.prefixmax;

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
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.opf.prefixsum.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.opf.prefixsum.PrefixAggParty;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleParty;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Prefix sum aggregator.
 *
 * @author Li Peng
 * @date 2023/11/1
 */
public abstract class PrefixMaxAggregator extends AbstractTwoPartyPto implements PrefixOp, PrefixAggParty {
    /**
     * z2 circuit party.
     */
    protected Z2cParty z2cParty;
    /**
     * zl circuit party.
     */
    protected ZlcParty zlcParty;
    /**
     * Zl greater party;
     */
    protected ZlGreaterParty zlGreaterParty;
    /**
     * Zl mux party;
     */
    protected ZlMuxParty zlMuxParty;
    /**
     * Shuffle party.
     */
    protected ShuffleParty shuffleParty;
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
    protected PrefixMaxNode[] nodes;
    /**
     * Number of elements.
     */
    protected int num;
    /**
     * Zl
     */
    protected Zl zl;
    /**
     * whether need shuffle result before output.
     */
    protected boolean needShuffle;

    protected PrefixMaxAggregator(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    @Override
    public PrefixAggOutput agg(Vector<byte[]> groupField, SquareZlVector sumField) throws MpcAbortException {
        checkInputs(groupField, sumField);
        // generate prefix sum nodes.
        genNodes(groupField, sumField);
        // prefix-max
        prefixTree.addPrefix(num);
        // obtain sum fields
        SquareZlVector sums = SquareZlVector.create(zl, Arrays.stream(nodes)
            .map(PrefixMaxNode::getMaxShare).toArray(BigInteger[]::new), false);
        // obtain group indicator
        SquareZ2Vector groupIndicator = obtainGroupIndicator(groupField);
        // mux
        sums = zlMuxParty.mux(groupIndicator, sums);
        // shuffle
        if (needShuffle) {
            List<Vector<byte[]>> shuffledResult = shuffle(groupField, sums);
            groupField = shuffledResult.get(0);
            sums = SquareZlVector.create(zl, shuffledResult.get(1).stream()
                .map(BigIntegerUtils::byteArrayToNonNegBigInteger).toArray(BigInteger[]::new), false);
        }
        return new PrefixAggOutput(groupField, sums);
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
        int num = input1.length;
        byte[][] groupIn1 = Arrays.stream(input1).map(PrefixMaxNode::getGroupShare).toArray(byte[][]::new);
        byte[][] groupIn2 = Arrays.stream(input2).map(PrefixMaxNode::getGroupShare).toArray(byte[][]::new);
        // transpose
        ZlDatabase zlDatabase1 = ZlDatabase.create(zl.getL(), groupIn1);
        ZlDatabase zlDatabase2 = ZlDatabase.create(zl.getL(), groupIn2);
        // create z2 shares of grouping
        SquareZ2Vector[] groupIn1bc = Arrays.stream(zlDatabase1.bitPartition(EnvType.STANDARD, true))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] groupIn2bc = Arrays.stream(zlDatabase2.bitPartition(EnvType.STANDARD, true))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        // create zl shares of aggregation fields
        SquareZlVector aggIn1ac = SquareZlVector.create(zl, Arrays.stream(input1).map(PrefixMaxNode::getMaxShare).toArray(BigInteger[]::new), false);
        SquareZlVector sumIn2ac = SquareZlVector.create(zl, Arrays.stream(input2).map(PrefixMaxNode::getMaxShare).toArray(BigInteger[]::new), false);
        // group_out = group_in1
        byte[][] groupingOut = Arrays.stream(input1).map(PrefixMaxNode::getGroupShare).toArray(byte[][]::new);
        // group_indicator = (group_in1 ?= group_in2)
        SquareZ2Vector groupIndicatorOut = (SquareZ2Vector) z2IntegerCircuit.eq(groupIn1bc, groupIn2bc);
        // agg_out = mux((group_in1 ?= group_in2), greater(agg_1, agg_2)) + mux(not(group_in1 ?= group_in2), agg_1)
        SquareZlVector aggOut = zlcParty.add(zlMuxParty.mux(groupIndicatorOut, zlGreaterParty.gt(aggIn1ac, sumIn2ac)),
            zlMuxParty.mux(z2cParty.not(groupIndicatorOut), aggIn1ac));
        return IntStream.range(0, num).mapToObj(i -> new PrefixMaxNode(groupingOut[i], aggOut.getZlVector().getElement(i))).toArray(PrefixMaxNode[]::new);
    }

    private void checkInputs(Vector<byte[]> groupField, SquareZlVector sumField) {
        num = groupField.size();
        // check equal.
        MathPreconditions.checkEqual("size of groupField", "size of sumField", groupField.size(), sumField.getNum());
    }

    /**
     * obtain a boolean indicator to indicate whether (!group_i == group_{i-1}).
     *
     * @param groupField grouping field.
     * @return a boolean indicator.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private SquareZ2Vector obtainGroupIndicator(Vector<byte[]> groupField) throws MpcAbortException {
        byte[][] groupFieldBytes = groupField.toArray(new byte[0][]);
        // pad in the first position with the non-equal value to ensure the indicator is ture (the equality test is false).
        byte[] padding0 = new byte[groupFieldBytes[0].length];
        secureRandom.nextBytes(padding0);
        byte[] padding1 = new byte[groupFieldBytes[0].length];
        secureRandom.nextBytes(padding1);
        // shift right
        byte[][] groupShiftRight = new byte[groupField.size()][];
        groupShiftRight[0] = padding0;
        System.arraycopy(groupFieldBytes, 0, groupShiftRight, 1, groupField.size() - 1);
        // shift left
        byte[][] groupShiftLeft = new byte[groupField.size()][];
        groupShiftLeft[0] = padding1;
        System.arraycopy(groupFieldBytes, 1, groupShiftLeft, 1, groupField.size() - 1);
        // transpose to feed into equality test
        ZlDatabase zlDatabase1 = ZlDatabase.create(zl.getL(), groupShiftRight);
        ZlDatabase zlDatabase2 = ZlDatabase.create(zl.getL(), groupShiftLeft);
        // create z2 shares of grouping
        SquareZ2Vector[] groupShiftRightBc = Arrays.stream(zlDatabase1.bitPartition(EnvType.STANDARD, true))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] groupShiftLeftBc = Arrays.stream(zlDatabase2.bitPartition(EnvType.STANDARD, true))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        // equality test and reverse
        return z2cParty.not(z2IntegerCircuit.eq(groupShiftLeftBc, groupShiftRightBc));
    }

    /**
     * Shuffle grouping field and aggregation field.
     *
     * @param groupings grouping field.
     * @param aggs      aggregation field.
     * @return shuffled result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<Vector<byte[]>> shuffle(Vector<byte[]> groupings, SquareZlVector aggs) throws MpcAbortException {
        Vector<byte[]> sumBytes = Arrays.stream(aggs.getZlVector().getElements())
            .map(v -> BigIntegerUtils.nonNegBigIntegerToByteArray(v, zl.getByteL())).collect(Collectors.toCollection(Vector::new));
        int[] randomPerms = genRandomPerm(num);
        return shuffleParty.shuffle(Arrays.asList(groupings, sumBytes), randomPerms);
    }

    /**
     * Generates random permutation.
     *
     * @param num the number of inputs.
     * @return a random permutation of num.
     */
    private int[] genRandomPerm(int num) {
        // generate random permutation
        List<Integer> randomPermList = IntStream.range(0, num)
            .boxed()
            .collect(Collectors.toList());
        Collections.shuffle(randomPermList, secureRandom);
        return randomPermList.stream().mapToInt(permutation -> permutation).toArray();
    }
}
