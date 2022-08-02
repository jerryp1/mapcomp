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
     * 服务端关键词数组
     */
    protected ArrayList<ByteBuffer> serverKeywordArrayList;
    /**
     * 服务端关键词数量
     */
    protected int serverKeywordSize;
    /**
     * 标签字节长度
     */
    protected int labelByteLength;
    /**
     * 特殊空元素字节缓存区
     */
    protected ByteBuffer botElementByteBuffer;
    /**
     * 关键词和标签映射
     */
    protected Map<ByteBuffer, ByteBuffer> keywordLabelMap;

    protected AbstractKwPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, KwPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
        this.config = config;
    }

    @Override
    public KwPirFactory.PirType getPtoType() {
        return config.getProType();
    }

    protected void setInitInput(Map<ByteBuffer, ByteBuffer> keywordLabelMap, int labelByteLength) {
        assert labelByteLength >= 1;
        this.labelByteLength = labelByteLength;
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte) 0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        assert keywordLabelMap.size() >= 1;
        this.keywordLabelMap = keywordLabelMap;
        this.serverKeywordSize = keywordLabelMap.size();
        Iterator<Entry<ByteBuffer, ByteBuffer>> iter = keywordLabelMap.entrySet().iterator();
        Set<ByteBuffer> serverElementSet = new HashSet<>();
        while (iter.hasNext()) {
            Entry<ByteBuffer, ByteBuffer> entry = iter.next();
            ByteBuffer item = entry.getKey();
            serverElementSet.add(item);
        }
        this.serverKeywordArrayList = serverElementSet.stream()
            .peek(serverElement -> {
                assert !serverElement.equals(botElementByteBuffer) : "input equals ⊥";
            })
            .collect(Collectors.toCollection(ArrayList::new));
        extraInfo++;
        initialized = false;
    }

    protected void setPtoInput() {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        extraInfo++;
    }
}
