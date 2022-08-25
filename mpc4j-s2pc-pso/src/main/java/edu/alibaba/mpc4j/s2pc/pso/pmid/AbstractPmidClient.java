package edu.alibaba.mpc4j.s2pc.pso.pmid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pso.pmid.PmidFactory.PmidType;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * PMID协议客户端抽象类。
 *
 * @author Weiran Liu
 * @date 2022/5/10
 */
public abstract class AbstractPmidClient<T> extends AbstractSecureTwoPartyPto implements PmidClient<T> {
    /**
     * 配置项
     */
    private final PmidConfig config;
    /**
     * 客户端集合最大数量
     */
    private int maxClientSetSize;
    /**
     * 客户端最大重复元素上界
     */
    private int maxClientK;
    /**
     * 服务端集合最大数量
     */
    private int maxServerSetSize;
    /**
     * 服务端最大重复元素上界
     */
    private int maxServerK;
    /**
     * 客户端元素映射
     */
    protected Map<T, Integer> clientElementMap;
    /**
     * 客户端元素列表
     */
    protected ArrayList<T> clientElementArrayList;
    /**
     * 客户端元素数量
     */
    protected int clientSetSize;
    /**
     * 客户端重复元素上界
     */
    protected int clientK;
    /**
     * 服务端元素数量
     */
    protected int serverSetSize;
    /**
     * 服务端重复元素上界
     */
    protected int serverK;

    protected AbstractPmidClient(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PmidConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    @Override
    public PmidType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxClientSetSize, int maxClientK, int maxServerSetSize) {
        setInitInput(maxClientSetSize, maxClientK, maxServerSetSize, 1);
    }

    protected void setInitInput(int maxClientSetSize, int maxClientK, int maxServerSetSize, int maxServerK) {
        assert maxClientSetSize > 1 : "max(ClientSetSize) must be greater than 1";
        this.maxClientSetSize = maxClientSetSize;
        assert maxClientK >= 1 : "max(ClientK) must be greater than or equal to 1";
        this.maxClientK = maxClientK;
        assert maxServerSetSize > 1 : "max(ServerSetSize) must be greater than 1";
        this.maxServerSetSize = maxServerSetSize;
        assert maxServerK >= 1 : "max(ServerK) must be greater than or equal to 1";
        this.maxServerK = maxServerK;

        initialized = false;
    }

    protected void setPtoInput(Map<T, Integer> clientElementMap, int serverSetSize) {
        setPtoInput(clientElementMap, serverSetSize, 1);
    }

    protected void setPtoInput(Map<T, Integer> clientElementMap, int serverSetSize, int serverK) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        Set<T> clientElementSet = clientElementMap.keySet();
        assert clientElementSet.size() > 1 && clientElementSet.size() <= maxClientSetSize :
            "ClientSetSize must be in range (1, " + maxClientSetSize + "]";
        this.clientElementMap = clientElementMap;
        clientElementArrayList = new ArrayList<>(clientElementSet);
        clientSetSize = clientElementSet.size();
        clientK = clientElementSet.stream()
            .mapToInt(clientElementMap::get)
            .peek(uy -> {
                assert uy >= 1 : "uy must be greater than or equal to 1";
            })
            .max()
            .orElse(0);
        assert clientK >= 1 && clientK <= maxClientK : "ClientK must be in range [1, " + maxClientK + "]: " + clientK;
        assert serverSetSize > 1 && serverSetSize <= maxServerSetSize :
            "ServerSetSize must be in range (1, " + maxServerSetSize + "]";
        this.serverSetSize = serverSetSize;
        assert serverK >= 1 && serverK <= maxServerK : "ServerK must be in range [1, " + maxServerK + "]: " + serverK;
        this.serverK = serverK;
        extraInfo++;
    }
}
