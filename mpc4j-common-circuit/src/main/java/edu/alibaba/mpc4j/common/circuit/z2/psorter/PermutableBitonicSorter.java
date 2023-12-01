package edu.alibaba.mpc4j.common.circuit.z2.psorter;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(PermutableBitonicSorter.class);
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
    /**
     * timer
     */
    private StopWatch stopWatch;
    /**
     * time for bit cut and compare
     */
    private long getBitTime, compareTime;


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
    public MpcZ2Vector[] sort(MpcZ2Vector[][] xiArrays, MpcZ2Vector[][] payloadArrays, PlainZ2Vector dir, boolean needPermutation, boolean needStable) throws MpcAbortException {
        getBitTime = 0;
        compareTime = 0;
        stopWatch = new StopWatch();

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
        this.dir = dir;
        initMask();
        dealInput(xiArrays, payloadArrays, needPermutation, needStable);

        StopWatch s = new StopWatch();
        s.start();
        bitonicSort();
        s.stop();
        LOGGER.info("sort time:{}, bit time:{}, compare time:{}", s.getTime(TimeUnit.MILLISECONDS), getBitTime, compareTime);

        return recoverOutput(xiArrays, payloadArrays, needPermutation, needStable);
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
        if(needStable){
            this.xiArray = new MpcZ2Vector[xiArrays[0].length + indexes.length];
            System.arraycopy(xiArrays[0], 0, this.xiArray, 0, xiArrays[0].length);
            System.arraycopy(indexes, 0, this.xiArray, xiArrays[0].length, indexes.length);
        }else{
            this.xiArray = xiArrays[0];
        }
        List<MpcZ2Vector> payloadList  = payloadArrays == null ? new LinkedList<>()
            : Arrays.stream(payloadArrays).map(payloadArray -> Arrays.copyOf(payloadArray, payloadArray.length))
            .flatMap(Arrays::stream).collect(Collectors.toList());
        if((!needStable) & needPermutation){
            payloadList.addAll(Arrays.stream(indexes).collect(Collectors.toList()));
        }
        this.payloadArrays = payloadList.isEmpty() ? null : payloadList.toArray(new MpcZ2Vector[0]);
    }

    private MpcZ2Vector[] recoverOutput(MpcZ2Vector[][] xiArrays, MpcZ2Vector[][] payloadArrays, boolean needPermutation, boolean needStable) {
        System.arraycopy(xiArray, 0, xiArrays[0], 0,xls[0]);
        int index = 0;
        if (yls != null) {
            for (int i = 0; i < yls.length; index += yls[i++]) {
                System.arraycopy(this.payloadArrays, index, payloadArrays[i], 0, yls[i]);
            }
        }
        if(needPermutation){
            return needStable ? Arrays.copyOfRange(xiArray, xls[0], xiArray.length)
                : Arrays.copyOfRange(this.payloadArrays, index, this.payloadArrays.length);
        }else{
            return null;
        }
    }

