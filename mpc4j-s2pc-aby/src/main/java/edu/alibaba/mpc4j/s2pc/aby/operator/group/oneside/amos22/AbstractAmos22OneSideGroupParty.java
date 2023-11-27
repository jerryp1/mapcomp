package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.AbstractOneSideGroupParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory.AggTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory.OneSideGroupPartyTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupUtils;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxParty;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

public abstract class AbstractAmos22OneSideGroupParty extends AbstractOneSideGroupParty implements OneSideGroupParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAmos22OneSideGroupParty.class);
    private final Z2cParty z2cParty;
    private final Z2IntegerCircuit z2IntegerCircuit;
    private final PlainBitMuxParty plainBitMuxParty;
    private SquareZ2Vector[] pValues;
    private SquareZ2Vector[] sValues;
    private SquareZ2Vector[] resultData;
    private SquareZ2Vector[] inputDataProcessed;

    @Override
    public int[] getResPosFlag(BitVector groupFlag) {
        BitVector[][] params = getPlainBitVectorsNew(groupFlag);
        BitVector r = params[params.length - 1][0];
        TIntList updateIndexes = new TIntLinkedList();
        for(int i = 0; i < groupFlag.bitNum(); i++){
            if(r.get(i)){
                updateIndexes.add(i);
            }
        }
        return Arrays.stream(updateIndexes.toArray()).sorted().toArray();
    }

    public static BitVector[][] getPlainBitVectors(BitVector groupFlag){
        int dataNum = groupFlag.bitNum();
        BitVector choiceBits = BitVectorFactory.createZeros(dataNum);
        boolean[] p1 = BinaryUtils.byteArrayToBinary(groupFlag.getBytes(), dataNum);
        boolean[] p2 = new boolean[dataNum];
        p2[0] = true;
        System.arraycopy(p1, 0, p2, 1, dataNum - 1);
        for (int i = 0; i < dataNum; i++) {
            if (p1[i] & p2[i]) {
                choiceBits.set(i, true);
            }
        }
        int levelNum = LongUtils.ceilLog2(dataNum);
        BitVector[][] flagsInEachRound = new BitVector[levelNum + 1][];
        for (int level = 0; level < levelNum; level++) {
            int childGroupLen = 1 << level;
            int parentLen = childGroupLen << 1;
            int mergeNum = dataNum / parentLen;
            mergeNum += dataNum % parentLen > childGroupLen ? 1 : 0;
            boolean[] updateFlag = new boolean[mergeNum], leftTransFlag = new boolean[mergeNum], rightTransFlag = new boolean[mergeNum];
            for (int i = 0, leftIndex = 0, rightIndex = childGroupLen; i < mergeNum; i++, leftIndex += parentLen, rightIndex += parentLen) {
                updateFlag[i] = (p2[leftIndex] | p1[leftIndex]) & p1[rightIndex] & (!p2[rightIndex]);
                boolean allZero = (!p1[leftIndex]) & (!p1[rightIndex]);
                // 改变2：leftTransFlag可以remove掉 & (!p2[leftIndex])
                leftTransFlag[i] = ((!p1[leftIndex]) & p1[rightIndex]) | (allZero & ((i & 1) == 1));
                rightTransFlag[i] = (p1[leftIndex] & (!p1[rightIndex])) | (allZero & ((i & 1) == 0));
                if (updateFlag[i]) {
                    choiceBits.set(rightIndex - 1, true);
                }
                // 更新值
                p1[leftIndex] = p1[leftIndex] | p1[rightIndex];
            }
            flagsInEachRound[level] = new BitVector[]{
                BitVectorFactory.create(mergeNum, BinaryUtils.binaryToRoundByteArray(updateFlag)),
                BitVectorFactory.create(mergeNum, BinaryUtils.binaryToRoundByteArray(leftTransFlag)),
                BitVectorFactory.create(mergeNum, BinaryUtils.binaryToRoundByteArray(rightTransFlag))
            };
        }
        flagsInEachRound[levelNum] = new BitVector[]{choiceBits};
        return flagsInEachRound;
    }

    protected AbstractAmos22OneSideGroupParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, Amos22OneSideGroupConfig config, OneSideGroupPartyTypes partyTypes) {
        super(ptoDesc, rpc, otherParty, config);
        if (partyTypes.equals(OneSideGroupPartyTypes.RECEIVER)) {
            z2cParty = Z2cFactory.createReceiver(rpc, otherParty, config.getZ2cConfig());
            plainBitMuxParty = PlainBitMuxFactory.createReceiver(rpc, otherParty, config.getPlainBitMuxConfig());
        } else {
            z2cParty = Z2cFactory.createSender(rpc, otherParty, config.getZ2cConfig());
            plainBitMuxParty = PlainBitMuxFactory.createSender(rpc, otherParty, config.getPlainBitMuxConfig());
        }
        z2IntegerCircuit = new Z2IntegerCircuit(z2cParty);
        addMultipleSubPtos(z2cParty, plainBitMuxParty);
    }


    @Override
    public void init(int attrNum, int maxNum, int maxBitNum) throws MpcAbortException {
        setInitInput(attrNum, maxNum, maxBitNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int initSize = attrNum * maxBitNum * maxNum;
        initSize = initSize <= 0 ? Integer.MAX_VALUE : initSize;
        z2cParty.init(initSize);
        plainBitMuxParty.init(initSize);

        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector[] groupAgg(SquareZ2Vector[] xiArrays, SquareZ2Vector validFlags, AggTypes aggTypes, BitVector groupFlag) throws MpcAbortException {
        setInputs(xiArrays, validFlags, aggTypes, groupFlag);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 1. 先计算真实的值
        BitVector[] perpValue = IntStream.range(0, xiArrays.length).mapToObj(i ->
            aggTypes.equals(AggTypes.MAX)
                ? BitVectorFactory.createZeros(dataNum)
                : BitVectorFactory.createOnes(dataNum)).toArray(BitVector[]::new);
        SquareZ2Vector[] perpShare = (SquareZ2Vector[]) z2cParty.setPublicValues(perpValue);
        SquareZ2Vector[] validFs = IntStream.range(0, xiArrays.length).mapToObj(i -> validFlags).toArray(SquareZ2Vector[]::new);
        resultData = z2cParty.xor(z2cParty.and(validFs, z2cParty.xor(perpShare, xiArrays)), perpShare);
        // 2. 计算p值和s值
        // 改变1：pValues不需要判断
        pValues = Arrays.copyOf(resultData, resultData.length);
        sValues = z2cParty.xor(plainBitMuxParty.mux(groupFlag, z2cParty.xor(resultData, perpShare)), resultData);
        int levelNum = LongUtils.ceilLog2(dataNum);
        logStepInfo(PtoState.PTO_STEP, 0, levelNum + 1, resetAndGetTime(), "init end");
        // 3. 循环进行更新
        return commonIter(new AggTypes[]{aggTypes}, groupFlag, 1)[0];
    }

    @Override
    public SquareZ2Vector[][] groupAgg(SquareZ2Vector[][] xiArrays, SquareZ2Vector[] validFlags,
                                       AggTypes[] aggTypes, BitVector groupFlag) throws MpcAbortException {
        setInputs(xiArrays, validFlags, aggTypes, groupFlag);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 1. 先计算真实的值
        SquareZ2Vector[][] zerosAndOnes = new SquareZ2Vector[2][];
        zerosAndOnes[0] = (SquareZ2Vector[]) z2cParty.setPublicValues(
            IntStream.range(0, dimLen).mapToObj(i ->
                BitVectorFactory.createZeros(dataNum)).toArray(BitVector[]::new));
        zerosAndOnes[1] = (SquareZ2Vector[]) z2cParty.setPublicValues(
            IntStream.range(0, dimLen).mapToObj(i ->
                BitVectorFactory.createOnes(dataNum)).toArray(BitVector[]::new));
        SquareZ2Vector[] validFs = new SquareZ2Vector[dimLen * xiArrays.length];
        for(int i = 0, startPos = 0; i < xiArrays.length; i++, startPos += dimLen){
            for(int j = startPos; j < startPos + dimLen; j++){
                validFs[j] = validFlags[i];
            }
        }
        SquareZ2Vector[] perpShare = IntStream.range(0, xiArrays.length).mapToObj(i ->
            aggTypes[i].equals(AggTypes.MAX) ? zerosAndOnes[0] : zerosAndOnes[1])
            .flatMap(Arrays::stream).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] originValues = Arrays.stream(xiArrays).flatMap(Arrays::stream).toArray(SquareZ2Vector[]::new);
        resultData = z2cParty.xor(z2cParty.and(validFs, z2cParty.xor(perpShare, originValues)), perpShare);
        // 2. 计算p值和s值
        // 改变1：pValues不需要判断
        pValues = Arrays.copyOf(resultData, resultData.length);
        sValues = z2cParty.xor(plainBitMuxParty.mux(groupFlag, z2cParty.xor(resultData, perpShare)), resultData);
        int levelNum = LongUtils.ceilLog2(dataNum);
        logStepInfo(PtoState.PTO_STEP, 0, levelNum + 1, resetAndGetTime(), "init end");
        // 3. 循环进行更新
        return commonIter(aggTypes, groupFlag, xiArrays.length);
    }

    public SquareZ2Vector[][] commonIter(AggTypes[] aggTypes, BitVector groupFlag, int attrNum) throws MpcAbortException {
        BitVector[][] params = groupFlag != null ? getPlainBitVectors(groupFlag) : null;
        int levelNum = LongUtils.ceilLog2(dataNum);
        for (int level = 0; level < levelNum; level++) {
            stopWatch.start();
            int childGroupLen = 1 << level;
            int parentLen = childGroupLen << 1;
            int mergeNum = dataNum / parentLen;
            mergeNum += dataNum % parentLen > childGroupLen ? 1 : 0;
            // 3.1 计算各个flag
            // 是否更新的flag
            BitVector uFlagVec = groupFlag == null ? null : params[level][0],
                leftFlagVec = groupFlag == null ? null : params[level][1],
                rightFlagVec = groupFlag == null ? null : params[level][2];
            // 3.2 计算合并值
            SquareZ2Vector[] sl = OneSideGroupUtils.getPos(sValues, childGroupLen - 1, mergeNum, parentLen, parallel);
            SquareZ2Vector[] pr = OneSideGroupUtils.getPos(pValues, childGroupLen, mergeNum, parentLen, parallel);
            // 3.2.1 得到比较结果 v = OP(sl, pr)
            SquareZ2Vector[][] inputSl = IntStream.range(0, attrNum).mapToObj(i ->
                Arrays.copyOfRange(sl, i * dimLen, i * dimLen + dimLen)).toArray(SquareZ2Vector[][]::new);
            SquareZ2Vector[][] inputPr = IntStream.range(0, attrNum).mapToObj(i ->
                Arrays.copyOfRange(pr, i * dimLen, i * dimLen + dimLen)).toArray(SquareZ2Vector[][]::new);
            MpcZ2Vector[] leqRes = z2IntegerCircuit.leqParallel(inputSl, inputPr);

            SquareZ2Vector[] leqFlagExtend = new SquareZ2Vector[attrNum * dimLen];
            for(int i = 0; i < attrNum; i++){
                z2cParty.xori(leqRes[i], aggTypes[i].equals(AggTypes.MAX) ? SquareZ2Vector.createZeros(mergeNum) : SquareZ2Vector.createOnes(mergeNum));
                Arrays.fill(leqFlagExtend, i * dimLen, i * dimLen + dimLen, leqRes[i]);
            }
            SquareZ2Vector[] extremeValues = (SquareZ2Vector[]) z2cParty.mux(sl, pr, leqFlagExtend);
            // 3.3 更新 resultData
            SquareZ2Vector[] originRes = OneSideGroupUtils.getPos(resultData, childGroupLen - 1, mergeNum, parentLen, parallel);
            SquareZ2Vector[] vPrimeValues = z2cParty.xor(plainBitMuxParty.mux(uFlagVec, z2cParty.xor(extremeValues, originRes)), originRes);
            OneSideGroupUtils.setPos(resultData, vPrimeValues, childGroupLen - 1, mergeNum, parentLen, parallel);
            // 3.4 更新p和s
            SquareZ2Vector[] pl = OneSideGroupUtils.getPos(pValues, 0, mergeNum, parentLen, parallel);
            SquareZ2Vector[] sr = OneSideGroupUtils.getPos(sValues, parentLen - 1, mergeNum, parentLen, parallel);
            SquareZ2Vector[] newPl = z2cParty.xor(plainBitMuxParty.mux(leftFlagVec, z2cParty.xor(pl, extremeValues)), pl);
            SquareZ2Vector[] newSr = z2cParty.xor(plainBitMuxParty.mux(rightFlagVec, z2cParty.xor(sr, extremeValues)), sr);
            OneSideGroupUtils.setPos(pValues, newPl, 0, mergeNum, parentLen, parallel);
            OneSideGroupUtils.setPos(sValues, newSr, parentLen - 1, mergeNum, parentLen, parallel);

            logStepInfo(PtoState.PTO_STEP, level + 1, levelNum + 1, resetAndGetTime());
        }
        // 再得到最终的结果
        stopWatch.start();
        resultData = plainBitMuxParty.mux(groupFlag == null ? null : params[levelNum][0], resultData);
        SquareZ2Vector[] finalResultData = resultData;
        SquareZ2Vector[][] finalRes = attrNum == 1 ? new SquareZ2Vector[][]{resultData} : IntStream.range(0, attrNum).mapToObj(i ->
            Arrays.copyOfRange(finalResultData, i * dimLen, i * dimLen + dimLen)).toArray(SquareZ2Vector[][]::new);
        logStepInfo(PtoState.PTO_STEP, levelNum + 1, levelNum + 1, resetAndGetTime());
        return finalRes;
    }

    public static BitVector[][] getPlainBitVectorsNew(BitVector groupFlag){
        int dataNum = groupFlag.bitNum();
        BitVector gFlag = groupFlag.copy();
        // 为了放置最后一组数据没有更新，强制将最后一个group flag置为1
        gFlag.set(dataNum - 1, true);
        BitVector resF = BitVectorFactory.createZeros(dataNum);
        int levelNum = LongUtils.ceilLog2(dataNum);
        BitVector[][] flagsInEachRound = new BitVector[levelNum + 1][];
        for (int level = 0; level < levelNum; level++) {
//            LOGGER.info("level:{}", level);
//            LOGGER.info("gFlag before:{}", gFlag);
            int childGroupLen = 1 << level;
            int parentLen = childGroupLen << 1;
            int mergeNum = dataNum / parentLen;
            mergeNum += dataNum % parentLen > childGroupLen ? 1 : 0;
            BitVector fIntoMux;
            BitVector leftF = gFlag.getPointsWithFixedSpace(0, mergeNum, parentLen);
            BitVector rightF = gFlag.getPointsWithFixedSpace(childGroupLen, mergeNum, parentLen);
            BitVector a = leftF.and(rightF);
            // 更新结果
            resF.setPointsWithFixedSpace(a, childGroupLen - 1, mergeNum, parentLen);
            gFlag.setPointsWithFixedSpace(leftF.or(rightF), 0, mergeNum, parentLen);
            // 3.1.1 计算得到fv的flag
            if(mergeNum == 1){
                fIntoMux = rightF.copy();
            }else{
                if((mergeNum & 1) == 0 || dataNum % parentLen > childGroupLen){
                    // 当前merge得到的最后一个节点应该是右节点
                    fIntoMux = leftF.copy();
                    fIntoMux.setPointsWithFixedSpace(rightF.getPointsWithFixedSpace(0, mergeNum / 2, 2), 0, mergeNum / 2, 2);
                }else{
                    fIntoMux = rightF.copy();
                    fIntoMux.setPointsWithFixedSpace(leftF.getPointsWithFixedSpace(1, mergeNum / 2, 2), 1, mergeNum / 2, 2);
                }
            }
            flagsInEachRound[level] = new BitVector[]{a, fIntoMux};
//            LOGGER.info("a:{}", a);
//            LOGGER.info("fIntoMux:{}", fIntoMux);
//            LOGGER.info("gFlag after:{}", gFlag);
        }
        if(groupFlag.get(dataNum - 1)){
            resF.set(dataNum - 1, true);
        }
        flagsInEachRound[levelNum] = new BitVector[]{resF};
        return flagsInEachRound;
    }

    @Override
    public SquareZ2Vector[][] groupAggNew(SquareZ2Vector[][] xiArrays, SquareZ2Vector[] validFlags,
                                       AggTypes[] aggTypes, BitVector groupFlag) throws MpcAbortException {
        setInputs(xiArrays, validFlags, aggTypes, groupFlag);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 1. 先计算真实的值
        BitVector[][] zerosAndOnes = new BitVector[2][];
        zerosAndOnes[0] = IntStream.range(0, dimLen).mapToObj(i ->
                BitVectorFactory.createZeros(dataNum)).toArray(BitVector[]::new);
        zerosAndOnes[1] = IntStream.range(0, dimLen).mapToObj(i ->
                BitVectorFactory.createOnes(dataNum)).toArray(BitVector[]::new);
        BitVector[] perpValue = IntStream.range(0, xiArrays.length).mapToObj(i ->
                aggTypes[i].equals(AggTypes.MAX) ? zerosAndOnes[0] : zerosAndOnes[1])
            .flatMap(Arrays::stream).toArray(BitVector[]::new);
        if(validFlags != null){
            SquareZ2Vector[] validFs = new SquareZ2Vector[dimLen * xiArrays.length];
            for(int i = 0, startPos = 0; i < xiArrays.length; i++, startPos += dimLen){
                for(int j = startPos; j < startPos + dimLen; j++){
                    validFs[j] = validFlags[i];
                }
            }
            SquareZ2Vector[] perpShare = (SquareZ2Vector[]) z2cParty.setPublicValues(perpValue);
            SquareZ2Vector[] originValues = Arrays.stream(xiArrays).flatMap(Arrays::stream).toArray(SquareZ2Vector[]::new);
            inputDataProcessed = z2cParty.xor(z2cParty.and(validFs, z2cParty.xor(perpShare, originValues)), perpShare);
        }else{
            inputDataProcessed = Arrays.stream(xiArrays).flatMap(Arrays::stream).toArray(SquareZ2Vector[]::new);
        }
//        if(groupFlag != null){
//            BitVector[] inputDataProcessedPlain = z2cParty.revealOwn(inputDataProcessed);
//            LOGGER.info("before all");
//            for(int i = 1; i < xiArrays.length; i++){
//                LOGGER.info("inputDataProcessedPlain[{}]:{}", i,  Arrays.toString(trans(Arrays.copyOfRange(inputDataProcessedPlain, i * dimLen, i * dimLen + dimLen))));
//            }
//        }else{
//            z2cParty.revealOther(inputDataProcessed);
//        }
        // 1.1 将result data置为 0
        resultData = IntStream.range(0, perpValue.length).mapToObj(i ->
            SquareZ2Vector.createZeros(dataNum, false)).toArray(SquareZ2Vector[]::new);
        // 2. 计算p值和s值
        SquareZ2Vector[] fe = null;
        if(groupFlag != null){
            BitVector[] fePlain = Arrays.stream(perpValue).map(x -> x.and(groupFlag)).toArray(BitVector[]::new);
            fe = z2cParty.shareOwn(fePlain);
        }else{
            fe = z2cParty.shareOther(IntStream.range(0, perpValue.length).map(i -> dataNum).toArray());
        }
        int rightChildNum = dataNum / 2 + (dataNum % 2);
        // 2.1 初始化 p = fe, s = v
        pValues = Arrays.copyOf(fe, fe.length);
        sValues = Arrays.copyOf(inputDataProcessed, inputDataProcessed.length);
        // 2.2 更新右节点的值为：p = v - fv + fe, s = fv
        SquareZ2Vector[] rightV = OneSideGroupUtils.getPos(inputDataProcessed, 1, rightChildNum, 2, parallel);
        SquareZ2Vector[] rightFe = OneSideGroupUtils.getPos(fe, 1, rightChildNum, 2, parallel);
        BitVector rightF = null;
        if(groupFlag != null){
            rightF = groupFlag.getPointsWithFixedSpace(1, rightChildNum, 2);
        }
        SquareZ2Vector[] rightFv = plainBitMuxParty.mux(rightF, rightV);
        OneSideGroupUtils.setPos(pValues, z2cParty.xor(rightV, z2cParty.xor(rightFv, rightFe)), 1, rightChildNum, 2, parallel);
        OneSideGroupUtils.setPos(sValues, rightFv, 1, rightChildNum, 2, parallel);

        int levelNum = LongUtils.ceilLog2(dataNum);
        logStepInfo(PtoState.PTO_STEP, 0, levelNum + 1, resetAndGetTime(), "init end");
        // 3. 循环进行更新
        return commonIterNew(aggTypes, groupFlag, xiArrays.length);
    }

    public SquareZ2Vector[][] commonIterNew(AggTypes[] aggTypes, BitVector groupFlag, int attrNum) throws MpcAbortException {
        BitVector[][] params = groupFlag != null ? getPlainBitVectorsNew(groupFlag) : null;
//        if(groupFlag != null){
//            BitVector[] pPlain = z2cParty.revealOwn(pValues);
//            BitVector[] sPlain = z2cParty.revealOwn(sValues);
//            BitVector[] resultPlain = z2cParty.revealOwn(resultData);
//            LOGGER.info("before all");
//            for(int i = 1; i < attrNum; i++){
//                LOGGER.info("pPlain[{}]:{}", i,  Arrays.toString(trans(Arrays.copyOfRange(pPlain, i * dimLen, i * dimLen + dimLen))));
//                LOGGER.info("sPlain[{}]:{}", i,  Arrays.toString(trans(Arrays.copyOfRange(sPlain, i * dimLen, i * dimLen + dimLen))));
//                LOGGER.info("resultPlain[{}]:{}", i,  Arrays.toString(trans(Arrays.copyOfRange(resultPlain, i * dimLen, i * dimLen + dimLen))));
//            }
//        }else{
//            z2cParty.revealOther(pValues);
//            z2cParty.revealOther(sValues);
//            z2cParty.revealOther(resultData);
//        }

        int levelNum = LongUtils.ceilLog2(dataNum);
        for (int level = 0; level < levelNum; level++) {
            stopWatch.start();
            int childGroupLen = 1 << level;
            int parentLen = childGroupLen << 1;
            int mergeNum = dataNum / parentLen;
            mergeNum += dataNum % parentLen > childGroupLen ? 1 : 0;
            // 3.1 计算各个flag
            BitVector a = groupFlag == null ? null : params[level][0],
                fIntoMux = groupFlag == null ? null : params[level][1];
            // 3.2 计算合并值
            SquareZ2Vector[] sl = OneSideGroupUtils.getPos(sValues, childGroupLen - 1, mergeNum, parentLen, parallel);
            SquareZ2Vector[] pr = OneSideGroupUtils.getPos(pValues, childGroupLen, mergeNum, parentLen, parallel);
            // 3.2.1 得到比较结果 v = OP(sl, pr)
            SquareZ2Vector[][] inputSl = IntStream.range(0, attrNum).mapToObj(i ->
                Arrays.copyOfRange(sl, i * dimLen, i * dimLen + dimLen)).toArray(SquareZ2Vector[][]::new);
            SquareZ2Vector[][] inputPr = IntStream.range(0, attrNum).mapToObj(i ->
                Arrays.copyOfRange(pr, i * dimLen, i * dimLen + dimLen)).toArray(SquareZ2Vector[][]::new);
            MpcZ2Vector[] leqRes = z2IntegerCircuit.leqParallel(inputSl, inputPr);
            // 得到op结果
            SquareZ2Vector[] leqFlagExtend = new SquareZ2Vector[attrNum * dimLen];
            for(int i = 0; i < attrNum; i++){
                z2cParty.xori(leqRes[i], aggTypes[i].equals(AggTypes.MAX) ? SquareZ2Vector.createZeros(mergeNum) : SquareZ2Vector.createOnes(mergeNum));
                Arrays.fill(leqFlagExtend, i * dimLen, i * dimLen + dimLen, leqRes[i]);
            }
            SquareZ2Vector[] v = (SquareZ2Vector[]) z2cParty.mux(sl, pr, leqFlagExtend);
            SquareZ2Vector[] vPrime = plainBitMuxParty.mux(a, v);
            // 3.3 更新left child的最后一个值
            OneSideGroupUtils.setPos(resultData, vPrime, childGroupLen - 1, mergeNum, parentLen, parallel);
            if(mergeNum == 1){
                // 3.4 先计算出fv
                SquareZ2Vector[] fv = plainBitMuxParty.mux(fIntoMux, v);
                // 3.5 如果只更新一个，那么说明自己已经是左节点了，这时候更新p是没有意义的
                SquareZ2Vector[] sr = OneSideGroupUtils.getPos(sValues, parentLen - 1, mergeNum, parentLen, parallel);
                SquareZ2Vector[] leftV = OneSideGroupUtils.getPos(v, 0, 1, 2, parallel);
                for(int i = 0; i < sr.length; i++){
                    z2cParty.xori(sr[i], fv[i]);
                    z2cParty.xori(sr[i], leftV[i]);
                }
                OneSideGroupUtils.setPos(sValues, sr, parentLen - 1, mergeNum, parentLen, parallel);
            }else{
                // 3.4 先计算出fv
                SquareZ2Vector[] fv = plainBitMuxParty.mux(fIntoMux, v);
                // 3.5 更新p和s
                SquareZ2Vector[] pl = OneSideGroupUtils.getPos(pValues, 0, mergeNum, parentLen, parallel);
                SquareZ2Vector[] sr = OneSideGroupUtils.getPos(sValues, parentLen - 1, mergeNum, parentLen, parallel);

                SquareZ2Vector[] leftVRightVPrime, leftVPrimeRightV;
                if((mergeNum & 1) == 0 || dataNum % parentLen > childGroupLen){
                    // 最后一个节点是右节点
                    SquareZ2Vector[] leftV = OneSideGroupUtils.getPos(v, 0, mergeNum / 2, 2, parallel);
                    SquareZ2Vector[] leftVPrime = OneSideGroupUtils.getPos(vPrime, 0, mergeNum / 2, 2, parallel);
                    leftVRightVPrime = Arrays.copyOf(vPrime, vPrime.length);
                    leftVPrimeRightV = Arrays.copyOf(v, v.length);
                    OneSideGroupUtils.setPos(leftVRightVPrime, leftV, 0, mergeNum / 2, 2, parallel);
                    OneSideGroupUtils.setPos(leftVPrimeRightV, leftVPrime, 0, mergeNum / 2, 2, parallel);
                }else{
                    SquareZ2Vector[] rightV = OneSideGroupUtils.getPos(v, 1, mergeNum / 2, 2, parallel);
                    SquareZ2Vector[] rightVPrime = OneSideGroupUtils.getPos(vPrime, 1, mergeNum / 2, 2, parallel);
                    leftVRightVPrime = Arrays.copyOf(v, v.length);
                    leftVPrimeRightV = Arrays.copyOf(vPrime, vPrime.length);
                    OneSideGroupUtils.setPos(leftVRightVPrime, rightVPrime, 1, mergeNum / 2, 2, parallel);
                    OneSideGroupUtils.setPos(leftVPrimeRightV, rightV, 1, mergeNum / 2, 2, parallel);
                }
                for(int i = 0; i < pl.length; i++){
                    z2cParty.xori(pl[i], fv[i]);
                    z2cParty.xori(sr[i], fv[i]);
                    z2cParty.xori(pl[i], leftVPrimeRightV[i]);
                    z2cParty.xori(sr[i], leftVRightVPrime[i]);
                }
                OneSideGroupUtils.setPos(pValues, pl, 0, mergeNum, parentLen, parallel);
                OneSideGroupUtils.setPos(sValues, sr, parentLen - 1, mergeNum, parentLen, parallel);
            }
            logStepInfo(PtoState.PTO_STEP, level + 1, levelNum + 1, resetAndGetTime());

//            if(groupFlag != null){
//                BitVector[] pPlain = z2cParty.revealOwn(pValues);
//                BitVector[] sPlain = z2cParty.revealOwn(sValues);
//                BitVector[] resultPlain = z2cParty.revealOwn(resultData);
//                LOGGER.info("after level:{}", level);
//                LOGGER.info("origin group flag:{}", groupFlag);
//                LOGGER.info("a:{}", a);
//                LOGGER.info("fIntoMux:{}", fIntoMux);
//                for(int i = 1; i < attrNum; i++){
//                    LOGGER.info("pPlain[{}]:{}", i,  Arrays.toString(trans(Arrays.copyOfRange(pPlain, i * dimLen, i * dimLen + dimLen))));
//                    LOGGER.info("sPlain[{}]:{}", i,  Arrays.toString(trans(Arrays.copyOfRange(sPlain, i * dimLen, i * dimLen + dimLen))));
//                    LOGGER.info("resultPlain[{}]:{}", i,  Arrays.toString(trans(Arrays.copyOfRange(resultPlain, i * dimLen, i * dimLen + dimLen))));
//                }
//            }else{
//                z2cParty.revealOther(pValues);
//                z2cParty.revealOther(sValues);
//                z2cParty.revealOther(resultData);
//            }

        }
        // 如果最后两个flag都是1的话，那么最后一个数据也要更新下，直接设为自己的值
        SquareZ2Vector[] lastOneOrigin = OneSideGroupUtils.getPos(inputDataProcessed, dataNum - 1, 1, 1, parallel);
        SquareZ2Vector[] lastOneNew = plainBitMuxParty.mux(groupFlag == null ? null :
            (groupFlag.get(dataNum - 1) ? BitVectorFactory.createOnes(1) : BitVectorFactory.createZeros(1)), lastOneOrigin);
        OneSideGroupUtils.setPos(resultData, lastOneNew, dataNum - 1, 1, 1, parallel);
        // 再得到最终的结果
        stopWatch.start();
        resultData = plainBitMuxParty.mux(groupFlag == null ? null : params[levelNum][0], resultData);
        SquareZ2Vector[] finalResultData = resultData;
        SquareZ2Vector[][] finalRes = attrNum == 1 ? new SquareZ2Vector[][]{resultData} : IntStream.range(0, attrNum).mapToObj(i ->
            Arrays.copyOfRange(finalResultData, i * dimLen, i * dimLen + dimLen)).toArray(SquareZ2Vector[][]::new);
        logStepInfo(PtoState.PTO_STEP, levelNum + 1, levelNum + 1, resetAndGetTime());
        return finalRes;
    }

    public BigInteger[] trans(BitVector[] da) {
        return ZlDatabase.create(envType, parallel, da).getBigIntegerData();
    }
}
