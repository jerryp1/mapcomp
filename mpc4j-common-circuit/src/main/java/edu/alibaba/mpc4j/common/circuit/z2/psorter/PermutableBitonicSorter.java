package edu.alibaba.mpc4j.common.circuit.z2.psorter;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Bitonic Sorter for permutation generation
 * Only support dim=1 input currently.
 * Bitonic Sorter. Bitonic sort has a complexity of O(m log^2 m) comparisons with small constant, and is data-oblivious
 * since its control flow is independent of the input.
 * <p>
 * The scheme comes from the following paper:
 *
 * <p>
 * Kenneth E. Batcher. 1968. Sorting Networks and Their Applications. In American Federation of Information Processing
 * Societies: AFIPS, Vol. 32. Thomson Book Company, Washington D.C., 307–314.
 * </p>
 *
 * @author Feng Han
 * @date 2023/10/30
 */
public class PermutableBitonicSorter extends AbstractPermutationSorter {
    /**
     * input of sorter
     */
    private MpcZ2Vector[] xiArray;
    /**
     * associated payload
     */
    private MpcZ2Vector[] payloadArrays;
    /**
     * true for ascending order, false for descending order
     */
    private MpcZ2Vector dir;
    /**
     * compare mask, representing the default order that → ← → ← ...
     */
    private byte[][] compareMask;

    public PermutableBitonicSorter(Z2IntegerCircuit circuit) {
        super(circuit);
    }

    @Override
    public MpcZ2Vector[] sort(MpcZ2Vector[][] xiArrays, boolean needPermutation, boolean needStable) throws MpcAbortException {
        return sort(xiArrays, null, PlainZ2Vector.createOnes(xiArrays.length), needPermutation, needStable);
    }

    @Override
    public MpcZ2Vector[] sort(MpcZ2Vector[][] xiArrays, PlainZ2Vector dir, boolean needPermutation, boolean needStable) throws MpcAbortException {
        return sort(xiArrays, null, dir, needPermutation, needStable);
    }

    @Override
    public MpcZ2Vector[] sort(MpcZ2Vector[][] xiArrays, MpcZ2Vector[][] payloadArrays, PlainZ2Vector dir,
                              boolean needPermutation, boolean needStable) throws MpcAbortException {
        assert xiArrays != null;
        sortedNum = xiArrays[0][0].bitNum();
        if (sortedNum == 1) {
            if (needPermutation) {
                return party.setPublicValues(new BitVector[]{BitVectorFactory.createZeros(1)});
            }
        }
        if (dir == null) {
            dir = PlainZ2Vector.createOnes(xiArrays.length);
        }
        MathPreconditions.checkEqual("xiArrays.length", "dir.bitNum", xiArrays.length, dir.bitNum());
        this.needPermutation = needPermutation;
        this.needStable = needStable;
        this.dir = dir;
        initMask();
        dealInput(xiArrays, payloadArrays, needPermutation, needStable);

        bitonicSort();

        return recoverOutput(xiArrays, payloadArrays, needPermutation);
    }

    private void initMask() {
        compareMask = PSorterUtils.returnCompareResultMask(LongUtils.ceilLog2(sortedNum));
        for (int level = 0; level < LongUtils.ceilLog2(sortedNum) - 1; level++) {
            int validMaskBit = (1 << level) * (sortedNum / (1 << (level + 1)));
            BytesUtils.reduceByteArray(compareMask[level], validMaskBit);
        }
    }

