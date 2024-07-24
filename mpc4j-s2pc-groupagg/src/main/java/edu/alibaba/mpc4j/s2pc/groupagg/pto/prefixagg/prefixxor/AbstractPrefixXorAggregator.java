package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixxor;

import edu.alibaba.mpc4j.common.circuit.prefix.PrefixOp;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.AbstractPrefixGroupAggregator;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggNode;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggParty;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Prefix xor aggregator.
 *
 * @author Li Peng
 * @date 2024/7/19
 */
public abstract class AbstractPrefixXorAggregator extends AbstractPrefixGroupAggregator implements PrefixOp, PrefixAggParty {

    protected AbstractPrefixXorAggregator(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    @Override
    protected PrefixAggNode[] vectorOp(PrefixAggNode[] input1, PrefixAggNode[] input2) throws MpcAbortException {
        // receiver hold plain f.
        int num = input1.length;
        int l = input1[0].getL();
        int byteLen = CommonUtils.getByteLength(l);

        byte[][] input1ByteArray = Arrays.stream(input1).map(PrefixAggNode::getAggShare)
            .map(v -> BigIntegerUtils.nonNegBigIntegerToByteArray(v, byteLen)).toArray(byte[][]::new);
        SquareZ2Vector[] input1Z2Array = Arrays.stream(TransposeUtils.transposeSplit(input1ByteArray, l))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        byte[][] input2ByteArray = Arrays.stream(input2).map(PrefixAggNode::getAggShare)
            .map(v -> BigIntegerUtils.nonNegBigIntegerToByteArray(v, byteLen)).toArray(byte[][]::new);
        SquareZ2Vector[] input2Z2Array = Arrays.stream(TransposeUtils.transposeSplit(input2ByteArray, l))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);

        // group_indicator = (group1 ?= group2)
        SquareZ2Vector groupIndicator1bc = SquareZ2Vector.createZeros(input1.length, false);
        IntStream.range(0, input1.length).forEach(i -> groupIndicator1bc.getBitVector().set(i, input1[i].isGroupIndicator()));
        SquareZ2Vector groupIndicator2bc = SquareZ2Vector.createZeros(input2.length, false);
        IntStream.range(0, input2.length).forEach(i -> groupIndicator2bc.getBitVector().set(i, input2[i].isGroupIndicator()));

        // get indicator
        BitVector indicator1 = receiver ? groupIndicator1bc.getBitVector() : null;
        BitVector indicator2 = receiver ? groupIndicator2bc.getBitVector() : null;

        // xor_out = xor_in1 ⊕ (group_indicator1 ? (xor_in1 ⊕ xor_in2) : 0), which is a prefix-copy operation.
        SquareZ2Vector[] xorOut = z2cParty.xor(input1Z2Array, pbMuxParty.mux(indicator1, z2cParty.xor(input2Z2Array, input1Z2Array)));

        // group_indicator_out
        BitVector groupIndicatorOut = receiver ? indicator1.and(indicator2) : BitVectorFactory.createZeros(num);

        // transform xor_out to bigInteger
        BitVector[] outBitArray = Arrays.stream(xorOut).map(v -> v.getBitVector()).toArray(BitVector[]::new);
        byte[][] outByteArray = TransposeUtils.transposeMerge(outBitArray);
        BigInteger[] aggBigInt = Arrays.stream(outByteArray).map(v -> BigIntegerUtils.byteArrayToNonNegBigInteger(v)).toArray(BigInteger[]::new);

        return IntStream.range(0, num).mapToObj(i ->
            new PrefixAggNode(aggBigInt[i], groupIndicatorOut.get(i), l)).toArray(PrefixAggNode[]::new);
    }

