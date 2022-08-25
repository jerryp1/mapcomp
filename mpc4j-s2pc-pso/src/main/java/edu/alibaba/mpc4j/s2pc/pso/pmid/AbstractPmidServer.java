package edu.alibaba.mpc4j.s2pc.pso.pmid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PMID协议服务端抽象类。
 *
 * @author Weiran Liu
 * @date 2022/5/6
 */
public abstract class AbstractPmidServer<T> extends AbstractSecureTwoPartyPto implements PmidServer<T> {
    /**
     * 配置项
     */
    private final PmidConfig config;
    /**
     * 服务端集合最大数量
     */
    private int maxServerSetSize;
    /**
     * 服务端最大重复元素上界
     */
    private int maxServerK;
    /**
     * 客户端集合最大数量
     */
    private int maxClientSetSize;
    /**
     * 客户端最大重复元素上界
     */
    private int maxClientK;
    /**
     * 服务端元素映射（只用于服务端有重复元素的情况）
     */
    protected Map<T, Integer> serverElementMap;
    /**
     * 服务端元素列表
     */
    protected ArrayList<T> serverElementArrayList;
    /**
     * 服务端元素数量
     */
    protected int serverSetSize;
    /**
     * 服务端重复元素上界（只用于服务端有重复元素的情况）
     */
    protected int serverK;
    /**
     * 客户端元素数量
     */
    protected int clientSetSize;
    /**
     * 客户端重复元素上界
     */
    protected int clientK;

    protected AbstractPmidServer(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PmidConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    @Override
    public PmidFactory.PmidType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxServerSetSize, int maxClientSetSize, int maxClientK) {
        setInitInput(maxServerSetSize, 1, maxClientSetSize, maxClientK);
    }

    protected void setInitInput(int maxServerSetSize, int maxServerK, int maxClientSetSize, int maxClientK) {
        assert maxServerSetSize > 1 : "max(ServerSetSize) must be greater than 1";
        this.maxServerSetSize = maxServerSetSize;
        assert maxServerK >= 1 : "max(ServerK) must be greater than or equal to 1";
        this.maxServerK = maxServerK;
        assert maxClientSetSize > 1 : "max(ClientSetSize) must be greater than 1";
        this.maxClientSetSize = maxClientSetSize;
        assert maxClientK >= 1 : "max(ClientK) must be greater than or equal to 1";
        this.maxClientK = maxClientK;
        initialized = false;
    }

    protected void setPtoInput(Set<T> serverElementSet, int clientSetSize, int clientK) {
        Map<T, Integer> serverElementMap = serverElementSet.stream()
            .collect(Collectors.toMap(
                    element -> element,
                    element -> 1
                )
            );
        setPtoInput(serverElementMap, clientSetSize, clientK);
    }

    protected void setPtoInput(Map<T, Integer> serverElementMap, int clientSetSize, int clientK) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        Set<T> serverElementSet = serverElementMap.keySet();
        assert serverElementSet.size() > 1 && serverElementSet.size() <= maxServerSetSize :
            "ServerSetSize must be in range (1, " + maxServerSetSize + "]";
        this.serverElementMap = serverElementMap;
        serverElementArrayList = new ArrayList<>(serverElementSet);
        serverSetSize = serverElementSet.size();
        serverK = serverElementSet.stream()
            .mapToInt(serverElementMap::get)
            .peek(ux -> {
                assert ux >= 1 : "ux must be greater than or equal to 1: " + ux;
            })
            .max()
            .orElse(0);
        assert serverK >= 1 && serverK <= maxServerK : "ServerK must be in range [1, " + maxServerK + "]: " + serverK;
        assert clientSetSize > 1 && clientSetSize <= maxClientSetSize :
            "ClientSetSize must be in range (1, " + maxClientSetSize + "]";
        this.clientSetSize = clientSetSize;
        assert clientK >= 1 && clientK <= maxClientK : "ClientK must be in range [1, " + maxClientK + "]: " + clientK;
        this.clientK = clientK;
        extraInfo++;
    }
}
