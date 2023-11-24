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

    @Override
    public int[] getResPosFlag(BitVector groupFlag) {
        BitVector[][] params = getPlainBitVectors(groupFlag);
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
            MpcZ2Vector[] leqRes = z2IntegerCircuit.leq(inputSl, inputPr);

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

    public BigInteger[] trans(BitVector[] da) {
        return ZlDatabase.create(envType, parallel, da).getBigIntegerData();
    }
}