//    private void getPayload(MpcZ2Vector[][] payloadArrays) {
//        List<MpcZ2Vector> all = new LinkedList<>();
//        if (payloadArrays != null) {
//            all.addAll(Arrays.stream(payloadArrays).map(payloadArray ->
//                    Arrays.copyOf(payloadArray, payloadArray.length))
//                .flatMap(Arrays::stream).collect(Collectors.toList()));
//        }
//        if (needPermutation) {
//            BitVector[] indexes = PSorterUtils.getBinaryIndex(sortedNum);
//            all.addAll(Arrays.stream(party.setPublicValues(indexes)).collect(Collectors.toList()));
//        }
//        this.payloadArrays = all.isEmpty() ? null : all.toArray(new MpcZ2Vector[0]);
//    }
//
//    private MpcZ2Vector[] recoverPayload(MpcZ2Vector[][] payloadArrays) {
//        int index = 0;
//        if (yls != null) {
//            for (int i = 0; i < yls.length; index += yls[i++]) {
//                System.arraycopy(this.payloadArrays, index, payloadArrays[i], 0, yls[i]);
//            }
//        }
//        return needPermutation ? Arrays.copyOfRange(this.payloadArrays, index, this.payloadArrays.length) : null;
//    }

    private void bitonicSort() throws MpcAbortException {
        for (int i = 0; i < LongUtils.ceilLog2(sortedNum); i++) {
            dealBigLevel(i);
        }
    }

    private void dealBigLevel(int level) throws MpcAbortException {
        for (int i = 0; i <= level; i++) {
            dealOneIter(level, i);
        }
//        long[] data = PSorterUtils.transport(this.xiArray);
//        LOGGER.info("level - {}, res:{}", level, Arrays.toString(data));
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
        // todo 如果排序多个字段，或者降序排，需要dir
        compareExchange(totalCompareNum, skipLen, BitVectorFactory.create(totalCompareNum, currentMask));
    }

    private void compareExchange(int totalCompareNum, int skipLen, BitVector plainCompareMask) throws MpcAbortException {
        if(!dir.getBitVector().get(0)){
            plainCompareMask.noti();
        }
        MpcZ2Vector compareMaskVec = party.setPublicValues(new BitVector[]{plainCompareMask})[0];
        MpcZ2Vector[] upperX = new MpcZ2Vector[xiArray.length], belowX = new MpcZ2Vector[xiArray.length];

        stopWatch.start();
        IntStream intStream = party.getParallel() ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            MpcZ2Vector[] tmp = xiArray[i].getBitsWithSkip(totalCompareNum, skipLen);
            upperX[i] = tmp[0];
            belowX[i] = tmp[1];
        });
        MpcZ2Vector[] flagSwitchCmp = Arrays.stream(belowX).map(x -> party.create(plainCompareMask)).toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] xorUpperAndBelow = party.and(flagSwitchCmp, party.xor(upperX, belowX));

        MpcZ2Vector[] cmpUpperX = party.xor(xorUpperAndBelow, upperX);
        MpcZ2Vector[] cmpBelowX = party.xor(xorUpperAndBelow, belowX);
        // 如果需要逆转过来比较，那么标志应该是什么？
        // 当正向的时候，compare mask = 1的时候，应该反过来比较，即计算一个新的数据为：upper = mask \cdot (upper \xor below) \xor upper
        // 当反向的时候，compare mask = 0的时候，应该反过来吗？
        stopWatch.stop();
        getBitTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();

        // 先比较得到是否需要交换顺序的flag，如果=0，则不用换顺序，如果=1，则换顺序
        stopWatch.start();

        BitVector[] upperBitVec = Arrays.stream(upperX).map(MpcZ2Vector::getBitVector).toArray(BitVector[]::new);
        BitVector[] belowBitVec = Arrays.stream(belowX).map(MpcZ2Vector::getBitVector).toArray(BitVector[]::new);
        BigInteger[] upperPlain = ZlDatabase.create(EnvType.STANDARD, true, upperBitVec).getBigIntegerData();
        BigInteger[] belowPlain = ZlDatabase.create(EnvType.STANDARD, true, belowBitVec).getBigIntegerData();
        BitVector maskV = compareMaskVec.getBitVector();
        boolean[] switchFlag = new boolean[maskV.bitNum()];
        for(int i = 0; i < switchFlag.length; i++){
            if(maskV.get(i)){
                switchFlag[i] = belowPlain[i].compareTo(upperPlain[i]) > 0;
            }else{
                switchFlag[i] = upperPlain[i].compareTo(belowPlain[i]) > 0;
            }
        }
        BitVector trueFlag = BitVectorFactory.create(switchFlag.length, BinaryUtils.binaryToRoundByteArray(switchFlag));
        MpcZ2Vector compFlag = PlainZ2Vector.create(trueFlag);

        MpcZ2Vector compFlag1 = party.not(circuit.leq(cmpUpperX, cmpBelowX));
        if(!Arrays.equals(compFlag.getBitVector().getBytes(), compFlag1.getBitVector().getBytes())){
            LOGGER.info("cmpUpperX:{}", Arrays.toString(PSorterUtils.transport(cmpUpperX)));
            LOGGER.info("cmpBelowX:{}", Arrays.toString(PSorterUtils.transport(cmpBelowX)));
            LOGGER.info("cmpRes:{}", circuit.leq(cmpUpperX, cmpBelowX).getBitVector());
            LOGGER.info("compFlag:{}", compFlag.getBitVector());
            LOGGER.info("compFlag1:{}", compFlag1.getBitVector());
        }


//        MpcZ2Vector compFlag = party.xor(party.not(circuit.leq(upperX, belowX)), compareMaskVec);
        stopWatch.stop();
        compareTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();

        MpcZ2Vector[] flags = IntStream.range(0, xiArray.length).mapToObj(i -> compFlag).toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] switchX = party.and(flags, party.xor(upperX, belowX));

