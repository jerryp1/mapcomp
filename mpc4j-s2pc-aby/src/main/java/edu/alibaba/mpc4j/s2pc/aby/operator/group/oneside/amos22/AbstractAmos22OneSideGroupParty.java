package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
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

import java.util.Arrays;
import java.util.stream.IntStream;


public abstract class AbstractAmos22OneSideGroupParty extends AbstractOneSideGroupParty implements OneSideGroupParty {
    private final Z2cParty z2cParty;
    private final Z2IntegerCircuit z2IntegerCircuit;
    private final PlainBitMuxParty plainBitMuxParty;

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
    public void init(int maxL, int maxNum, int maxBitNum) throws MpcAbortException {

    }

    @Override
    public SquareZ2Vector[] groupAgg(SquareZ2Vector[] xiArrays, SquareZ2Vector validFlags, AggTypes aggTypes, BitVector groupFlag) throws MpcAbortException {
        setInputs(xiArrays, validFlags, aggTypes, groupFlag);
        int levelNum = LongUtils.ceilLog2(dataNum);
        // todo 先处理一遍 得到初始的sValue和pValue，对于左孩子：p=0, s=v; 对于右孩子：p = v - fv, s = fv. 如果n % 2 == 1，那么最后一个一定是右孩子 需要验证是否正确
        // todo 算法的逻辑有问题，即第一个组的结果可能永远没有放置，最后一个元素单独成组的时候也没有放置
        int leafInitHalfLen = dataNum / 2 + dataNum % 2 == 1 ? 1 : 0;
        SquareZ2Vector[] evenValues = OneSideGroupUtils.getPos(xiArrays, 1, leafInitHalfLen, 2, parallel);
        BitVector envGroupFlag = gFlag == null ? null : gFlag.getPointsWithFixedSpace(1, leafInitHalfLen, 2);
        SquareZ2Vector[] fv = plainBitMuxParty.mux(envGroupFlag, evenValues);
        SquareZ2Vector[] vXorFv = z2cParty.xor(fv, evenValues);
        OneSideGroupUtils.setPos(sValues, fv, 1, leafInitHalfLen, 2, parallel);
        OneSideGroupUtils.setPos(pValues, vXorFv, 1, leafInitHalfLen, 2, parallel);

        for (int level = 0; level < levelNum; level++) {
            int childGroupLen = 1 << level;
            int parentLen = childGroupLen << 1;
            int mergeNum = dataNum / parentLen;
            mergeNum += dataNum % parentLen > childGroupLen ? 1 : 0;
            SquareZ2Vector[] sl = OneSideGroupUtils.getPos(sValues, childGroupLen - 1, mergeNum, parentLen, parallel);
            SquareZ2Vector[] pr = OneSideGroupUtils.getPos(pValues, childGroupLen, mergeNum, parentLen, parallel);

            // 得到比较结果 v = OP(sl, pr)
            SquareZ2Vector leqRes = (SquareZ2Vector) z2IntegerCircuit.leqParallel(sl, pr);
            SquareZ2Vector aggChange = aggTypes.equals(AggTypes.MAX) ? SquareZ2Vector.createZeros(mergeNum) : SquareZ2Vector.createOnes(mergeNum);
            z2cParty.xori(leqRes, aggChange);
            SquareZ2Vector[] leqFlagExtend = new SquareZ2Vector[resultData.length];
            Arrays.fill(leqFlagExtend, leqRes);
            SquareZ2Vector[] extremeValues = (SquareZ2Vector[]) z2cParty.mux(sl, pr, leqFlagExtend);

            // a = fl·fr
            BitVector[] flAndFr = gFlag == null ? null : gFlag.getPointsWithFixedSpace(new int[]{0, parentLen - 1}, mergeNum, parentLen);
            BitVector aFlag = gFlag == null ? null : flAndFr[0].and(flAndFr[1]);
            // [𝑣′]←IfThen([𝑎],[𝑣]).
            SquareZ2Vector[] vPrimeValues = plainBitMuxParty.mux(aFlag, extremeValues);
            // 设置 resultData 的值
            OneSideGroupUtils.setPos(resultData, vPrimeValues, childGroupLen - 1, mergeNum, parentLen, parallel);

            // 选择是否要置换的两个flag
            BitVector plNoChangeFlag = null, srNoChangeFlag = null;
            if (gFlag != null) {
                // 更新gFlag，直接将最两端的flag都更新了
                assert aFlag != null;
                gFlag.setPointsWithFixedSpace(aFlag, 0, mergeNum, parentLen);
                gFlag.setPointsWithFixedSpace(aFlag, parentLen - 1, mergeNum, parentLen);
                BitVector isRightNode = OneSideGroupUtils.crossZeroAndOne(mergeNum, true);
                BitVector isLeftNode = OneSideGroupUtils.crossZeroAndOne(mergeNum, false);
                plNoChangeFlag = flAndFr[0].or(flAndFr[1].not().and(isLeftNode));
                srNoChangeFlag = flAndFr[1].or(flAndFr[0].not().and(isRightNode));
            }
            // 原始的pl和sr
            SquareZ2Vector[] pl = OneSideGroupUtils.getPos(pValues, 0, mergeNum, parentLen, parallel);
            SquareZ2Vector[] sr = OneSideGroupUtils.getPos(sValues, parentLen - 1, mergeNum, parentLen, parallel);
            SquareZ2Vector[] newPl = z2cParty.xor(plainBitMuxParty.mux(plNoChangeFlag, z2cParty.xor(pl, extremeValues)), extremeValues);
            SquareZ2Vector[] newSr = z2cParty.xor(plainBitMuxParty.mux(srNoChangeFlag, z2cParty.xor(sr, extremeValues)), extremeValues);

            OneSideGroupUtils.setPos(pValues, newPl, 0, mergeNum, parentLen, parallel);
            OneSideGroupUtils.setPos(sValues, newSr, parentLen - 1, mergeNum, parentLen, parallel);
        }

        // 处理最初始的一个和最后一个，如果最初始的一个flag是0，就将最后的p值赋过去；如果最后一个flag和倒数第二个flag都是1，就将最后的s值赋过去
        BitVector finalFlag = null;
        if(gFlag != null){
            finalFlag = BitVectorFactory.createZeros(2);
            finalFlag.set(0, groupFlag.get(0));
            finalFlag.set(1, groupFlag.get(dataNum - 2) & groupFlag.get(dataNum - 1));
        }
        SquareZ2Vector[] finalData = IntStream.range(0, resultData.length).mapToObj(i -> {
            SquareZ2Vector tmp = SquareZ2Vector.createZeros(2, false);
            tmp.setPointsWithFixedSpace(pValues[i], 0, 1, 1);
            tmp.setPointsWithFixedSpace(sValues[i], dataNum - 1, 1, 1);
            return tmp;
        }).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] choice = plainBitMuxParty.mux(finalFlag, finalData);
        IntStream.range(0, resultData.length).forEach(i -> {
            resultData[i].getBitVector().set(0, choice[i].getBitVector().get(0));
            resultData[i].getBitVector().set(dataNum - 1, choice[i].getBitVector().get(1));
        });

        // 先得到最初始的结果，
        return resultData;
    }

    @Override
    public SquareZ2Vector[][] groupAgg(SquareZ2Vector[][] xiArrays, SquareZ2Vector[] validFlags,
                                       AggTypes[] aggTypes, boolean[] groupFlag) throws MpcAbortException {
//        setInputs(xiArrays, validFlags, aggTypes, groupFlag);
//        int levelNum = LongUtils.ceilLog2(dataNum);
//        for (int level = 0; level < levelNum; level++) {
//            int mergeLen = 1 << (level + 1);
//            int mergeNum = dataNum / mergeLen;
//
//        }

        // 先得到最初始的结果，
        return new SquareZ2Vector[0][];
    }


}
