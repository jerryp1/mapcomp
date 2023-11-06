package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory.AggTypes;

import java.util.Arrays;

public abstract class AbstractOneSideGroupReceiver extends AbstractTwoPartyPto implements OneSideGroupReceiver {
    protected int dataNum;
    protected SquareZ2Vector[] inputData;
    protected SquareZ2Vector validFlagArray;
    protected AggTypes aggType;
    protected boolean[] gFlag;


    protected AbstractOneSideGroupReceiver(PtoDesc ptoDesc, Rpc rpc, Party otherParty, OneSideGroupConfig config) {
        super(ptoDesc, rpc, otherParty, config);

    }

    protected void setInputs(SquareZ2Vector[] xiArrays, SquareZ2Vector validFlags, AggTypes aggType, boolean[] groupFlag){
        dataNum = xiArrays[0].bitNum();
        int eachDimBitLen = xiArrays.length;
        MathPreconditions.checkEqual("dataNum", "groupFlag.length", dataNum, groupFlag.length);
        MathPreconditions.checkEqual("dataNum", "validFlags[i].bitNum()", dataNum, validFlags.bitNum());
        Arrays.stream(xiArrays).forEach(x -> MathPreconditions.checkEqual("dataNum", "xiArrays[i].bitNum()", dataNum, x.bitNum()));
        // copy original data
        inputData = Arrays.copyOf(xiArrays, xiArrays.length);
        validFlagArray = validFlags;
        this.aggType = aggType;
        gFlag = Arrays.copyOf(groupFlag, groupFlag.length);
    }

//    protected void setInputs(SquareZ2Vector[][] xiArrays, SquareZ2Vector[] validFlags, AggTypes[] aggTypes, boolean[] groupFlag){
//        MathPreconditions.checkEqual("xiArrays.length", "validFlags.length", xiArrays.length, validFlags.length);
//        MathPreconditions.checkEqual("xiArrays.length", "aggTypes.length", xiArrays.length, aggTypes.length);
//        dataNum = xiArrays[0][0].bitNum();
//        int eachDimBitLen = xiArrays[0].length;
//        MathPreconditions.checkEqual("dataNum", "groupFlag.length", dataNum, groupFlag.length);
//        for(int i = 0; i < xiArrays.length; i++){
//            MathPreconditions.checkEqual("dataNum", "validFlags[i].bitNum()", dataNum, validFlags[i].bitNum());
//            MathPreconditions.checkEqual("eachDimBitLen", "xiArrays[i].length", eachDimBitLen, xiArrays[i].length);
//            Arrays.stream(xiArrays[i]).forEach(x -> MathPreconditions.checkEqual("dataNum", "xiArrays[i][j].bitNum()", dataNum, x.bitNum()));
//        }
//        // copy original data
//        inputData = Arrays.stream(xiArrays).map(xi -> Arrays.copyOf(xi, xi.length)).toArray(SquareZ2Vector[][]::new);
//        validFlagArray = Arrays.copyOf(validFlags, validFlags.length);
//        this.aggTypes = aggTypes;
//        gFlag = Arrays.copyOf(groupFlag, groupFlag.length);
//    }
}