//        LOGGER.info("before skipLen - {}, res:{}", skipLen, Arrays.toString(PSorterUtils.transport(xiArray)));
//        LOGGER.info("upperX:{}", Arrays.toString(PSorterUtils.transport(upperX)));
//        LOGGER.info("belowX:{}", Arrays.toString(PSorterUtils.transport(belowX)));
//        LOGGER.info("compareMaskVec:{}", compareMaskVec.getBitVector().toString());
//        LOGGER.info("compFlag:{}", compFlag.getBitVector().toString());
//        LOGGER.info("indexes:{}", Arrays.toString(PSorterUtils.transport(Arrays.copyOf(payloadArrays, LongUtils.ceilLog2(payloadArrays[0].bitNum())))));

        stopWatch.start();
        intStream = party.getParallel() ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        MpcZ2Vector[] extendSwitchX = intStream.mapToObj(i -> switchX[i].extendBitsWithSkip(sortedNum, skipLen)).toArray(MpcZ2Vector[]::new);
        stopWatch.stop();
        getBitTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();

        xiArray = party.xor(extendSwitchX, xiArray);

        // 然后再处理payload
        if (payloadArrays != null) {
            MpcZ2Vector[] upperPayload = new MpcZ2Vector[payloadArrays.length], belowPayload = new MpcZ2Vector[payloadArrays.length];

            stopWatch.start();
            intStream = party.getParallel() ? IntStream.range(0, payloadArrays.length).parallel() : IntStream.range(0, payloadArrays.length);
            intStream.forEach(i -> {
                MpcZ2Vector[] tmp = payloadArrays[i].getBitsWithSkip(totalCompareNum, skipLen);
                upperPayload[i] = tmp[0];
                belowPayload[i] = tmp[1];
            });
            stopWatch.stop();
            getBitTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();

            flags = IntStream.range(0, payloadArrays.length).mapToObj(i -> compFlag).toArray(MpcZ2Vector[]::new);
            MpcZ2Vector[] switchPayload = party.and(flags, party.xor(upperPayload, belowPayload));

            stopWatch.start();
            intStream = party.getParallel() ? IntStream.range(0, payloadArrays.length).parallel() : IntStream.range(0, payloadArrays.length);
            MpcZ2Vector[] extendSwitchPayload = intStream.mapToObj(i -> switchPayload[i].extendBitsWithSkip(sortedNum, skipLen)).toArray(MpcZ2Vector[]::new);
            stopWatch.stop();
            getBitTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();

            payloadArrays = party.xor(extendSwitchPayload, payloadArrays);
        }
    }

//    private void compareExchange(int totalCompareNum, int skipLen, MpcZ2Vector compareMaskVec) throws MpcAbortException {
//        MpcZ2Vector[] upperX = new MpcZ2Vector[xiArray.length], belowX = new MpcZ2Vector[xiArray.length];
//
//        stopWatch.start();
//        IntStream intStream = party.getParallel() ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
//        intStream.forEach(i -> {
//            MpcZ2Vector[] tmp = xiArray[i].getBitsWithSkip(totalCompareNum, skipLen);
//            upperX[i] = tmp[0];
//            belowX[i] = tmp[1];
//        });
//        stopWatch.stop();
//        getBitTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//
//        // 先比较得到是否需要交换顺序的flag，如果=0，则不用换顺序，如果=1，则换顺序
//        stopWatch.start();
//        MpcZ2Vector compFlag = party.xor(party.not(circuit.leq(upperX, belowX)), compareMaskVec);
//        stopWatch.stop();
//        compareTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//
//        MpcZ2Vector[] flags = IntStream.range(0, xiArray.length).mapToObj(i -> compFlag).toArray(MpcZ2Vector[]::new);
//        MpcZ2Vector[] switchX = party.and(flags, party.xor(upperX, belowX));
//
////        LOGGER.info("before skipLen - {}, res:{}", skipLen, Arrays.toString(PSorterUtils.transport(xiArray)));
////        LOGGER.info("upperX:{}", Arrays.toString(PSorterUtils.transport(upperX)));
////        LOGGER.info("belowX:{}", Arrays.toString(PSorterUtils.transport(belowX)));
////        LOGGER.info("compareMaskVec:{}", compareMaskVec.getBitVector().toString());
////        LOGGER.info("compFlag:{}", compFlag.getBitVector().toString());
//
//        stopWatch.start();
//        intStream = party.getParallel() ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
//        MpcZ2Vector[] extendSwitchX = intStream.mapToObj(i -> switchX[i].extendBitsWithSkip(sortedNum, skipLen)).toArray(MpcZ2Vector[]::new);
//        stopWatch.stop();
//        getBitTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//
//        xiArray = party.xor(extendSwitchX, xiArray);
//
//        // 然后再处理payload
//        if (payloadArrays != null) {
//            MpcZ2Vector[] upperPayload = new MpcZ2Vector[payloadArrays.length], belowPayload = new MpcZ2Vector[payloadArrays.length];
//
//            stopWatch.start();
//            intStream = party.getParallel() ? IntStream.range(0, payloadArrays.length).parallel() : IntStream.range(0, payloadArrays.length);
//            intStream.forEach(i -> {
//                MpcZ2Vector[] tmp = payloadArrays[i].getBitsWithSkip(totalCompareNum, skipLen);
//                upperPayload[i] = tmp[0];
//                belowPayload[i] = tmp[1];
//            });
//            stopWatch.stop();
//            getBitTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
//            stopWatch.reset();
//
//            flags = IntStream.range(0, payloadArrays.length).mapToObj(i -> compFlag).toArray(MpcZ2Vector[]::new);
//            MpcZ2Vector[] switchPayload = party.and(flags, party.xor(upperPayload, belowPayload));
//
//            stopWatch.start();
//            intStream = party.getParallel() ? IntStream.range(0, payloadArrays.length).parallel() : IntStream.range(0, payloadArrays.length);
//            MpcZ2Vector[] extendSwitchPayload = intStream.mapToObj(i -> switchPayload[i].extendBitsWithSkip(sortedNum, skipLen)).toArray(MpcZ2Vector[]::new);
//            stopWatch.stop();
//            getBitTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
//            stopWatch.reset();
//
//            payloadArrays = party.xor(extendSwitchPayload, payloadArrays);
//        }
//    }
//

}
