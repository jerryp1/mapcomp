package edu.alibaba.mpc4j.s2pc.opf.prefixagg;

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
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleParty;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Abstract group aggregator
 *
 * @author Li Peng
 * @date 2023/11/1
 */
public abstract class AbstractPrefixGroupAggregator extends AbstractTwoPartyPto implements PrefixAggParty, PrefixOp {
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
     * Shuffle party.
     */
    protected ShuffleParty shuffleParty;
    /**
     * Prefix sum tree used for addition.
     */
    protected PrefixTree prefixTree;
    /**
     * Prefix sum nodes.
     */
    protected PrefixAggNode[] nodes;
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

    protected AbstractPrefixGroupAggregator(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    @Override
    public PrefixAggOutput agg(Vector<byte[]> groupField, SquareZlVector aggField) throws MpcAbortException {
        checkInputs(groupField, aggField);
        // generate prefix sum nodes.
        SquareZ2Vector groupIndicator2 = obtainGroupIndicator2(groupField);
        // generate nodes.
        genNodes(aggField, groupIndicator2);
        // prefix-computation
        prefixTree.addPrefix(num);
        // obtain agg fields
        SquareZlVector sums = SquareZlVector.create(zl, Arrays.stream(nodes)
            .map(PrefixAggNode::getAggShare).toArray(BigInteger[]::new), false);
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

    @Override
    public void operateAndUpdate(PrefixNode[] x, PrefixNode[] y, int[] outputIndexes) throws MpcAbortException {
        PrefixAggNode[] xTuples = Arrays.stream(x).map(v -> (PrefixAggNode) v).toArray(PrefixAggNode[]::new);
        PrefixAggNode[] yTuples = Arrays.stream(y).map(v -> (PrefixAggNode) v).toArray(PrefixAggNode[]::new);
        PrefixAggNode[] result = vectorOp(xTuples, yTuples);
        // update nodes.
        IntStream.range(0, result.length).forEach(i -> nodes[outputIndexes[i]] = result[i]);
    }

    /**
     * Prefix computation in vector form.
     *
     * @param input1 input1.
     * @param input2 input2.
     * @return prefix computation result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    protected abstract PrefixAggNode[] vectorOp(PrefixAggNode[] input1, PrefixAggNode[] input2) throws MpcAbortException;

    /**
     * Generate prefix-sum nodes.
     *
     * @param sumField   sum field.
     */
    private void genNodes(SquareZlVector sumField, SquareZ2Vector indicator) {
        nodes = IntStream.range(0, num).mapToObj(i -> new PrefixAggNode(sumField.getZlVector().getElement(i),
            indicator.getBitVector().get(i))).toArray(PrefixAggNode[]::new);
    }

    /**
     * Shuffle grouping field and aggregation field.
     *
     * @param groupings grouping field.
     * @param aggs      aggregation field.
     * @return shuffled result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    protected List<Vector<byte[]>> shuffle(Vector<byte[]> groupings, SquareZlVector aggs) throws MpcAbortException {
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
    protected int[] genRandomPerm(int num) {
        // generate random permutation
        List<Integer> randomPermList = IntStream.range(0, num)
            .boxed()
            .collect(Collectors.toList());
        Collections.shuffle(randomPermList, secureRandom);
        return randomPermList.stream().mapToInt(permutation -> permutation).toArray();
    }

    /**
     * obtain a boolean indicator to indicate whether (group_i != group_{i-1}), such as 10001000.
     *
     * @param groupField grouping field.
     * @return a boolean indicator.
     * @throws MpcAbortException the protocol failure aborts.
     */
    protected SquareZ2Vector obtainGroupIndicator(Vector<byte[]> groupField) throws MpcAbortException {
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
     * obtain a boolean indicator to indicate whether (group_i == group_{i+1}), such as 11101110.
     *
     * @param groupField grouping field.
     * @return a boolean indicator.
     * @throws MpcAbortException the protocol failure aborts.
     */
    protected SquareZ2Vector obtainGroupIndicator2(Vector<byte[]> groupField) throws MpcAbortException {
        byte[][] groupFieldBytes = groupField.toArray(new byte[0][]);
        // pad in the first position with the non-equal value to ensure the indicator is ture (the equality test is false).
        byte[] padding0 = new byte[groupFieldBytes[0].length];
        secureRandom.nextBytes(padding0);
        byte[] padding1 = new byte[groupFieldBytes[0].length];
        secureRandom.nextBytes(padding1);
        // shift right
        byte[][] groupShiftRight = new byte[groupField.size()][];
        groupShiftRight[groupField.size() - 1] = padding0;
        System.arraycopy(groupFieldBytes, 0, groupShiftRight, 0, groupField.size() - 1);
        // shift left
        byte[][] groupShiftLeft = new byte[groupField.size()][];
        groupShiftLeft[groupField.size() - 1] = padding1;
        System.arraycopy(groupFieldBytes, 1, groupShiftLeft, 0, groupField.size() - 1);
        // transpose to feed into equality test
        ZlDatabase zlDatabase1 = ZlDatabase.create(zl.getL(), groupShiftRight);
        ZlDatabase zlDatabase2 = ZlDatabase.create(zl.getL(), groupShiftLeft);
        // create z2 shares of grouping
        SquareZ2Vector[] groupShiftRightBc = Arrays.stream(zlDatabase1.bitPartition(EnvType.STANDARD, true))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] groupShiftLeftBc = Arrays.stream(zlDatabase2.bitPartition(EnvType.STANDARD, true))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        // equality test and reverse
        return (SquareZ2Vector) z2IntegerCircuit.eq(groupShiftLeftBc, groupShiftRightBc);
    }

    private void checkInputs(Vector<byte[]> groupField, SquareZlVector sumField) {
        num = groupField.size();
        // check equal.
        MathPreconditions.checkEqual("size of groupField", "size of sumField", groupField.size(), sumField.getNum());
    }

    @Override
    public PrefixNode[] getPrefixSumNodes() {
        return nodes;
    }
}
