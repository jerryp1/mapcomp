package edu.alibaba.mpc4j.s2pc.groupagg.pto.group.share;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.GroupTypes.AggTypes;

/**
 * Abstract group aggregation Party.
 *
 */
public abstract class AbstractShareGroupParty extends AbstractTwoPartyPto implements ShareGroupParty {
    /**
     * the number of tuples
     */
    protected int dataNum;
    /**
     * bit length of attribute
     */
    protected int dimLen;

    protected AbstractShareGroupParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, ShareGroupConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    protected void setInitInput(int attrNum, int maxNum, int maxBitNum) {
        MathPreconditions.checkPositive("attrNum", attrNum);
        MathPreconditions.checkGreaterOrEqual("maxNum", maxNum, 2);
        MathPreconditions.checkPositive("maxBitNum", maxBitNum);
        initState();
    }

    protected void setInputs(SquareZ2Vector[][] xiArrays, SquareZ2Vector[] validFlags, AggTypes[] aggType, SquareZ2Vector groupFlag) {
        dataNum = xiArrays[0][0].bitNum();
        assert dataNum >= 2;
        dimLen = xiArrays[0].length;
        assert xiArrays.length == aggType.length;
        for (SquareZ2Vector[] eachAttr : xiArrays) {
            MathPreconditions.checkEqual("dimLen", "eachAttr.length", dimLen, eachAttr.length);
            for (SquareZ2Vector each : eachAttr) {
                MathPreconditions.checkEqual("dataNum", "each.bitNum()", dataNum, each.bitNum());
            }
        }
        if (validFlags != null) {
            for (SquareZ2Vector each : validFlags) {
                MathPreconditions.checkEqual("dataNum", "validFlag.bitNum()", dataNum, each.bitNum());
            }
        }
        MathPreconditions.checkEqual("dataNum", "groupFlag.length", dataNum, groupFlag.bitNum());
    }
}