    private void dealInput(MpcZ2Vector[][] xiArrays, MpcZ2Vector[][] payloadArrays, boolean needPermutation, boolean needStable) {
        MathPreconditions.checkEqual("xiArrays.length", "1", xiArrays.length, 1);
        assert dir.getBitVector().get(0);

        xls = Arrays.stream(xiArrays).mapToInt(xiArray -> xiArray.length).toArray();
        yls = payloadArrays == null ? null : Arrays.stream(payloadArrays).mapToInt(payloadArray -> payloadArray.length).toArray();
        MpcZ2Vector[] indexes = (needStable | needPermutation) ? party.setPublicValues(PSorterUtils.getBinaryIndex(sortedNum)) : null;

        this.xiArray = xiArrays[0];
        List<MpcZ2Vector> payloadList = payloadArrays == null ? new LinkedList<>()
            : Arrays.stream(payloadArrays).map(payloadArray -> Arrays.copyOf(payloadArray, payloadArray.length))
            .flatMap(Arrays::stream).collect(Collectors.toList());
        if (needStable || needPermutation) {
            payloadList.addAll(Arrays.stream(indexes).collect(Collectors.toList()));
        }
        this.payloadArrays = payloadList.isEmpty() ? null : payloadList.toArray(new MpcZ2Vector[0]);
        if (needStable) {
            assert this.payloadArrays != null;
            if(party.getParallel()){
                Arrays.stream(this.xiArray).parallel().forEach(MpcZ2Vector::reverseBits);
                Arrays.stream(this.payloadArrays).parallel().forEach(MpcZ2Vector::reverseBits);
            }else{
                Arrays.stream(this.xiArray).forEach(MpcZ2Vector::reverseBits);
                Arrays.stream(this.payloadArrays).forEach(MpcZ2Vector::reverseBits);
            }
        }
    }

    private MpcZ2Vector[] recoverOutput(MpcZ2Vector[][] xiArrays, MpcZ2Vector[][] payloadArrays, boolean needPermutation) {
        System.arraycopy(xiArray, 0, xiArrays[0], 0, xls[0]);
        int index = 0;
        if (yls != null) {
            for (int i = 0; i < yls.length; index += yls[i++]) {
                System.arraycopy(this.payloadArrays, index, payloadArrays[i], 0, yls[i]);
            }
        }
        return needPermutation ? Arrays.copyOfRange(this.payloadArrays, index, this.payloadArrays.length) : null;
    }

    private void bitonicSort() throws MpcAbortException {
        for (int i = 0; i < LongUtils.ceilLog2(sortedNum); i++) {
            dealBigLevel(i);
        }
    }

    private void dealBigLevel(int level) throws MpcAbortException {
        int originXiLen = xiArray.length, originPayloadLen = payloadArrays.length;
        int log2 = LongUtils.ceilLog2(sortedNum);
        MpcZ2Vector[] noUsed = null;
        if(needStable || needPermutation){
            noUsed = Arrays.copyOfRange(payloadArrays, originPayloadLen - log2, originPayloadLen - level - 1);
            MpcZ2Vector[] currentY = new MpcZ2Vector[originPayloadLen - log2 + (needStable ? 0 : level + 1)];
            System.arraycopy(payloadArrays, 0, currentY, 0, originPayloadLen - log2);
            if(needStable){
                MpcZ2Vector[] currentX = new MpcZ2Vector[originXiLen + level + 1];
                System.arraycopy(xiArray, 0, currentX, 0, originXiLen);
                System.arraycopy(payloadArrays, originPayloadLen - level - 1, currentX, originXiLen, level + 1);
                xiArray = currentX;
            }else{
                System.arraycopy(payloadArrays, originPayloadLen - level - 1, currentY, originPayloadLen - log2, level + 1);
            }
            payloadArrays = currentY;
        }
        for (int i = 0; i <= level; i++) {
            dealOneIter(level, i);
        }
        if(needStable || needPermutation){
            MpcZ2Vector[] currentY = new MpcZ2Vector[originPayloadLen];
            System.arraycopy(payloadArrays, 0, currentY, 0, originPayloadLen - log2);
            System.arraycopy(noUsed, 0, currentY, originPayloadLen - log2, noUsed.length);
            if(needStable){
                System.arraycopy(xiArray, originXiLen, currentY, originPayloadLen - level - 1, level + 1);
                xiArray = Arrays.copyOf(xiArray, originXiLen);
            }else{
                System.arraycopy(payloadArrays, originPayloadLen - log2, currentY, originPayloadLen - level - 1, level + 1);
            }
            payloadArrays = currentY;
        }
    }

