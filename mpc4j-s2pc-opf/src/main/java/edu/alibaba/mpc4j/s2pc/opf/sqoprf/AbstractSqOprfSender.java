package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSenderOutput;

/**
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public abstract class AbstractSqOprfSender extends AbstractTwoPartyPto implements SqOprfSender {

    /**
     * 最大批处理数量
     */
    protected int maxBatchSize;

    /**
     * 批处理数量
     */
    protected int batchSize;



    protected AbstractSqOprfSender(PtoDesc ptoDesc, Rpc rpc, Party otherParty, SqOprfConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }


    protected void setInitInput(int maxBatchSize) {
        // SQ OPRF requires maxBatchSize > 1
        MathPreconditions.checkGreater("maxBatchSize", maxBatchSize, 1);
        this.maxBatchSize = maxBatchSize;
        initState();
    }

    protected void setPtoInput(int batchSize) {
        checkInitialized();
        // SQ OPRF requires batchSize > 1
        MathPreconditions.checkGreater("batchSize", batchSize, 1);
        MathPreconditions.checkLessOrEqual("batchSize", batchSize, maxBatchSize);
        this.batchSize = batchSize;
        extraInfo++;
    }

}
