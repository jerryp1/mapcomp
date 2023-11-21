package edu.alibaba.mpc4j.s2pc.pjc.pmap;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * abstract private map client
 *
 * @author Feng Han
 * @date 2023/10/23
 */
public abstract class AbstractPmapClient<T> extends AbstractTwoPartyPto implements PmapClient<T> {
    /**
     * the max size of client's elements
     */
    private int maxClientElementSize;
    /**
     * the max size of server's elements
     */
    private int maxServerElementSize;
    /**
     * the list of client's element
     */
    protected ArrayList<T> clientElementArrayList;
    /**
     * the real input size of client
     */
    protected int clientElementSize;
    /**
     * the real input size of server
     */
    protected int serverElementSize;

    protected AbstractPmapClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, PmapConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int maxClientElementSize, int maxServerElementSize) {
        MathPreconditions.checkGreaterOrEqual("maxServerElementSize", maxServerElementSize, 2);
        MathPreconditions.checkGreaterOrEqual("maxServerElementSize >= maxClientElementSize", maxServerElementSize, maxClientElementSize);
        MathPreconditions.checkGreaterOrEqual("maxClientElementSize >= 2", maxClientElementSize, 2);
        this.maxClientElementSize = maxClientElementSize;
        MathPreconditions.checkPositive("maxServerElementSize", maxServerElementSize);
        this.maxServerElementSize = maxServerElementSize;
        initState();
    }

    protected void setPtoInput(List<T> clientElementList, int serverElementSize) {
        checkInitialized();
        MathPreconditions.checkGreaterOrEqual("serverElementSize >= 2", serverElementSize, 2);
        MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementList.size(), maxClientElementSize);
        clientElementSize = clientElementList.size();
        clientElementArrayList = new ArrayList<>(clientElementList);
        MathPreconditions.checkPositiveInRangeClosed("serverElementSize", serverElementSize, maxServerElementSize);
        this.serverElementSize = serverElementSize;
        extraInfo++;
    }
}
