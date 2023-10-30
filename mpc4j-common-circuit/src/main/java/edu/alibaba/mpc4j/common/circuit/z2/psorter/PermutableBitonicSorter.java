package edu.alibaba.mpc4j.common.circuit.z2.psorter;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PermutableBitonicSorter extends AbstractPermutationSorter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermutableBitonicSorter.class);

//    private static class ComparePlan {
//        public final int startIndex;
//        public final int skipLen;
//        public final int len;
//        public final boolean whatDir;
//
//        public ComparePlan(int startIndex, int skipLen, int len, boolean whatDir) {
//            this.startIndex = startIndex;
//            this.skipLen = skipLen;
//            this.len = len;
//            this.whatDir = whatDir;
//        }
//    }

    //    private MpcZ2Vector[] ascendingArray;
//    private MpcZ2Vector[] descendingArray;
    private MpcZ2Vector[] xiArray;
    private MpcZ2Vector[] payloadArrays;
    private MpcZ2Vector dir;
//    private List<List<ComparePlan>> compare2kPlanList;
//    private List<ComparePlan> mergeComparePlanList;

    private byte[][] compareMask;

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
        // 得到了compare mask，是每个大level的mask，但大level中每个小循环中实际参与比较的数量可能不一致，所以后续执行的过程中要根据实际比较的数量进行截断
        // 此外，如果发现比较的组数为奇数的时候，说明
        this.compareMask = PSorterUtils.returnCompareResultMask(LongUtils.ceilLog2(sortedNum));

        MathPreconditions.checkEqual("xiArrays.length", "1", xiArrays.length, 1);
        assert dir.getBitVector().get(0);
        this.xiArray = xiArrays[0];
//        getInputInOrder(xiArrays);
        getPayload(payloadArrays);

//        compare2kPlanList = new ArrayList<>();
//        IntStream.range(0, LongUtils.ceilLog2(sortedNum, 1)).forEach(i -> compare2kPlanList.add(new LinkedList<>()));
//        mergeComparePlanList = new LinkedList<>();
        bitonicSort();

//        recoverInput(xiArrays);
        xiArrays[0] = this.xiArray;
        return recoverPayload(payloadArrays);
    }

    //    private void getInputInOrder(MpcZ2Vector[][] xiArrays){
//        List<MpcZ2Vector> ascengingList = new LinkedList<>(), descengingList = new LinkedList<>();
//        for(int i = 0; i < xls.length; i++){
//            List<MpcZ2Vector> tmp = Arrays.stream(Arrays.copyOf(xiArrays[i], xiArrays.length)).collect(Collectors.toList());
//            if(dir.getBitVector().get(i)){
//                ascengingList.addAll(tmp);
//            }else{
//                descengingList.addAll(tmp);
//            }
//        }
//        ascendingArray = ascengingList.isEmpty() ? null : ascengingList.toArray(new MpcZ2Vector[0]);
//        descendingArray = descengingList.isEmpty() ? null : descengingList.toArray(new MpcZ2Vector[0]);
//    }
//    private void recoverInput(MpcZ2Vector[][] xiArrays){
//        for(int i = 0, ascIndex = 0, descIndex = 0; i < xls.length; i++){
//            if(dir.getBitVector().get(i)){
//                xiArrays[i] = Arrays.copyOfRange(ascendingArray, ascIndex, ascIndex + xls[i]);
//                ascIndex += xls[i];
//            }else{
//                xiArrays[i] = Arrays.copyOfRange(descendingArray, descIndex, descIndex + xls[i]);
//                descIndex += xls[i];
//            }
//        }
//    }
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

