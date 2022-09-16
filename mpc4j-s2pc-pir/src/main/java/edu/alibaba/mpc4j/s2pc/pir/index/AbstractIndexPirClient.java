package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

import java.util.Set;

/**
 * 索引PIR协议客户端抽象类。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public abstract class AbstractIndexPirClient extends AbstractSecureTwoPartyPto implements IndexPirClient {
    /**
     * 配置项
     */
    private final IndexPirConfig config;
    /**
     * 客户端单次查询最大查询关键词数目
     */
    protected int maxRetrievalSize;
    /**
     * 元素字节长度
     */
    protected int elementByteLength;
    /**
     * 客户端检索值。
     */
    protected int[] indexArray;
    /**
     * 客户端检索数量
     */
    protected int retrievalSize;
    /**
     * 服务端元素数量
     */
    protected int serverElementSize;

    protected AbstractIndexPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, IndexPirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
        this.config = config;
    }

    @Override
    public IndexPirFactory.IndexPirType getPtoType() {
        return config.getProType();
    }

    protected void setInitInput(int maxRetrievalSize, int serverElementSize, int elementByteLength) {
        assert elementByteLength >= 1;
        this.elementByteLength = elementByteLength;
        assert serverElementSize >= 1;
        this.serverElementSize = serverElementSize;
        assert maxRetrievalSize >= 1;
        this.maxRetrievalSize = maxRetrievalSize;
        extraInfo++;
        initialized = false;
    }

    protected void setPtoInput(Set<Integer> indexSet) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert indexSet.size() <= maxRetrievalSize;
        retrievalSize = indexSet.size();
        indexArray = indexSet.stream().mapToInt(index -> {
            assert index >= 0 && index < serverElementSize;
            return index;
        }).toArray();
        extraInfo++;
    }
}
