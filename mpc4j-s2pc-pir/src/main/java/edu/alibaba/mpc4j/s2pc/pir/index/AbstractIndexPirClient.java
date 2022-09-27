package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

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
     * 元素字节长度
     */
    protected int elementByteLength;
    /**
     * 客户端检索值。
     */
    protected int index;
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

    protected void setInitInput(int serverElementSize, int elementByteLength) {
        assert elementByteLength >= 1;
        this.elementByteLength = elementByteLength;
        assert serverElementSize >= 1;
        this.serverElementSize = serverElementSize;
        extraInfo++;
        initialized = false;
    }

    protected void setPtoInput(int index) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert index >= 0 && index < serverElementSize;
        this.index = index;
        extraInfo++;
    }
}