//        long[] data = transport(this.xiArray);
//        LOGGER.info("level - {}, res:{}", level, Arrays.toString(data));
//        int part = 1 << (level + 1);
//        boolean flag = level == LongUtils.ceilLog2(sortedNum) - 1;
//        for(int startIndex = sortedNum - part; startIndex >= 0; startIndex -= part, flag = !flag){
//            for(int i = startIndex; i < startIndex + part - 1; i++){
//                if(flag ? data[i] > data[i + 1] : data[i] < data[i + 1]){
//                    LOGGER.info("startIndex:{}, i:{}, data[i]:{}, data[i + 1]:{}, flag:{}", startIndex, i, data[i], data[i + 1], flag);
//                    LOGGER.info(Arrays.toString(Arrays.copyOfRange(data, startIndex, startIndex + part)));
//                }
//            }
//        }
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
        int validMaskBit = (1<<level) * (sortedNum / (1<<(level+1)));
        BytesUtils.reduceByteArray(currentMask, validMaskBit);
        MpcZ2Vector currentMaskVec = party.setPublicValues(new BitVector[]{BitVectorFactory.create(totalCompareNum, currentMask)})[0];
        compareExchange(totalCompareNum, skipLen, currentMaskVec);

    }

    private void compareExchange(int totalCompareNum, int skipLen, MpcZ2Vector compareMaskVec) throws MpcAbortException {
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

//        LOGGER.info("before skipLen - {}, res:{}", skipLen, Arrays.toString(transport(xiArray)));
//        LOGGER.info("upperX:{}", Arrays.toString(transport(upperX)));
//        LOGGER.info("belowX:{}", Arrays.toString(transport(belowX)));
//        LOGGER.info("compareMaskVec:{}", compareMaskVec.getBitVector().toString());
//        LOGGER.info("compFlag:{}", compFlag.getBitVector().toString());

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

    public static long[] transport(MpcZ2Vector[] data) {
        BitVector[] permutationVec = Arrays.stream(data).map(MpcZ2Vector::getBitVector).toArray(BitVector[]::new);
        BigInteger[] permutationVecTrans = ZlDatabase.create(EnvType.STANDARD, false, permutationVec).getBigIntegerData();
        return Arrays.stream(permutationVecTrans).mapToLong(BigInteger::longValue).toArray();
    }

//
//
//    private static MpcZ2Vector[] getBitsWithSkip(MpcZ2Vector data, int totalCompareNum, int skipLen) {
//        MathPreconditions.checkEqual("skipLen", "2^k", 1 << LongUtils.ceilLog2(skipLen), skipLen);
//        byte[] src = data.getBitVector().getBytes();
//        int destByteNum = CommonUtils.getByteLength(totalCompareNum);
//        byte[][] res = new byte[2][destByteNum];
//        int groupNum = totalCompareNum / skipLen + (totalCompareNum % skipLen > 0 ? 1 : 0);
//
//        // 如果第一个的length和其他的不一致，则先处理第一个
//        if (totalCompareNum % skipLen > 0) {
//            int destOffset = (destByteNum << 3) - totalCompareNum;
//            int firstLen = totalCompareNum % skipLen;
//            for (int i = 0; i < firstLen; i++, destOffset++) {
//                if (data.getBitVector().get(i)) {
//                    BinaryUtils.setBoolean(res[0], destOffset, true);
//                }
//                if (data.getBitVector().get(i + skipLen)) {
//                    BinaryUtils.setBoolean(res[1], destOffset, true);
//                }
//            }
//        }
//        // 处理后续满skipLen的数据
//        int notFullNum = totalCompareNum % skipLen > 0 ? 1 : 0;
//        if (skipLen >= 8) {
//            int eachByteNum = skipLen >> 3, eachPartNum = eachByteNum << 1;
//            int srcEndIndex = src.length, destEndIndex = destByteNum;
//            for (int i = groupNum - 1; i >= notFullNum; i--, destEndIndex -= eachByteNum, srcEndIndex -= eachPartNum) {
//                System.arraycopy(src, srcEndIndex - eachByteNum, res[1], destEndIndex - eachByteNum, eachByteNum);
//                System.arraycopy(src, srcEndIndex - eachPartNum, res[0], destEndIndex - eachByteNum, eachByteNum);
//            }
//        } else {
//            int andNum = (1 << skipLen) - 1;
//            // todo 先用最简单的方法处理
//            int currentDestIndex = (destByteNum << 3) - skipLen, currentSrcIndex = (src.length << 3) - skipLen;
//            for (int i = groupNum - 1; i >= notFullNum; i--) {
//                for (int j = 0; j < skipLen; j++) {
//                    if (BinaryUtils.getBoolean(src, currentSrcIndex + j)) {
//                        BinaryUtils.setBoolean(res[1], currentDestIndex + j, true);
//                    }
//                }
//                currentSrcIndex -= skipLen;
//                for (int j = 0; j < skipLen; j++) {
//                    if (BinaryUtils.getBoolean(src, currentSrcIndex + j)) {
//                        BinaryUtils.setBoolean(res[0], currentDestIndex + j, true);
//                    }
//                }
//                currentSrcIndex -= skipLen;
//                currentDestIndex -= skipLen;
//            }
//        }
//        return data instanceof SquareZ2Vector
//            ? Arrays.stream(res).map(x -> SquareZ2Vector.create(totalCompareNum, x, data.isPlain())).toArray(SquareZ2Vector[]::new)
//            : Arrays.stream(res).map(x -> PlainZ2Vector.create(totalCompareNum, x)).toArray(PlainZ2Vector[]::new);
//    }
//
//    private static MpcZ2Vector extendBitsWithSkip(MpcZ2Vector data, int destBitLen, int skipLen) {
//        MathPreconditions.checkEqual("skipLen", "2^k", 1 << LongUtils.ceilLog2(skipLen), skipLen);
//        int destByteNum = CommonUtils.getByteLength(destBitLen);
//        byte[] destByte = new byte[destByteNum];
//        int notFullNum = destBitLen % (skipLen << 1) - skipLen > 0 ? 1 : 0;
//        int groupNum = destBitLen / (skipLen << 1) + notFullNum;
//        // 如果第一个的length和其他的不一致，则先处理第一个
//        if (notFullNum > 0) {
//            // 如果第一个组是未满的，则之前的比较一定是从第一个开始取数比较的
//            int destOffset = (destByteNum << 3) - destBitLen;
//            int firstLen = destBitLen % skipLen;
//            for (int i = 0; i < firstLen; i++, destOffset++) {
//                if (data.getBitVector().get(i)) {
//                    BinaryUtils.setBoolean(destByte, destOffset, true);
//                    BinaryUtils.setBoolean(destByte, destOffset + skipLen, true);
//                }
//            }
//        }
//        // 处理后续满skipLen的数据
//        byte[] srcByte = data.getBitVector().getBytes();
//        if (skipLen >= 8) {
//            int eachByteNum = skipLen >> 3, eachPartNum = eachByteNum << 1;
//            int srcEndIndex = srcByte.length, destEndIndex = destByteNum;
//            for (int i = groupNum - 1; i >= notFullNum; i--, destEndIndex -= eachPartNum, srcEndIndex -= eachByteNum) {
//                System.arraycopy(srcByte, srcEndIndex - eachByteNum, destByte, destEndIndex - eachByteNum, eachByteNum);
//                System.arraycopy(srcByte, srcEndIndex - eachByteNum, destByte, destEndIndex - eachPartNum, eachByteNum);
//            }
//        } else {
//            // todo 先用最简单的方法处理
//            int eachPartBit = skipLen << 1;
//            int currentDestIndex = (destByteNum << 3) - eachPartBit, currentSrcIndex = (srcByte.length << 3) - skipLen;
//            for (int i = groupNum - 1; i >= notFullNum; i--) {
//                for (int j = 0; j < skipLen; j++) {
//                    if (BinaryUtils.getBoolean(srcByte, currentSrcIndex + j)) {
//                        BinaryUtils.setBoolean(destByte, currentDestIndex + j, true);
//                        BinaryUtils.setBoolean(destByte, currentDestIndex + j + skipLen, true);
//                    }
//                }
//                currentSrcIndex -= skipLen;
//                currentDestIndex -= eachPartBit;
//            }
//        }
//        return data instanceof SquareZ2Vector ? SquareZ2Vector.create(destBitLen, destByte, data.isPlain()) : PlainZ2Vector.create(destBitLen, destByte);
//    }

//    private void genPlan(int level) {
//        // 这个level的初始skip length应该是多少，初始skip length指的是按照同一个方向，互相比较的两个元素之间的位置距离
//        int initSkipLen = 1 << level;
//        int initPartLen = initSkipLen << 1;
//        // 有多少个需要排序的part
//        int partNum = sortedNum / initPartLen + (sortedNum % initPartLen == 0 ? 0 : 1);
//        // 最外层，每一个part应该的顺序是什么，按照逆序存储。即实际上是正-逆....，但由于我们bitVector和sort的执行逻辑是让后端满2^k，因此按逆序
//        boolean[] posDir = new boolean[partNum];
//        for (int i = 1; i < partNum; i += 2) {
//            posDir[i] = true;
//        }
//        for (int i = level; i >= 0; i--) {
//            int skipLen = 1 << i;
//            int partLen = skipLen << 1;
//            // 每一层有多少个需要排序的part，如果剩下的那个小part不满skipLen，则不需要比较了
//            int currentSortNum = sortedNum / partLen + (sortedNum % partLen > skipLen ? 1 : 0);
//            // 第一个需要比较的组，长度并不一定是2^k
//            int firstCompareLen = (sortedNum % partLen == 0 ? partLen : sortedNum % partLen) - skipLen;
//        }
//    }
//    static int greatPowerOfTwoLessThan(int m) {
//        return 1 << (BigInteger.valueOf(m - 1).bitLength() - 1);
//    }
//
//    static int smallPowerOfTwoGreaterEqualThan(int m) {
//        return 1 << LongUtils.ceilLog2(m);
//    }
}
