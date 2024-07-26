package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg;

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
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggUtils;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleParty;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.crypto.matrix.TransposeUtils.transposeMergeToVector;

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
     * B2a party
     */
    protected B2aParty b2aParty;
    /**
     * A2b party
     */
    protected A2bParty a2bParty;
    /**
     * Z2 mux party.
     */
    protected Z2MuxParty z2MuxParty;
    /**
     * Plain bit mux party.
     */
    protected PlainBitMuxParty pbMuxParty;

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
     * whether needs shuffle result before output.
     */
    protected boolean needShuffle;
    /**
     * is receiver.
     */
    protected boolean receiver;
    /**
     * the output will be directly output
     */
    protected boolean plainOutput = false;

    protected AbstractPrefixGroupAggregator(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    @Override
    public PrefixAggOutput agg(Vector<byte[]> groupField, SquareZlVector aggField) throws MpcAbortException {
        return agg(groupField, aggField, null);
    }

    @Override
    public PrefixAggOutput agg(String[] groupField, SquareZlVector aggField) throws MpcAbortException {
        return agg(groupField, aggField, null);
    }

    @Override
    public PrefixAggOutput agg(Vector<byte[]> groupField, SquareZlVector aggField, SquareZ2Vector intersFlag) throws MpcAbortException {
        checkInputs(groupField, aggField);
        // obtain group indicator
        SquareZ2Vector groupIndicator = obtainGroupIndicator1(groupField, intersFlag);
        // agg
        if (intersFlag != null) {
            aggField = zlMuxParty.mux(intersFlag, aggField);
        }
        SquareZlVector sums;
        // optimize for sum
        if (plainOutput && getAggType().equals(PrefixAggTypes.SUM)) {
            sums = optimizeForSum(aggField, groupIndicator);
        } else {
            sums = aggWithIndicatorsArithmetic(groupIndicator, aggField);
        }
        // shuffle
        if (needShuffle) {
            List<Vector<byte[]>> shuffledResult = shuffle(groupField, sums);
            groupField = shuffledResult.get(0);
            sums = SquareZlVector.create(zl, shuffledResult.get(1).stream()
                .map(BigIntegerUtils::byteArrayToNonNegBigInteger).toArray(BigInteger[]::new), false);
        }
        return new PrefixAggOutput(groupField, sums, groupIndicator);
    }

    @Override
    public PrefixAggOutput agg(String[] groupField, SquareZlVector aggField, SquareZ2Vector intersFlag) throws MpcAbortException {
        checkInputs(groupField, aggField);
        // obtain group indicator
        SquareZ2Vector groupIndicator = obtainPlainGroupIndicator1(groupField, intersFlag);
        // agg
        if (intersFlag != null) {
            aggField = zlMuxParty.mux(intersFlag, aggField);
        }
        SquareZlVector sums;
        // optimize for sum
        if (plainOutput && getAggType().equals(PrefixAggTypes.SUM)) {
            sums = optimizeForSum(aggField, groupIndicator);
        } else {
            sums = aggWithIndicatorsArithmetic(groupIndicator, aggField);
        }
        // share
        Vector<byte[]> sharedGroup = receiver ? shareOwnGroup(groupField) : shareOtherGroup();

        return new PrefixAggOutput(sharedGroup, sums, groupIndicator);
    }

    @Override
    public PrefixAggOutput agg(String[] groupField, SquareZ2Vector[] aggField, SquareZ2Vector intersFlag) throws MpcAbortException {
        checkInputs(groupField, aggField);
        // obtain group indicator
        SquareZ2Vector groupIndicator = obtainPlainGroupIndicator1(groupField, intersFlag);
        // agg
        if (intersFlag != null) {
            aggField = z2MuxParty.mux(intersFlag, aggField);
        }
        SquareZ2Vector[] sums;
        // optimize for sum
        if (plainOutput && getAggType().equals(PrefixAggTypes.SUM)) {
            SquareZlVector transRes = b2aParty.b2a(aggField);
            SquareZlVector tmp = optimizeForSum(transRes, groupIndicator);
            sums = a2bParty.a2b(tmp);
        } else {
            sums = aggWithIndicators(groupIndicator, aggField);
        }
        // share
        Vector<byte[]> sharedGroup = receiver ? shareOwnGroup(groupField) : shareOtherGroup();

        return new PrefixAggOutput(sharedGroup, sums, groupIndicator);
    }

    @Override
    public PrefixAggOutput agg(Vector<byte[]> groupField, SquareZ2Vector[] aggField) throws MpcAbortException {
        return agg(groupField, aggField, null);
    }

    @Override
    public PrefixAggOutput agg(String[] groupField, SquareZ2Vector[] aggField) throws MpcAbortException {
        return agg(groupField, aggField, null);
    }

    @Override
    public PrefixAggOutput agg(Vector<byte[]> groupField, SquareZ2Vector[] aggField, SquareZ2Vector intersFlag) throws MpcAbortException {
        checkInputs(groupField, aggField);
        // obtain group indicator
        SquareZ2Vector groupIndicator = obtainGroupIndicator1(groupField, intersFlag);
        // agg
        if (intersFlag != null) {
            aggField = z2MuxParty.mux(intersFlag, aggField);
        }

        SquareZ2Vector[] sums;
        // optimize for sum
        if (plainOutput && getAggType().equals(PrefixAggTypes.SUM)) {
            SquareZlVector transRes = b2aParty.b2a(aggField);
            SquareZlVector tmp = optimizeForSum(transRes, groupIndicator);
            sums = a2bParty.a2b(tmp);
        } else {
            sums = aggWithIndicators(groupIndicator, aggField);
        }
        // shuffle
        if (needShuffle) {
            List<Vector<byte[]>> shuffledResult = shuffle(groupField, sums);
            groupField = shuffledResult.get(0);
            BitVector[] tmps = ZlDatabase.create(sums.length, shuffledResult.get(1).toArray(new byte[0][])).bitPartition(envType, parallel);
            sums = Arrays.stream(tmps).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new);
        }
        return new PrefixAggOutput(groupField, sums, groupIndicator);
    }

    private SquareZlVector optimizeForSum(SquareZlVector aggField, SquareZ2Vector indicator) throws MpcAbortException {
        Zl zl = aggField.getZl();
        // summation
        BigInteger[] aggs = new BigInteger[num];
        aggs[num - 1] = aggField.getZlVector().getElement(num - 1);
        for (int i = num - 2; i >= 0; i--) {
            aggs[i] = zl.add(aggField.getZlVector().getElement(i), (aggs[i + 1]));
        }
        SquareZlVector summationsZl = SquareZlVector.create(aggField.getZl(), aggs, false);
        return zlMuxParty.mux(indicator, summationsZl);
    }

    private Vector<byte[]> shareOwnGroup(String[] groupField) {
        int bitLength = groupField[0].length();
        List<byte[]> shareOwnGroupPayload = new ArrayList<>(GroupAggUtils.binaryStringToBytes(groupField));
        Vector<byte[]> ownShare = IntStream.range(0, groupField.length).mapToObj(i -> {
            byte[] bytes = new byte[CommonUtils.getByteLength(bitLength)];
            secureRandom.nextBytes(bytes);
            return bytes;
        }).collect(Collectors.toCollection(Vector::new));

        List<byte[]> otherShare = IntStream.range(0, groupField.length).mapToObj(i -> BytesUtils.xor(ownShare.get(i), shareOwnGroupPayload.get(i))).collect(Collectors.toList());

        DataPacketHeader groupShareHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), 0, extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(groupShareHeader, otherShare));
        return ownShare;
    }

    protected Vector<byte[]> shareOtherGroup() {
        DataPacketHeader groupShareHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), 0, extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> groupSharesPayload = rpc.receive(groupShareHeader).getPayload();
        return new Vector<>(groupSharesPayload);
    }

    /**
     * Executes the protocol.
     *
     * @param groupIndicator1 (group_i != group_{i-1}), such as 10001000.
     * @param aggField        the aggregation field.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public SquareZlVector aggWithIndicatorsArithmetic(SquareZ2Vector groupIndicator1, SquareZlVector aggField) throws MpcAbortException {
        SquareZ2Vector groupIndicator2 = z2cParty.not(groupIndicator1);
        groupIndicator2.getBitVector().shiftLeftUnChangeNum(1);
        // generate prefix sum nodes.
        genNodes(aggField, groupIndicator2);
        // prefix-computation
        prefixTree.addPrefix(num);
        // obtain agg fields
        return zlMuxParty.mux(groupIndicator1, SquareZlVector.create(zl, Arrays.stream(nodes)
            .map(PrefixAggNode::getAggShare).toArray(BigInteger[]::new), false));
    }

    protected abstract SquareZ2Vector[] aggWithIndicators(SquareZ2Vector groupIndicator1, SquareZ2Vector[] aggField) throws MpcAbortException;


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
     * @param sumField  sum field.
     * @param indicator group indicator in secret shared form.
     */
    protected void genNodes(SquareZlVector sumField, SquareZ2Vector indicator) {
        nodes = IntStream.range(0, num).mapToObj(i -> new PrefixAggNode(sumField.getZlVector().getElement(i),
            indicator.getBitVector().get(i))).toArray(PrefixAggNode[]::new);
    }

    /**
     * Generate prefix-sum nodes.
     *
     * @param sumField  sum field.
     * @param indicator group indicator in secret shared form.
     */
    protected void genNodesBool(SquareZlVector sumField, SquareZ2Vector indicator, int l) {
        nodes = IntStream.range(0, num).mapToObj(i -> new PrefixAggNode(sumField.getZlVector().getElement(i),
            indicator.getBitVector().get(i), l)).toArray(PrefixAggNode[]::new);
    }

    /**
     * Generate prefix-sum nodes with plain indicator.
     *
     * @param sumField  sum field.
     * @param indicator plain group indicator
     */
    private void genNodes(SquareZlVector sumField, BitVector indicator) {
        nodes = IntStream.range(0, num).mapToObj(i -> new PrefixAggNode(sumField.getZlVector().getElement(i),
            indicator != null && indicator.get(i))).toArray(PrefixAggNode[]::new);
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
        // Vector<byte[]> sumBytes = transposeMergeToVector(Arrays.stream(aggs).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));
        int[] randomPerms = genRandomPerm(num);
        return shuffleParty.shuffle(Arrays.asList(groupings, sumBytes), randomPerms);
    }

    /**
     * Shuffle grouping field and aggregation field.
     *
     * @param groupings grouping field.
     * @param aggs      aggregation field.
     * @return shuffled result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    protected List<Vector<byte[]>> shuffle(Vector<byte[]> groupings, SquareZ2Vector[] aggs) throws MpcAbortException {
//        Vector<byte[]> sumBytes = Arrays.stream(aggs.getZlVector().getElements())
//            .map(v -> BigIntegerUtils.nonNegBigIntegerToByteArray(v, zl.getByteL())).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> sumBytes = transposeMergeToVector(Arrays.stream(aggs).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));
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
    public SquareZ2Vector obtainGroupIndicator1(Vector<byte[]> groupField, SquareZ2Vector intersFlag) throws MpcAbortException {
        byte[][] groupFieldBytes = groupField.toArray(new byte[0][]);
        int byteLength = groupFieldBytes[0].length;
        // pad in the first position with the non-equal value to ensure the indicator is ture (the equality test is false).
        byte[] padding0 = new byte[byteLength];
        secureRandom.nextBytes(padding0);
        byte[] padding1 = new byte[byteLength];
        secureRandom.nextBytes(padding1);
        // shift right
        byte[][] groupShiftRight = new byte[groupField.size()][];
        groupShiftRight[0] = padding0;
        System.arraycopy(groupFieldBytes, 0, groupShiftRight, 1, groupField.size() - 1);
        // shift left
        byte[][] groupShiftLeft = new byte[groupField.size()][];
        groupShiftLeft[0] = padding1;
        System.arraycopy(groupFieldBytes, 1, groupShiftLeft, 1, groupField.size() - 1);
        // create z2 shares of grouping
        SquareZ2Vector[] groupShiftRightBc = Arrays.stream(TransposeUtils.transposeSplit(groupShiftRight, byteLength * Byte.SIZE))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] groupShiftLeftBc = Arrays.stream(TransposeUtils.transposeSplit(groupShiftLeft, byteLength * Byte.SIZE))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        // equality test and reverse
        SquareZ2Vector indicator = z2cParty.not(z2IntegerCircuit.eq(groupShiftLeftBc, groupShiftRightBc));
        if (intersFlag != null) {
            // and
            indicator = z2cParty.and(indicator, intersFlag);
        }
        return indicator;
    }


    /**
     * obtain a boolean indicator to indicate whether (group_i == group_{i+1}), such as 11101110.
     *
     * @param groupField grouping field.
     * @return a boolean indicator.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public SquareZ2Vector obtainGroupIndicator2(Vector<byte[]> groupField, SquareZ2Vector intersFlag) throws MpcAbortException {
        byte[][] groupFieldBytes = groupField.toArray(new byte[0][]);
        int byteLength = groupFieldBytes[0].length;
        // pad in the first position with the non-equal value to ensure the indicator is ture (the equality test is false).
        byte[] padding0 = new byte[byteLength];
        secureRandom.nextBytes(padding0);
        byte[] padding1 = new byte[byteLength];
        secureRandom.nextBytes(padding1);
        // shift right
        byte[][] groupShiftRight = new byte[groupField.size()][];
        groupShiftRight[groupField.size() - 1] = padding0;
        System.arraycopy(groupFieldBytes, 0, groupShiftRight, 0, groupField.size() - 1);
        // shift left
        byte[][] groupShiftLeft = new byte[groupField.size()][];
        groupShiftLeft[groupField.size() - 1] = padding1;
        System.arraycopy(groupFieldBytes, 1, groupShiftLeft, 0, groupField.size() - 1);
        // create z2 shares of grouping
        SquareZ2Vector[] groupShiftRightBc = Arrays.stream(TransposeUtils.transposeSplit(groupShiftRight, byteLength * Byte.SIZE))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] groupShiftLeftBc = Arrays.stream(TransposeUtils.transposeSplit(groupShiftLeft, byteLength * Byte.SIZE))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        // equality test and reverse
        SquareZ2Vector indicator = (SquareZ2Vector) z2IntegerCircuit.eq(groupShiftLeftBc, groupShiftRightBc);
        if (intersFlag != null) {
            indicator = z2cParty.or(z2cParty.not(intersFlag), indicator);
        }
        return indicator;
    }

    private void checkInputs(Vector<byte[]> groupField, SquareZlVector sumField) {
        receiver = !isSender();
        num = sumField.getNum();
        // check equal.
        MathPreconditions.checkEqual("size of groupField", "size of sumField", groupField.size(), sumField.getNum());
    }

    private void checkInputs(String[] groupField, SquareZlVector sumField) {
        receiver = !isSender();
        if (groupField == null && receiver || groupField != null && !receiver) {
            throw new IllegalArgumentException("Wrong input field, receiver should input groupField, or sender should not input groupField");
        }
        num = sumField.getNum();
        if (groupField != null) {
            // check equal.
            MathPreconditions.checkEqual("size of groupField", "size of sumField", groupField.length, sumField.getNum());
        }
    }

    private void checkInputs(Vector<byte[]> groupField, SquareZ2Vector[] sumField) {
        receiver = !isSender();
        num = sumField[0].getNum();
        // check equal.
        MathPreconditions.checkEqual("size of groupField", "size of sumField", groupField.size(), sumField[0].getNum());
    }

    private void checkInputs(String[] groupField, SquareZ2Vector[] sumField) {
        receiver = !isSender();
        if (groupField == null && receiver || groupField != null && !receiver) {
            throw new IllegalArgumentException("Wrong input field, receiver should input groupField, or sender should not input groupField");
        }
        num = sumField[0].getNum();
        if (groupField != null) {
            // check equal.
            MathPreconditions.checkEqual("size of groupField", "size of sumField", groupField.length, sumField[0].getNum());
        }
    }

    /**
     * obtain a boolean indicator to indicate whether (group_i != group_{i-1}), such as 10001000.
     *
     * @param x grouping field.
     * @return a boolean indicator.
     */
    protected SquareZ2Vector obtainPlainGroupIndicator1(String[] x, SquareZ2Vector intersFlag) throws MpcAbortException {
        SquareZ2Vector indicator;
        if (receiver) {
            BitVector plainIndicator = BitVectorFactory.createZeros(x.length);
            IntStream.range(0, x.length - 1).forEach(i -> plainIndicator.set(i + 1, !x[i].equals(x[i + 1])));
            plainIndicator.set(0, true);
            indicator = SquareZ2Vector.create(plainIndicator, false);
        } else {
            indicator = SquareZ2Vector.createZeros(num, false);
        }
        if (intersFlag != null) {
            SquareZ2Vector temp = intersFlag.copy();
            temp.getBitVector().set(0, receiver);
            indicator = z2cParty.and(indicator, temp);
        }
        return indicator;
    }

    /**
     * obtain a boolean indicator to indicate whether (group_i == group_{i+1}), such as 11101110.
     *
     * @param x grouping field.
     * @return a boolean indicator.
     */
    protected SquareZ2Vector obtainPlainGroupIndicator2(String[] x, SquareZ2Vector intersFlag) throws MpcAbortException {
        SquareZ2Vector indicator;
        if (receiver) {
            BitVector plainIndicator = BitVectorFactory.createZeros(x.length);
            IntStream.range(0, x.length - 1).forEach(i -> plainIndicator.set(i, !x[i].equals(x[i + 1])));
            plainIndicator.noti();
            plainIndicator.set(x.length - 1, false);
            indicator = SquareZ2Vector.create(plainIndicator, false);
        } else {
            indicator = SquareZ2Vector.createZeros(num, false);
        }
        if (intersFlag != null) {
            indicator = z2cParty.or(z2cParty.not(intersFlag), indicator);
        }
        return indicator;
    }

    @Override
    public PrefixNode[] getPrefixSumNodes() {
        return nodes;
    }

}
