package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;

/**
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public abstract class AbstractSqOprfReceiver extends AbstractTwoPartyPto implements SqOprfReceiver {

    /**
     * 最大批处理数量
     */
    protected int maxBatchSize;
    /**
     * 输入数组
     */
    protected byte[][] inputs;
    /**
     * 批处理数量
     */
    protected int batchSize;

    protected AbstractSqOprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, SqOprfConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(int maxBatchSize) {
        // SQ OPRF requires maxBatchSize > 1
        MathPreconditions.checkGreater("maxBatchSize", maxBatchSize, 1);
        this.maxBatchSize = maxBatchSize;

        initState();
    }

    protected void setPtoInput(byte[][] inputs) {
        checkInitialized();
        // SQ OPRF requires batchSize > 1
        MathPreconditions.checkGreater("batchSize", inputs.length, 1);
        MathPreconditions.checkLessOrEqual("batchSize", inputs.length, maxBatchSize);
        this.inputs = inputs;
        batchSize = inputs.length;
        extraInfo++;
    }

}
