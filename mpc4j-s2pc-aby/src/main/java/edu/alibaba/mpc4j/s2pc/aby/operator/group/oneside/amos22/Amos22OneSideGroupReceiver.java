//package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22;
//
//import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
//import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
//import edu.alibaba.mpc4j.common.rpc.Party;
//import edu.alibaba.mpc4j.common.rpc.Rpc;
//import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
//import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
//import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
//import edu.alibaba.mpc4j.s2pc.aby.basics.z2.AbstractZ2cParty;
//import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
//import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.AbstractOneSideGroupReceiver;
//import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupConfig;
//import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory.AggTypes;
//import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupReceiver;
//import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.AbstractPlainBitMuxParty;
//import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxFactory;
//import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxParty;
//
//import java.util.Arrays;
//
//
//public class Amos22OneSideGroupReceiver extends AbstractOneSideGroupReceiver implements OneSideGroupReceiver {
//    private final AbstractZ2cParty z2cReceiver;
//    private final Z2IntegerCircuit z2IntegerCircuit;
//    private final AbstractPlainBitMuxParty plainBitMuxReceiver;
//    protected Amos22OneSideGroupReceiver(PtoDesc ptoDesc, Rpc rpc, Party otherParty, OneSideGroupConfig config) {
//        super(ptoDesc, rpc, otherParty, config);
//        z2cReceiver = null;
//        z2IntegerCircuit = new Z2IntegerCircuit(z2cReceiver);
//        plainBitMuxReceiver = PlainBitMuxFactory.createReceiver();
//    }
//
//    @Override
//    public void init(int maxL, int maxNum, int maxBitNum) throws MpcAbortException {
//
//    }
//
//    @Override
//    public boolean[] getResPosFlag(boolean[] groupFlag) {
//        int skipNum = (1 << LongUtils.ceilLog2(groupFlag.length)) - groupFlag.length;
//        boolean[] res = new boolean[groupFlag.length];
//        int index = groupFlag.length - 1;
//        while (index >= 0) {
//            int curr = index - 1;
//            while (curr >= 0 && (!groupFlag[curr])) {
//                curr--;
//            }
//            res[rightMostChildOfLeftSubTreeOfCommonAncestor(curr + 1, index)] = true;
//            index = curr;
//        }
//        return res;
//    }
//
//    /**
//     * the index at the rightmost leaf of the left subtree under the lowest common ancestor of l and r
//     */
//    int rightMostChildOfLeftSubTreeOfCommonAncestor(int l, int r) {
//        if (l == r) {
//            return l;
//        }
//        int level = ceilSmallOrEqual(l ^ r);
//        return ((r >>> level) << level) - 1;
//    }
//
//    /**
//     * return the value y such that (1 << y) <= n && (1 << (1 + y)) > n
//     */
//    int ceilSmallOrEqual(int n) {
//        if (n == 0) {
//            return 0;
//        }
//        int level = -1;
//        int initNum = 1;
//        while (initNum <= n) {
//            level++;
//            initNum <<= 1;
//        }
//        return level;
//    }
//
//    @Override
//    public SquareZ2Vector[] groupAgg(SquareZ2Vector[] xiArrays, SquareZ2Vector validFlags, AggTypes aggTypes, BitVector groupFlag) throws MpcAbortException {
//        setInputs(xiArrays, validFlags, aggTypes, groupFlag);
//        int levelNum = LongUtils.ceilLog2(dataNum);
//        // todo 先处理一遍 得到初始的sValue和pValue，对于左孩子：p=0, s=v; 对于右孩子：p = v - fv, s = fv. 需要验证是否正确
//
//        SquareZ2Vector[] evenValues = Amos22OneSideGroupUtils.getPos(xiArrays, 1, dataNum / 2, 2, parallel);
//        BitVector envGroupFlag = gFlag.getPointsWithFixedSpace(1, dataNum / 2, 2);
//        SquareZ2Vector[] fv = plainBitMuxReceiver.mux(envGroupFlag, evenValues);
//        SquareZ2Vector[] vXorFv = z2cReceiver.xor(fv, evenValues);
//        Amos22OneSideGroupUtils.setPos(sValues, fv, 1, dataNum / 2, 2, parallel);
//        Amos22OneSideGroupUtils.setPos(pValues, vXorFv, 1, dataNum / 2, 2, parallel);
//
//        for (int level = 0; level < levelNum; level++) {
//            int childGroupLen = 1<<level;
//            int parentLen = childGroupLen<<1;
//            int mergeNum = dataNum / parentLen;
//            mergeNum += dataNum % parentLen > childGroupLen ? 1 : 0;
//            SquareZ2Vector[] sl = Amos22OneSideGroupUtils.getPos(sValues, childGroupLen - 1, mergeNum, parentLen, parallel);
//            SquareZ2Vector[] pr = Amos22OneSideGroupUtils.getPos(pValues, childGroupLen, mergeNum, parentLen, parallel);
//            // 得到比较结果
//            SquareZ2Vector leqRes = (SquareZ2Vector) z2IntegerCircuit.leqParallel(sl, pr);
//            SquareZ2Vector aggChange = aggTypes.equals(AggTypes.MAX) ? SquareZ2Vector.createZeros(mergeNum) : SquareZ2Vector.createOnes(mergeNum);
//            z2cReceiver.xori(leqRes, aggChange);
//            SquareZ2Vector[] leqFlagExtend = new SquareZ2Vector[resultData.length];
//            Arrays.fill(leqFlagExtend, leqRes);
//            SquareZ2Vector[] extremeValues = (SquareZ2Vector[]) z2cReceiver.mux(sl, pr, leqFlagExtend);
//
//            // todo 选择是否要置换的两个flag
//            BitVector plChangeFlag = null;
//            BitVector srChangeFlag = null;
//            // 原始的pl和sr
//            SquareZ2Vector[] pl = Amos22OneSideGroupUtils.getPos(pValues, 0, mergeNum, parentLen, parallel);
//            SquareZ2Vector[] sr = Amos22OneSideGroupUtils.getPos(sValues, parentLen - 1, mergeNum, parentLen, parallel);
//            SquareZ2Vector[] newPl = z2cReceiver.xor(plainBitMuxReceiver.mux(plChangeFlag, z2cReceiver.xor(pl, extremeValues)), pl);
//            SquareZ2Vector[] newSr = z2cReceiver.xor(plainBitMuxReceiver.mux(srChangeFlag, z2cReceiver.xor(sr, extremeValues)), sr);
//
//            Amos22OneSideGroupUtils.setPos(pValues, newPl, 0, mergeNum, parentLen, parallel);
//            Amos22OneSideGroupUtils.setPos(sValues, newSr, parentLen - 1, mergeNum, parentLen, parallel);
//
//            // todo 处理resultData
//        }
//        // 先得到最初始的结果，
//        return resultData;
//    }
//
//    @Override
//    public SquareZ2Vector[][] groupAgg(SquareZ2Vector[][] xiArrays, SquareZ2Vector[] validFlags,
//                                       AggTypes[] aggTypes, boolean[] groupFlag) throws MpcAbortException {
////        setInputs(xiArrays, validFlags, aggTypes, groupFlag);
////        int levelNum = LongUtils.ceilLog2(dataNum);
////        for (int level = 0; level < levelNum; level++) {
////            int mergeLen = 1 << (level + 1);
////            int mergeNum = dataNum / mergeLen;
////
////        }
//
//        // 先得到最初始的结果，
//        return new SquareZ2Vector[0][];
//    }
//
//
//}