    /**
     * backup
     *
     * @param input1
     * @param input2
     * @return
     * @throws MpcAbortException
     */
    protected PrefixAggNode[] vectorOp2(PrefixAggNode[] input1, PrefixAggNode[] input2) throws MpcAbortException {
        int num = input1.length;
        int l = input1[0].getL();
        int byteLen = CommonUtils.getByteLength(l);

        byte[][] input1ByteArray = Arrays.stream(input1).map(PrefixAggNode::getAggShare)
            .map(v -> BigIntegerUtils.nonNegBigIntegerToByteArray(v, byteLen)).toArray(byte[][]::new);
        SquareZ2Vector[] input1Z2Array = Arrays.stream(TransposeUtils.transposeSplit(input1ByteArray, l))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        byte[][] input2ByteArray = Arrays.stream(input2).map(PrefixAggNode::getAggShare)
            .map(v -> BigIntegerUtils.nonNegBigIntegerToByteArray(v, byteLen)).toArray(byte[][]::new);
        SquareZ2Vector[] input2Z2Array = Arrays.stream(TransposeUtils.transposeSplit(input2ByteArray, l))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);

        // group_indicator = (group1 ?= group2)
        SquareZ2Vector groupIndicator1bc = SquareZ2Vector.createZeros(input1.length, false);
        IntStream.range(0, input1.length).forEach(i -> groupIndicator1bc.getBitVector().set(i, input1[i].isGroupIndicator()));
        SquareZ2Vector groupIndicator2bc = SquareZ2Vector.createZeros(input2.length, false);
        IntStream.range(0, input2.length).forEach(i -> groupIndicator2bc.getBitVector().set(i, input2[i].isGroupIndicator()));

        // xor_out = xor_in1 ⊕ (group_indicator1 ? xor_in2 : 0)
        SquareZ2Vector[] xorOut = z2cParty.xor(input1Z2Array, z2MuxParty.mux(groupIndicator1bc, input2Z2Array));
        // group_indicator_out
        SquareZ2Vector groupIndicatorOut = z2cParty.and(groupIndicator1bc, groupIndicator2bc);

        // transform xor_out to bigInteger
        BitVector[] outBitArray = Arrays.stream(xorOut).map(v -> v.getBitVector()).toArray(BitVector[]::new);
        byte[][] outByteArray = TransposeUtils.transposeMerge(outBitArray);
        BigInteger[] aggBigInt = Arrays.stream(outByteArray).map(v -> BigIntegerUtils.byteArrayToNonNegBigInteger(v)).toArray(BigInteger[]::new);

        return IntStream.range(0, num).mapToObj(i ->
            new PrefixAggNode(aggBigInt[i], groupIndicatorOut.getBitVector().get(i))).toArray(PrefixAggNode[]::new);
    }

    @Override
    protected SquareZ2Vector[] aggWithIndicators(SquareZ2Vector groupIndicator1, SquareZ2Vector[] aggField) throws MpcAbortException {
        int l = aggField.length;
        int byteLen = CommonUtils.getByteLength(l);
        Zl zl = ZlFactory.createInstance(EnvType.INLAND_JDK, l);
        // compute (plain) indicator2 from (plain) indicator 1
        SquareZ2Vector groupIndicator2;
        if (receiver) {
            BitVector temp = groupIndicator1.getBitVector().not();
            temp.shiftLeftUnChangeNum(1);
            groupIndicator2 = SquareZ2Vector.create(temp, false);
        } else {
            groupIndicator2 = SquareZ2Vector.createZeros(groupIndicator1.getNum(), false);
        }

        BitVector[] aggBitArray = Arrays.stream(aggField).map(v -> v.getBitVector()).toArray(BitVector[]::new);
        byte[][] aggByteArray = TransposeUtils.transposeMerge(aggBitArray);
        BigInteger[] aggBigInt = Arrays.stream(aggByteArray).map(v -> BigIntegerUtils.byteArrayToNonNegBigInteger(v)).toArray(BigInteger[]::new);
        SquareZlVector data = SquareZlVector.create(zl, aggBigInt, false);
        // generate prefix sum nodes.
        genNodesBool(data, groupIndicator2, l);
        // prefix-computation
        prefixTree.addPrefix(num);

        // obtain agg fields, transfer to z2Vector
        byte[][] resultBytes = Arrays.stream(nodes)
            .map(PrefixAggNode::getAggShare).map(v -> BigIntegerUtils.nonNegBigIntegerToByteArray(v, byteLen)).toArray(byte[][]::new);
        SquareZ2Vector[] resultZ2Vector = Arrays.stream(TransposeUtils.transposeSplit(resultBytes, l)).map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);

        return resultZ2Vector;
    }

    @Override
    public PrefixAggTypes getAggType() {
        return PrefixAggTypes.SUM;
    }
}
