package edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.smallfield;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.AbstractPermGenParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenParty;

/**
 * Abstract Permutable Sorter Party.
 *
 * @author Li Peng
 * @date 2023/10/11
 */
public abstract class AbstractSmallFieldPermGenParty extends AbstractPermGenParty implements PermGenParty {

    protected AbstractSmallFieldPermGenParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, PermGenConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    protected void setInitInput(int maxNum, int maxBitNum) {
        MathPreconditions.checkGreaterOrEqual("maxBitNum <= 3", 3, maxBitNum);
        super.setInitInput(maxNum, maxBitNum);
    }

    protected void setPtoInput(SquareZ2Vector[] xiArray) {
        MathPreconditions.checkGreaterOrEqual("Number of input bits <= 3", 3, xiArray.length);
        super.setPtoInput(xiArray);
    }
}
