package edu.alibaba.mpc4j.s2pc.pir.index.single;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * Abstract Single Index PIR client.
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public abstract class AbstractSingleIndexPirClient extends AbstractTwoPartyPto implements SingleIndexPirClient {
    /**
     * element byte length
     */
    protected int elementByteLength;
    /**
     * database size
     */
    protected int num;
    /**
     * partition byte length
     */
    protected int partitionByteLength;
    /**
     * partition size
     */
    protected int partitionSize;

    protected AbstractSingleIndexPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty,
                                           SingleIndexPirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int num, int elementByteLength, int maxPartitionByteLength) {
        MathPreconditions.checkPositive("elementByteLength", elementByteLength);
        this.elementByteLength = elementByteLength;
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        partitionByteLength = Math.min(maxPartitionByteLength, elementByteLength);
        partitionSize = CommonUtils.getUnitNum(elementByteLength, partitionByteLength);
        initState();
    }

    protected void setPtoInput(int index) {
        checkInitialized();
        MathPreconditions.checkNonNegativeInRange("index", index, num);
        extraInfo++;
    }
}