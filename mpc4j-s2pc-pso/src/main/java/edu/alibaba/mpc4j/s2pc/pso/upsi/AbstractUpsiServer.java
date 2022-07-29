package edu.alibaba.mpc4j.s2pc.pso.upsi;

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
 * 非平衡PSI协议服务端。
 *
 * @author Liqiang Peng
 * @date 2022/6/14
 */
public abstract class AbstractUpsiServer extends AbstractSecureTwoPartyPto implements UpsiServer {
    /**
     * 配置项
     */
    private final UpsiConfig config;
    /**
     * 客户端最大元素数量
     */
    private int maxClientElementSize;
    /**
     * 服务端元素数组
     */
    protected ArrayList<ByteBuffer> serverElementArrayList;
    /**
     * 服务端元素数量
     */
    protected int serverElementSize;
    /**
     * 客户端元素数量
     */
    protected int clientElementSize;
    /**
     * 元素字节长度
     */
    protected int elementByteLength;
    /**
     * 特殊空元素字节缓存区
     */
    protected ByteBuffer botElementByteBuffer;


    protected AbstractUpsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, UpsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
        this.config = config;
    }

    @Override
    public UpsiFactory.UpsiType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxClientElementSize) {
        assert maxClientElementSize >= 1;
        this.maxClientElementSize = maxClientElementSize;
        extraInfo++;
        initialized = false;
    }

    protected void setPtoInput(Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert elementByteLength >= CommonConstants.STATS_BYTE_LENGTH;
        this.elementByteLength = elementByteLength;
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[elementByteLength];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        assert serverElementSet.size() >= 1;
        this.serverElementArrayList = serverElementSet.stream()
            .peek(senderElement -> {
                assert senderElement.array().length == elementByteLength;
                assert !senderElement.equals(botElementByteBuffer) : "input equals ⊥";
            })
            .collect(Collectors.toCollection(ArrayList::new));
        serverElementSize = serverElementSet.size();
        assert clientElementSize >= 1 && clientElementSize <= maxClientElementSize;
        this.clientElementSize = clientElementSize;
        extraInfo++;
    }
}
