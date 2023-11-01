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
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public MpcZ2Vector[] sort(MpcZ2Vector[][] xiArrays, boolean needPermutation) throws MpcAbortException {
        return sort(xiArrays, null, PlainZ2Vector.createOnes(xiArrays.length), needPermutation);
    }

    @Override
    public MpcZ2Vector[] sort(MpcZ2Vector[][] xiArrays, PlainZ2Vector dir, boolean needPermutation) throws MpcAbortException {
        return sort(xiArrays, null, dir, needPermutation);
    }

    @Override
    public MpcZ2Vector[] sort(MpcZ2Vector[][] xiArrays, MpcZ2Vector[][] payloadArrays, PlainZ2Vector dir, boolean needPermutation) throws MpcAbortException {
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
        xls = Arrays.stream(xiArrays).mapToInt(xiArray -> xiArray.length).toArray();
        yls = payloadArrays == null ? null : Arrays.stream(payloadArrays).mapToInt(payloadArray -> payloadArray.length).toArray();
        this.needPermutation = needPermutation;
        this.dir = dir;
        initMask();

        MathPreconditions.checkEqual("xiArrays.length", "1", xiArrays.length, 1);
        assert dir.getBitVector().get(0);
        this.xiArray = xiArrays[0];
        getPayload(payloadArrays);

        StopWatch s = new StopWatch();
        s.start();
        bitonicSort();
        s.stop();

        xiArrays[0] = this.xiArray;

        LOGGER.info("sort time:{}, bit time:{}, compare time:{}", s.getTime(TimeUnit.MILLISECONDS), getBitTime, compareTime);

        return recoverPayload(payloadArrays);
    }

    private void initMask(){
        compareMask = PSorterUtils.returnCompareResultMask(LongUtils.ceilLog2(sortedNum));
        for(int level = 0; level < LongUtils.ceilLog2(sortedNum) - 1; level++){
            int validMaskBit = (1<<level) * (sortedNum / (1<<(level+1)));
            BytesUtils.reduceByteArray(compareMask[level], validMaskBit);
        }
    }

    private void getPayload(MpcZ2Vector[][] payloadArrays) {
        List<MpcZ2Vector> all = new LinkedList<>();
        if (payloadArrays != null) {
            all.addAll(Arrays.stream(payloadArrays).map(payloadArray ->
                    Arrays.copyOf(payloadArray, payloadArray.length))
                .flatMap(Arrays::stream).collect(Collectors.toList()));
        }
        if (needPermutation) {
            BitVector[] indexes = PSorterUtils.getBinaryIndex(sortedNum);
            all.addAll(Arrays.stream(party.setPublicValues(indexes)).collect(Collectors.toList()));
        }
        this.payloadArrays = all.isEmpty() ? null : all.toArray(new MpcZ2Vector[0]);
    }

    private MpcZ2Vector[] recoverPayload(MpcZ2Vector[][] payloadArrays) {
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
        MpcZ2Vector currentMaskVec = party.setPublicValues(new BitVector[]{BitVectorFactory.create(totalCompareNum, currentMask)})[0];
        // todo 如果排序多个字段，或者降序排，需要dir
        currentMaskVec = dir.getBitVector().get(0) ? currentMaskVec : party.not(currentMaskVec);
        compareExchange(totalCompareNum, skipLen, currentMaskVec);
    }

    private void compareExchange(int totalCompareNum, int skipLen, MpcZ2Vector compareMaskVec) throws MpcAbortException {
        MpcZ2Vector[] upperX = new MpcZ2Vector[xiArray.length], belowX = new MpcZ2Vector[xiArray.length];

        stopWatch.start();
        IntStream intStream = party.getParallel() ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            MpcZ2Vector[] tmp = xiArray[i].getBitsWithSkip(totalCompareNum, skipLen);
            upperX[i] = tmp[0];
            belowX[i] = tmp[1];
        });
        stopWatch.stop();
        getBitTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();

        // 先比较得到是否需要交换顺序的flag，如果=0，则不用换顺序，如果=1，则换顺序
        stopWatch.start();
        MpcZ2Vector compFlag = party.xor(party.not(circuit.leqParallel(upperX, belowX)), compareMaskVec);
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
}
