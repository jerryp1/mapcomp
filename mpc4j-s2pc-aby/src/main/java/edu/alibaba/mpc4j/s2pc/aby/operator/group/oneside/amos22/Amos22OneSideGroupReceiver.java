package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.AbstractZ2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.AbstractOneSideGroupReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory.AggTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupReceiver;

import java.util.Arrays;


public class Amos22OneSideGroupReceiver extends AbstractOneSideGroupReceiver implements OneSideGroupReceiver {
    private final AbstractZ2cParty z2cReceiver;
    private final  Z2IntegerCircuit z2IntegerCircuit;
    protected Amos22OneSideGroupReceiver(PtoDesc ptoDesc, Rpc rpc, Party otherParty, OneSideGroupConfig config) {
        super(ptoDesc, rpc, otherParty, config);
        z2cReceiver = null;
        z2IntegerCircuit = new Z2IntegerCircuit(z2cReceiver);

    }

    @Override
    public void init(int maxL, int maxNum, int maxBitNum) throws MpcAbortException {

    }

    @Override
    public boolean[] getResPosFlag(boolean[] groupFlag) {
        int skipNum = (1 << LongUtils.ceilLog2(groupFlag.length)) - groupFlag.length;
        boolean[] res = new boolean[groupFlag.length];
        int index = groupFlag.length - 1;
        while (index >= 0) {
            int curr = index - 1;
            while (curr >= 0 && (!groupFlag[curr])) {
                curr--;
            }
            res[rightMostChildOfLeftSubTreeOfCommonAncestor(curr + 1, index)] = true;
            index = curr;
        }
        return res;
    }

    /**
     * the index at the rightmost leaf of the left subtree under the lowest common ancestor of l and r
     */
    int rightMostChildOfLeftSubTreeOfCommonAncestor(int l, int r) {
        if (l == r) {
            return l;
        }
        int level = ceilSmallOrEqual(l ^ r);
        return ((r >>> level) << level) - 1;
    }

    /**
     * return the value y such that (1 << y) <= n && (1 << (1 + y)) > n
     */
    int ceilSmallOrEqual(int n) {
        if (n == 0) {
            return 0;
        }
        int level = -1;
        int initNum = 1;
        while (initNum <= n) {
            level++;
            initNum <<= 1;
        }
        return level;
    }

    @Override
    public SquareZ2Vector[] groupAgg(SquareZ2Vector[] xiArrays, SquareZ2Vector validFlags, AggTypes aggTypes, boolean[] groupFlag) throws MpcAbortException {
        setInputs(xiArrays, validFlags, aggTypes, groupFlag);
        int levelNum = LongUtils.ceilLog2(dataNum);
        for (int level = 0; level < levelNum; level++) {
            int childGroupLen = 1<<level;
            int parentLen = childGroupLen<<1;
            int mergeNum = dataNum / parentLen;
            mergeNum += dataNum % parentLen > childGroupLen ? 1 : 0;
            SquareZ2Vector[][] slAndPr = Amos22OneSideGroupUtils.getPos(inputData, new int[]{childGroupLen - 1, childGroupLen}, parentLen, mergeNum, parallel);
            // 得到比较结果
            SquareZ2Vector leqRes = (SquareZ2Vector) z2IntegerCircuit.leqParallel(slAndPr[0], slAndPr[1]);
            SquareZ2Vector aggChange = aggTypes.equals(AggTypes.MAX) ? SquareZ2Vector.createZeros(mergeNum) : SquareZ2Vector.createOnes(mergeNum);
            z2cReceiver.xori(leqRes, aggChange);
            SquareZ2Vector[] leqFlagExtend = new SquareZ2Vector[inputData.length];
            Arrays.fill(leqFlagExtend, leqRes);
            SquareZ2Vector[] extremeValues = (SquareZ2Vector[]) z2cReceiver.mux(slAndPr[0], slAndPr[1], leqFlagExtend);

            // 选择是否要置换的两个flag
            SquareZ2Vector[] plChangeFlag = new SquareZ2Vector[inputData.length];
            SquareZ2Vector[] srChangeFlag = new SquareZ2Vector[inputData.length];
            // 原始的pl和sr
            SquareZ2Vector[][] plAndSr = Amos22OneSideGroupUtils.getPos(inputData, new int[]{0, parentLen - 1}, parentLen, mergeNum, parallel);
            SquareZ2Vector[] newPlValues = (SquareZ2Vector[]) z2cReceiver.mux(plAndSr[0], extremeValues, plChangeFlag);
            SquareZ2Vector[] newSrValues = (SquareZ2Vector[]) z2cReceiver.mux(plAndSr[1], extremeValues, srChangeFlag);
            Amos22OneSideGroupUtils.setPos(inputData, newPlValues, 0, parentLen, mergeNum, parallel);
            Amos22OneSideGroupUtils.setPos(inputData, newSrValues, parentLen - 1, parentLen, mergeNum, parallel);
        }


        // 先得到最初始的结果，
        return new SquareZ2Vector[0];
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
