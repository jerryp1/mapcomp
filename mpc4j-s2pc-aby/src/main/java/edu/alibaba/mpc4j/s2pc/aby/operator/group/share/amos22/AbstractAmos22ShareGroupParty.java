package edu.alibaba.mpc4j.s2pc.aby.operator.group.share.amos22;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.GroupTypes.AggTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.GroupTypes.GroupPartyTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.GroupUtils;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.share.AbstractShareGroupParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.share.ShareGroupParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxParty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

public abstract class AbstractAmos22ShareGroupParty extends AbstractShareGroupParty implements ShareGroupParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAmos22ShareGroupParty.class);
    private final int maxBitLenOneBatch;
    private final Z2cParty z2cParty;
    private final Z2MuxParty z2MuxParty;
    private final Z2IntegerCircuit z2IntegerCircuit;
    private SquareZ2Vector[] pValues;
    private SquareZ2Vector[] sValues;
    private SquareZ2Vector[] resultData;
    private SquareZ2Vector[] inputDataProcessed;
    private SquareZ2Vector[][] params;

    protected AbstractAmos22ShareGroupParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, Amos22ShareGroupConfig config, GroupPartyTypes partyTypes) {
        super(ptoDesc, rpc, otherParty, config);
        if (partyTypes.equals(GroupPartyTypes.RECEIVER)) {
            z2cParty = Z2cFactory.createReceiver(rpc, otherParty, config.getZ2cConfig());
            z2MuxParty = Z2MuxFactory.createReceiver(rpc, otherParty, config.getZ2MuxConfig());
        } else {
            z2cParty = Z2cFactory.createSender(rpc, otherParty, config.getZ2cConfig());
            z2MuxParty = Z2MuxFactory.createSender(rpc, otherParty, config.getZ2MuxConfig());
        }
        z2IntegerCircuit = new Z2IntegerCircuit(z2cParty);
        maxBitLenOneBatch = config.getMaxBitLenOneBatch();
        addMultipleSubPtos(z2cParty, z2MuxParty);
    }

    @Override
    public void init(int attrNum, int maxNum, int maxBitNum) throws MpcAbortException {
        setInitInput(attrNum, maxNum, maxBitNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int initSize = attrNum * maxBitNum * maxNum * 4;
        initSize = initSize <= 0 ? Integer.MAX_VALUE : initSize;
        z2cParty.init(initSize);
        z2MuxParty.init(initSize);

        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector getFlag(SquareZ2Vector groupFlag) throws MpcAbortException {
        if(params == null){
            getShareBitVectors(groupFlag);
        }
        return params[params.length - 1][0];
    }

    public void getShareBitVectors(SquareZ2Vector groupFlag) throws MpcAbortException {
        int dataNum = groupFlag.bitNum();
        SquareZ2Vector gFlag = groupFlag.copy();
        // 为了放置最后一组数据没有更新，强制将最后一个group flag置为1
        SquareZ2Vector oneShare = (SquareZ2Vector) z2cParty.setPublicValues(new BitVector[]{BitVectorFactory.createOnes(1)})[0];
        gFlag.getBitVector().set(dataNum - 1, oneShare.getBitVector().get(0));
        SquareZ2Vector resF = SquareZ2Vector.createZeros(dataNum, false);
        int levelNum = LongUtils.ceilLog2(dataNum);
        params = new SquareZ2Vector[levelNum + 1][];
        for (int level = 0; level < levelNum; level++) {
            int childGroupLen = 1 << level;
            int parentLen = childGroupLen << 1;
            int mergeNum = dataNum / parentLen;
            mergeNum += dataNum % parentLen > childGroupLen ? 1 : 0;
            SquareZ2Vector fIntoMux;
            SquareZ2Vector leftF = gFlag.getPointsWithFixedSpace(0, mergeNum, parentLen);
            SquareZ2Vector rightF = gFlag.getPointsWithFixedSpace(childGroupLen, mergeNum, parentLen);
            SquareZ2Vector f = z2cParty.or(leftF, rightF);
            SquareZ2Vector a = z2cParty.xor(leftF, rightF);
            z2cParty.xori(a, f);
            // 更新结果
            resF.setPointsWithFixedSpace(a, childGroupLen - 1, mergeNum, parentLen);
            gFlag.setPointsWithFixedSpace(f, 0, mergeNum, parentLen);
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
            params[level] = new SquareZ2Vector[]{a, fIntoMux};
        }
        resF.getBitVector().set(dataNum - 1, groupFlag.getBitVector().get(dataNum - 1));
        params[levelNum] = new SquareZ2Vector[]{resF};
    }

//    @Override
//    public SquareZ2Vector[][] groupAgg(SquareZ2Vector[][] xiArrays, SquareZ2Vector[] validFlags,
//                                       AggTypes[] aggTypes, SquareZ2Vector groupFlag) throws MpcAbortException {
//        setInputs(xiArrays, validFlags, aggTypes, groupFlag);
//        logPhaseInfo(PtoState.PTO_BEGIN);
//
//        getShareBitVectors(groupFlag);
//
//        stopWatch.start();
//        // 1. 先计算真实的值
//        BitVector[][] zerosAndOnes = new BitVector[2][];
//        zerosAndOnes[0] = IntStream.range(0, dimLen).mapToObj(i ->
//            BitVectorFactory.createZeros(dataNum)).toArray(BitVector[]::new);
//        zerosAndOnes[1] = IntStream.range(0, dimLen).mapToObj(i ->
//            BitVectorFactory.createOnes(dataNum)).toArray(BitVector[]::new);
//
//        BitVector[] perpValue = IntStream.range(0, xiArrays.length).mapToObj(i ->
//                aggTypes[i].equals(AggTypes.MAX) ? zerosAndOnes[0] : zerosAndOnes[1])
//            .flatMap(Arrays::stream).toArray(BitVector[]::new);
//        SquareZ2Vector[] perpShare = (SquareZ2Vector[]) z2cParty.setPublicValues(perpValue);
//
//        if(validFlags != null){
//            SquareZ2Vector[] validFs = new SquareZ2Vector[dimLen * xiArrays.length];
//            for(int i = 0, startPos = 0; i < xiArrays.length; i++, startPos += dimLen){
//                for(int j = startPos; j < startPos + dimLen; j++){
//                    validFs[j] = validFlags[i];
//                }
//            }
//
//            SquareZ2Vector[][] perpMatrix = IntStream.range(0, xiArrays.length).mapToObj(i ->
//                Arrays.copyOfRange(perpShare, i * dimLen, i * dimLen + dimLen)).toArray(SquareZ2Vector[][]::new);
//            SquareZ2Vector[][] xorRes =  IntStream.range(0, xiArrays.length).mapToObj(i -> {
//                try {
//                    return z2cParty.xor(perpMatrix[i], xiArrays[i]);
//                } catch (MpcAbortException e) {
//                    throw new RuntimeException(e);
//                }
//            }).toArray(SquareZ2Vector[][]::new);
//            SquareZ2Vector[][] tmp = z2MuxParty.mux(validFlags, xorRes);
//            for(int i = 0; i < tmp.length; i++){
//                for(int j = 0; j < tmp[i].length; j++){
//                    z2cParty.xori(tmp[i][j], perpMatrix[i][j]);
//                }
//            }
//            inputDataProcessed = Arrays.stream(tmp).flatMap(Arrays::stream).toArray(SquareZ2Vector[]::new);
////            SquareZ2Vector[] originValues = Arrays.stream(xiArrays).flatMap(Arrays::stream).toArray(SquareZ2Vector[]::new);
////            inputDataProcessed = z2cParty.xor(z2cParty.and(validFs, z2cParty.xor(perpShare, originValues)), perpShare);
//        }else{
//            inputDataProcessed = Arrays.stream(xiArrays).flatMap(Arrays::stream).toArray(SquareZ2Vector[]::new);
//        }
////        if(groupFlag != null){
////            BitVector[] inputDataProcessedPlain = z2cParty.revealOwn(inputDataProcessed);
////            LOGGER.info("before all");
////            for(int i = 1; i < xiArrays.length; i++){
////                LOGGER.info("inputDataProcessedPlain[{}]:{}", i,  Arrays.toString(trans(Arrays.copyOfRange(inputDataProcessedPlain, i * dimLen, i * dimLen + dimLen))));
////            }
////        }else{
////            z2cParty.revealOther(inputDataProcessed);
////        }
//        // 1.1 将result data置为 0
//        resultData = IntStream.range(0, perpValue.length).mapToObj(i ->
//            SquareZ2Vector.createZeros(dataNum, false)).toArray(SquareZ2Vector[]::new);
//        // 2. 计算p值和s值
//        SquareZ2Vector[] fe = z2MuxParty.mux(groupFlag, perpShare);
////        SquareZ2Vector[] fe = z2cParty.and(GroupUtils.extendData(groupFlag, perpShare.length), perpShare);
//        int rightChildNum = dataNum / 2 + (dataNum % 2);
//        // 2.1 初始化 p = fe, s = v
//        pValues = Arrays.copyOf(fe, fe.length);
//        sValues = Arrays.copyOf(inputDataProcessed, inputDataProcessed.length);
//        // 2.2 更新右节点的值为：p = v - fv + fe, s = fv
//        SquareZ2Vector[] rightV = GroupUtils.getPos(inputDataProcessed, 1, rightChildNum, 2, parallel);
//        SquareZ2Vector[] rightFe = GroupUtils.getPos(fe, 1, rightChildNum, 2, parallel);
//        SquareZ2Vector rightF = groupFlag.getPointsWithFixedSpace(1, rightChildNum, 2);
//        SquareZ2Vector[] rightFv = z2MuxParty.mux(rightF, rightV);
////        SquareZ2Vector[] rightFv = z2cParty.and(GroupUtils.extendData(rightF, rightV.length), rightV);
//        GroupUtils.setPos(pValues, z2cParty.xor(rightV, z2cParty.xor(rightFv, rightFe)), 1, rightChildNum, 2, parallel);
//        GroupUtils.setPos(sValues, rightFv, 1, rightChildNum, 2, parallel);
//
//        int levelNum = LongUtils.ceilLog2(dataNum);
//        logStepInfo(PtoState.PTO_STEP, 0, levelNum + 1, resetAndGetTime(), "init end");
//        // 3. 循环进行更新
//        return commonIter(aggTypes, groupFlag, xiArrays.length);
//    }

    @Override
    public SquareZ2Vector[][] groupAgg(SquareZ2Vector[][] xiArrays, SquareZ2Vector[] validFlags,
                                       AggTypes[] aggTypes, SquareZ2Vector groupFlag) throws MpcAbortException {
        setInputs(xiArrays, validFlags, aggTypes, groupFlag);
        logPhaseInfo(PtoState.PTO_BEGIN);
        getShareBitVectors(groupFlag);
        int eachOneBitLen = xiArrays[0].length * xiArrays[0][0].bitNum();
        int numInEachBatch = Math.max(Math.min(maxBitLenOneBatch / eachOneBitLen, xiArrays.length), 1);
        int batchNum = xiArrays.length / numInEachBatch + (xiArrays.length % numInEachBatch > 0 ? 1 : 0);
        SquareZ2Vector[][] res = new SquareZ2Vector[xiArrays.length][];
        for (int i = 0, copyStartIndex = 0; i < batchNum; i++, copyStartIndex += numInEachBatch) {
            int copyEndIndex = Math.min(copyStartIndex + numInEachBatch, xiArrays.length);
            LOGGER.info("processing one side group op in batch: {} / {}, each batch has {} parallel group", i, batchNum, copyEndIndex - copyStartIndex);
            SquareZ2Vector[][] tmp = groupAggBatch(
                Arrays.copyOfRange(xiArrays, copyStartIndex, copyEndIndex),
                Arrays.copyOfRange(validFlags, copyStartIndex, copyEndIndex),
                Arrays.copyOfRange(aggTypes, copyStartIndex, copyEndIndex), groupFlag);
            System.arraycopy(tmp, 0, res, copyStartIndex, tmp.length);
        }
        logPhaseInfo(PtoState.PTO_END);
        return res;
    }

    private SquareZ2Vector[][] groupAggBatch(SquareZ2Vector[][] xiArrays, SquareZ2Vector[] validFlags,
                                             AggTypes[] aggTypes, SquareZ2Vector groupFlag) throws MpcAbortException {
        stopWatch.start();
        // 1. 先计算真实的值
        BitVector[][] zerosAndOnes = new BitVector[2][dimLen];
        Arrays.fill(zerosAndOnes[0], BitVectorFactory.createZeros(dataNum));
        Arrays.fill(zerosAndOnes[1], BitVectorFactory.createOnes(dataNum));

        BitVector[] perpValue = IntStream.range(0, xiArrays.length).mapToObj(i ->
                aggTypes[i].equals(AggTypes.MAX) ? zerosAndOnes[0] : zerosAndOnes[1])
            .flatMap(Arrays::stream).toArray(BitVector[]::new);
        SquareZ2Vector[] perpShare = (SquareZ2Vector[]) z2cParty.setPublicValues(perpValue);

        if(validFlags != null){
            SquareZ2Vector[] validFs = new SquareZ2Vector[dimLen * xiArrays.length];
            for(int i = 0, startPos = 0; i < xiArrays.length; i++, startPos += dimLen){
                for(int j = startPos; j < startPos + dimLen; j++){
                    validFs[j] = validFlags[i];
                }
            }
            SquareZ2Vector[][] perpMatrix = IntStream.range(0, xiArrays.length).mapToObj(i ->
                Arrays.copyOfRange(perpShare, i * dimLen, i * dimLen + dimLen)).toArray(SquareZ2Vector[][]::new);
            SquareZ2Vector[][] xorRes =  IntStream.range(0, xiArrays.length).mapToObj(i -> {
                try {
                    return z2cParty.xor(perpMatrix[i], xiArrays[i]);
                } catch (MpcAbortException e) {
                    throw new RuntimeException(e);
                }
            }).toArray(SquareZ2Vector[][]::new);
            SquareZ2Vector[][] tmp = z2MuxParty.mux(validFlags, xorRes);
            for(int i = 0; i < tmp.length; i++){
                for(int j = 0; j < tmp[i].length; j++){
                    z2cParty.xori(tmp[i][j], perpMatrix[i][j]);
                }
            }
            inputDataProcessed = Arrays.stream(tmp).flatMap(Arrays::stream).toArray(SquareZ2Vector[]::new);
        }else{
            inputDataProcessed = Arrays.stream(xiArrays).flatMap(Arrays::stream).toArray(SquareZ2Vector[]::new);
        }
        // 1.1 将result data置为 0
        resultData = IntStream.range(0, perpValue.length).mapToObj(i ->
            SquareZ2Vector.createZeros(dataNum, false)).toArray(SquareZ2Vector[]::new);
        // 2. 计算p值和s值
        SquareZ2Vector[] fe = z2MuxParty.mux(groupFlag, perpShare);
        int rightChildNum = dataNum / 2 + (dataNum % 2);
        // 2.1 初始化 p = fe, s = v
        pValues = Arrays.copyOf(fe, fe.length);
        sValues = Arrays.copyOf(inputDataProcessed, inputDataProcessed.length);
        // 2.2 更新右节点的值为：p = v - fv + fe, s = fv
        SquareZ2Vector[] rightV = GroupUtils.getPos(inputDataProcessed, 1, rightChildNum, 2, parallel);
        SquareZ2Vector[] rightFe = GroupUtils.getPos(fe, 1, rightChildNum, 2, parallel);
        SquareZ2Vector rightF = groupFlag.getPointsWithFixedSpace(1, rightChildNum, 2);
        SquareZ2Vector[] rightFv = z2MuxParty.mux(rightF, rightV);
        GroupUtils.setPos(pValues, z2cParty.xor(rightV, z2cParty.xor(rightFv, rightFe)), 1, rightChildNum, 2, parallel);
        GroupUtils.setPos(sValues, rightFv, 1, rightChildNum, 2, parallel);

        int levelNum = LongUtils.ceilLog2(dataNum);
        logStepInfo(PtoState.PTO_STEP, 0, levelNum + 1, resetAndGetTime(), "init end");
        // 3. 循环进行更新
        return commonIter(aggTypes, groupFlag, xiArrays.length);
    }

    private SquareZ2Vector[][] commonIter(AggTypes[] aggTypes, SquareZ2Vector groupFlag, int attrNum) throws MpcAbortException {
        stopWatch.start();
        logStepInfo(PtoState.PTO_STEP, 0, 111, resetAndGetTime(), "compute Flag");
//        if(groupFlag != null){
//            BitVector[] pPlain = z2cParty.revealOwn(pValues);
//            BitVector[] sPlain = z2cParty.revealOwn(sValues);
//            BitVector[] resultPlain = z2cParty.revealOwn(resultData);
//            LOGGER.info("before all");
//            for(int i = 0; i < attrNum; i++){
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
            SquareZ2Vector a = params[level][0], fIntoMux =  params[level][1];
            // 3.2 计算合并值
            SquareZ2Vector[] sl = GroupUtils.getPos(sValues, childGroupLen - 1, mergeNum, parentLen, parallel);
            SquareZ2Vector[] pr = GroupUtils.getPos(pValues, childGroupLen, mergeNum, parentLen, parallel);
            // 3.2.1 得到比较结果 v = OP(sl, pr)
            SquareZ2Vector[][] inputSl = IntStream.range(0, attrNum).mapToObj(i ->
                Arrays.copyOfRange(sl, i * dimLen, i * dimLen + dimLen)).toArray(SquareZ2Vector[][]::new);
            SquareZ2Vector[][] inputPr = IntStream.range(0, attrNum).mapToObj(i ->
                Arrays.copyOfRange(pr, i * dimLen, i * dimLen + dimLen)).toArray(SquareZ2Vector[][]::new);
            MpcZ2Vector[] leqRes = z2IntegerCircuit.leq(inputSl, inputPr);

            // 得到op结果
            SquareZ2Vector[] leqFlagExtend = new SquareZ2Vector[attrNum * dimLen];
            for(int i = 0; i < attrNum; i++){
                z2cParty.xori(leqRes[i], aggTypes[i].equals(AggTypes.MAX) ? SquareZ2Vector.createZeros(mergeNum) : SquareZ2Vector.createOnes(mergeNum));
                Arrays.fill(leqFlagExtend, i * dimLen, i * dimLen + dimLen, leqRes[i]);
            }
            SquareZ2Vector[] v = (SquareZ2Vector[]) z2cParty.mux(sl, pr, leqFlagExtend);
            SquareZ2Vector[] vPrime = z2MuxParty.mux(a, v);
//            SquareZ2Vector[] vPrime = z2cParty.and(GroupUtils.extendData(a, v.length), v);
            // 3.3 更新left child的最后一个值
            GroupUtils.setPos(resultData, vPrime, childGroupLen - 1, mergeNum, parentLen, parallel);
            // 3.4 先计算出fv
            SquareZ2Vector[] fv = z2MuxParty.mux(fIntoMux, v);
//            SquareZ2Vector[] fv = z2cParty.and(GroupUtils.extendData(fIntoMux, v.length), v);
            if(mergeNum == 1){
                // 3.5 如果只更新一个，那么说明自己已经是左节点了，这时候更新p是没有意义的
                SquareZ2Vector[] sr = GroupUtils.getPos(sValues, parentLen - 1, mergeNum, parentLen, parallel);
                SquareZ2Vector[] leftV = GroupUtils.getPos(v, 0, 1, 2, parallel);
                for(int i = 0; i < sr.length; i++){
                    z2cParty.xori(sr[i], fv[i]);
                    z2cParty.xori(sr[i], leftV[i]);
                }
                GroupUtils.setPos(sValues, sr, parentLen - 1, mergeNum, parentLen, parallel);
            }else{
                // 3.5 更新p和s
                SquareZ2Vector[] pl = GroupUtils.getPos(pValues, 0, mergeNum, parentLen, parallel);
                SquareZ2Vector[] sr = GroupUtils.getPos(sValues, parentLen - 1, mergeNum, parentLen, parallel);

                SquareZ2Vector[] leftVRightVPrime, leftVPrimeRightV;
                if((mergeNum & 1) == 0 || dataNum % parentLen > childGroupLen){
                    // 最后一个节点是右节点
                    SquareZ2Vector[] leftV = GroupUtils.getPos(v, 0, mergeNum / 2, 2, parallel);
                    SquareZ2Vector[] leftVPrime = GroupUtils.getPos(vPrime, 0, mergeNum / 2, 2, parallel);
                    leftVRightVPrime = Arrays.copyOf(vPrime, vPrime.length);
                    leftVPrimeRightV = Arrays.copyOf(v, v.length);
                    GroupUtils.setPos(leftVRightVPrime, leftV, 0, mergeNum / 2, 2, parallel);
                    GroupUtils.setPos(leftVPrimeRightV, leftVPrime, 0, mergeNum / 2, 2, parallel);
                }else{
                    SquareZ2Vector[] rightV = GroupUtils.getPos(v, 1, mergeNum / 2, 2, parallel);
                    SquareZ2Vector[] rightVPrime = GroupUtils.getPos(vPrime, 1, mergeNum / 2, 2, parallel);
                    leftVRightVPrime = Arrays.copyOf(v, v.length);
                    leftVPrimeRightV = Arrays.copyOf(vPrime, vPrime.length);
                    GroupUtils.setPos(leftVRightVPrime, rightVPrime, 1, mergeNum / 2, 2, parallel);
                    GroupUtils.setPos(leftVPrimeRightV, rightV, 1, mergeNum / 2, 2, parallel);
                }
                for(int i = 0; i < pl.length; i++){
                    z2cParty.xori(pl[i], fv[i]);
                    z2cParty.xori(sr[i], fv[i]);
                    z2cParty.xori(pl[i], leftVPrimeRightV[i]);
                    z2cParty.xori(sr[i], leftVRightVPrime[i]);
                }
                GroupUtils.setPos(pValues, pl, 0, mergeNum, parentLen, parallel);
                GroupUtils.setPos(sValues, sr, parentLen - 1, mergeNum, parentLen, parallel);
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
//                for(int i = 0; i < attrNum; i++){
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
        SquareZ2Vector[] lastOneOrigin = GroupUtils.getPos(inputDataProcessed, dataNum - 1, 1, 1, parallel);
        SquareZ2Vector[] lastOneNew = z2MuxParty.mux(groupFlag.getPointsWithFixedSpace(dataNum - 1, 1, 1),  lastOneOrigin);
//        SquareZ2Vector[] lastOneNew = z2cParty.and(GroupUtils.extendData(groupFlag.getPointsWithFixedSpace(dataNum - 1, 1, 1), lastOneOrigin.length),  lastOneOrigin);
        GroupUtils.setPos(resultData, lastOneNew, dataNum - 1, 1, 1, parallel);
        // 再得到最终的结果
        stopWatch.start();
        resultData = z2MuxParty.mux(params[levelNum][0], resultData);
//        resultData = z2cParty.and(GroupUtils.extendData(params[levelNum][0], resultData.length), resultData);
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
