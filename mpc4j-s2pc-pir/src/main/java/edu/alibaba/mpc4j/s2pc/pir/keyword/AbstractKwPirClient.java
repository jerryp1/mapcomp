package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关键词索引PIR协议客户端。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public abstract class AbstractKwPirClient extends AbstractSecureTwoPartyPto implements KwPirClient {
    /**
     * 配置项
     */
    private final KwPirConfig config;
    /**
     * 服务端元素集合
     */
    protected ArrayList<ByteBuffer> serverElementArrayList;
    /**
     * 客户端元素数量
     */
    protected int clientElementSize;
    /**
     * 客户端单次查询最大查询元素数目
     */
    protected int maxClientRetrievalElementSize;
    /**
     * 元素字节长度
     */
    protected int elementByteLength;
    /**
     * 标签字节长度
     */
    protected int labelByteLength;
    /**
     * 特殊空元素字节缓存区
     */
    protected ByteBuffer botElementByteBuffer;
    /**
     * 查询次数
     */
    protected int retrievalNumber;

    protected AbstractKwPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, KwPirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
        this.config = config;
    }

    @Override
    public KwPirFactory.PirType getPtoType() {
        return config.getProType();
    }

    protected void setInitInput(Set<ByteBuffer> serverElementSet, int elementByteLength, int labelByteLength,
                                int maxClientRetrievalElementSize) {
        assert elementByteLength >= CommonConstants.STATS_BYTE_LENGTH;
        this.elementByteLength = elementByteLength;
        assert labelByteLength >= 1;
        this.labelByteLength = labelByteLength;
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[elementByteLength];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        assert serverElementSet.size() >= 1;
        this.serverElementArrayList = serverElementSet.stream()
            .peek(serverElement -> {
                assert serverElement.array().length == elementByteLength;
                assert !serverElement.equals(botElementByteBuffer) : "input equals ⊥";
            })
            .collect(Collectors.toCollection(ArrayList::new));
        assert maxClientRetrievalElementSize > 0;
        this.maxClientRetrievalElementSize = maxClientRetrievalElementSize;
        extraInfo++;
        initialized = false;
    }

    protected void setPtoInput(int retrievalNumber, int retrievalElementSize) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert retrievalElementSize <= maxClientRetrievalElementSize && retrievalElementSize > 0;
        this.clientElementSize = retrievalElementSize;
        assert retrievalNumber > 0;
        this.retrievalNumber = retrievalNumber;
        extraInfo++;
    }
}
