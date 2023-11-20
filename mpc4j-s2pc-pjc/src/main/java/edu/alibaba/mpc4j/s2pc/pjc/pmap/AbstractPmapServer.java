package edu.alibaba.mpc4j.s2pc.pjc.pmap;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * abstract private map server
 *
 * @author Feng Han
 * @date 2023/10/23
 */
public abstract class AbstractPmapServer<T> extends AbstractTwoPartyPto implements PmapServer<T> {
    /**
     * max server element size
     */
    private int maxServerElementSize;
    /**
     * max client element size
     */
    private int maxClientElementSize;
    /**
     * server element array list
     */
    protected ArrayList<T> serverElementArrayList;
    /**
     * server element size
     */
    protected int serverElementSize;
    /**
     * client element size
     */
    protected int clientElementSize;

    protected AbstractPmapServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, PmapConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(int maxServerElementSize, int maxClientElementSize) {
        MathPreconditions.checkGreaterOrEqual("maxServerElementSize >= 2", maxServerElementSize, 2);
        MathPreconditions.checkGreaterOrEqual("maxServerElementSize >= maxClientElementSize", maxServerElementSize, maxClientElementSize);
        MathPreconditions.checkGreaterOrEqual("maxClientElementSize >= 2", maxClientElementSize, 2);
        this.maxServerElementSize = maxServerElementSize;
        MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
        this.maxClientElementSize = maxClientElementSize;
        initState();
    }

    protected void setPtoInput(List<T> serverElementList, int clientElementSize) {
        checkInitialized();
        MathPreconditions.checkGreaterOrEqual("serverElementList.size() >= 2", serverElementList.size(), 2);
        MathPreconditions.checkPositiveInRangeClosed("serverElementSize", serverElementList.size(), maxServerElementSize);
        serverElementSize = serverElementList.size();
        serverElementArrayList = new ArrayList<>(serverElementList);
        MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementSize, maxClientElementSize);
        this.clientElementSize = clientElementSize;
        extraInfo++;
    }
}
