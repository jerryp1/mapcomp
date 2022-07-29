package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * 关键词索引PIR协议服务端。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public abstract class AbstractKwPirServer extends AbstractSecureTwoPartyPto implements KwPirServer {
    /**
     * 配置项
     */
    private final KwPirConfig config;
    /**
     * 服务端元素数组
     */
    protected ArrayList<ByteBuffer> serverElementArrayList;
    /**
     * 服务端元素数量
     */
    protected int serverElementSize;
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
     * 服务端元素和标签映射
     */
    protected Map<ByteBuffer, ByteBuffer> serverElementMap;
    /**
     * 查询次数
     */
    protected int retrievalNum;

    protected AbstractKwPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, KwPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
        this.config = config;
    }

    @Override
    public KwPirFactory.PirType getPtoType() {
        return config.getProType();
    }

    protected void setInitInput(Map<ByteBuffer, ByteBuffer> serverElementMap, int elementByteLength, int labelByteLength) {
        assert elementByteLength >= CommonConstants.STATS_BYTE_LENGTH;
        this.elementByteLength = elementByteLength;
        assert labelByteLength >= 1;
        this.labelByteLength = labelByteLength;
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[elementByteLength];
        Arrays.fill(botElementByteArray, (byte) 0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        assert serverElementMap.size() >= 1;
        this.serverElementMap = serverElementMap;
        this.serverElementSize = serverElementMap.size();
        Iterator<Entry<ByteBuffer, ByteBuffer>> iter = serverElementMap.entrySet().iterator();
        Set<ByteBuffer> serverElementSet = new HashSet<>();
        while (iter.hasNext()) {
            Entry<ByteBuffer, ByteBuffer> entry = iter.next();
            ByteBuffer item = entry.getKey();
            serverElementSet.add(item);
        }
        this.serverElementArrayList = serverElementSet.stream()
            .peek(senderElement -> {
                assert senderElement.array().length == elementByteLength;
                assert !senderElement.equals(botElementByteBuffer) : "input equals ⊥";
            })
            .collect(Collectors.toCollection(ArrayList::new));
        extraInfo++;
        initialized = false;
    }

    protected void setPtoInput(int retrievalNumber) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert retrievalNumber > 0;
        this.retrievalNum = retrievalNumber;
        extraInfo++;
    }
}
