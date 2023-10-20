package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * abstract payload-circuit PSI client.
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public abstract class AbstractPlpsiClient<T> extends AbstractTwoPartyPto implements PlpsiClient<T> {
    /**
     * whether the sharing type of payload is binary
     */
    protected final boolean isBinaryShare;
    /**
     * max client element size
     */
    private int maxClientElementSize;
    /**
     * max server element size
     */
    private int maxServerElementSize;
    /**
     * max server payload bit length
     */
    protected int serverPayloadBitL;
    /**
     * client element array list
     */
    protected ArrayList<T> clientElementArrayList;
    /**
     * client element size
     */
    protected int clientElementSize;
    /**
     * sever element size
     */
    protected int serverElementSize;

    protected AbstractPlpsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, PlpsiConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
        isBinaryShare = config.isBinaryShare();
    }

    protected void setInitInput(int maxClientElementSize, int maxServerElementSize, int serverPayloadBitL) {
        MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
        this.maxClientElementSize = maxClientElementSize;
        MathPreconditions.checkPositive("maxServerElementSize", maxServerElementSize);
        this.maxServerElementSize = maxServerElementSize;
        MathPreconditions.checkPositive("serverPayloadBitL", serverPayloadBitL);
        this.serverPayloadBitL = serverPayloadBitL;
        initState();
    }

    protected void setPtoInput(List<T> clientElementList, int serverElementSize) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementList.size(), maxClientElementSize);
        clientElementSize = clientElementList.size();
        clientElementArrayList = new ArrayList<>(clientElementList);
        MathPreconditions.checkPositiveInRangeClosed("serverElementSize", serverElementSize, maxServerElementSize);
        this.serverElementSize = serverElementSize;
        extraInfo++;
    }
}