    private void dealOneIter(int level, int iterNum) throws MpcAbortException {
        MathPreconditions.checkGreaterOrEqual("level >= iterNum", level, iterNum);
        int skipLen = 1 << (level - iterNum);
        int partLen = skipLen << 1;
        // 每一层有多少个需要排序的part，如果剩下的那个小part不满skipLen，则不需要比较了
        int currentSortNum = sortedNum / partLen + (sortedNum % partLen > skipLen ? 1 : 0);
        // 第一个需要比较的组，长度与skipLen相差多少，可能是0，也可能是0到skipLen之间的数字，即 0 <= x < skipLen
        int lessCompareLen = sortedNum % partLen <= skipLen ? 0 : partLen - sortedNum % partLen;
        // 得到总共需要比较多少组
        int totalCompareNum = currentSortNum * skipLen - lessCompareLen;
        // 得到比较结果的mask，现有的mask不包含最后一层，实际上最后一层不用翻转比较结果
        byte[] currentMask = level == LongUtils.ceilLog2(sortedNum) - 1 ? new byte[CommonUtils.getByteLength(totalCompareNum)] : BytesUtils.keepLastBits(compareMask[level], totalCompareNum);
        compareExchange(totalCompareNum, skipLen, BitVectorFactory.create(totalCompareNum, currentMask));
    }

    private void compareExchange(int totalCompareNum, int skipLen, BitVector plainCompareMask) throws MpcAbortException {
        if (!dir.getBitVector().get(0)) {
            plainCompareMask.noti();
        }
        MpcZ2Vector compareMaskVec = party.setPublicValues(new BitVector[]{plainCompareMask})[0];
        MpcZ2Vector[] upperX = new MpcZ2Vector[xiArray.length], belowX = new MpcZ2Vector[xiArray.length];

        IntStream intStream = party.getParallel() ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            MpcZ2Vector[] tmp = xiArray[i].getBitsWithSkip(totalCompareNum, skipLen);
            upperX[i] = tmp[0];
            belowX[i] = tmp[1];
        });

        // 先比较得到是否需要交换顺序的flag，如果=0，则不用换顺序，如果=1，则换顺序
        MpcZ2Vector compFlag = party.xor(party.not(circuit.leq(upperX, belowX)), compareMaskVec);
        MpcZ2Vector[] flags = IntStream.range(0, xiArray.length).mapToObj(i -> compFlag).toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] switchX = party.and(flags, party.xor(upperX, belowX));
        intStream = party.getParallel() ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        MpcZ2Vector[] extendSwitchX = intStream.mapToObj(i -> switchX[i].extendBitsWithSkip(sortedNum, skipLen)).toArray(MpcZ2Vector[]::new);

        xiArray = party.xor(extendSwitchX, xiArray);

        // 然后再处理payload
        if (payloadArrays != null) {
            MpcZ2Vector[] upperPayload = new MpcZ2Vector[payloadArrays.length], belowPayload = new MpcZ2Vector[payloadArrays.length];
            intStream = party.getParallel() ? IntStream.range(0, payloadArrays.length).parallel() : IntStream.range(0, payloadArrays.length);
            intStream.forEach(i -> {
                MpcZ2Vector[] tmp = payloadArrays[i].getBitsWithSkip(totalCompareNum, skipLen);
                upperPayload[i] = tmp[0];
                belowPayload[i] = tmp[1];
            });
            flags = IntStream.range(0, payloadArrays.length).mapToObj(i -> compFlag).toArray(MpcZ2Vector[]::new);
            MpcZ2Vector[] switchPayload = party.and(flags, party.xor(upperPayload, belowPayload));
            intStream = party.getParallel() ? IntStream.range(0, payloadArrays.length).parallel() : IntStream.range(0, payloadArrays.length);
            MpcZ2Vector[] extendSwitchPayload = intStream.mapToObj(i -> switchPayload[i].extendBitsWithSkip(sortedNum, skipLen)).toArray(MpcZ2Vector[]::new);
            payloadArrays = party.xor(extendSwitchPayload, payloadArrays);
        }
    }
}
