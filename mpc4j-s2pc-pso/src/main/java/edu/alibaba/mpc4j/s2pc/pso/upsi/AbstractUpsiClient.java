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
 * 非平衡PSI协议客户端。
 *
 * @author Liqiang Peng
 * @date 2022/6/13
 */
public abstract class AbstractUpsiClient extends AbstractSecureTwoPartyPto implements UpsiClient {
    /**
     * 配置项
     */
    private final UpsiConfig config;
    /**
     * 客户端最大元素数量
     */
    private int maxClientElementSize;
    /**
     * 客户端元素集合
     */
    protected ArrayList<ByteBuffer> clientElementArrayList;
    /**
     * 客户端元素数量
     */
    protected int clientElementSize;
    /**
     * 元素比特长度
     */
    protected int elementByteLength;
    /**
     * 特殊空元素字节缓存区
     */
    protected ByteBuffer botElementByteBuffer;

    protected AbstractUpsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, UpsiConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
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

    protected void setPtoInput(Set<ByteBuffer> clientElementSet, int elementByteLength) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert elementByteLength >= CommonConstants.STATS_BYTE_LENGTH;
        this.elementByteLength = elementByteLength;
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[elementByteLength];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        assert clientElementSet.size() >= 1 && clientElementSet.size() <= maxClientElementSize;
        this.clientElementArrayList = clientElementSet.stream()
            .peek(clientElement -> {
                assert clientElement.array().length == elementByteLength;
                assert !clientElement.equals(botElementByteBuffer) : "input equals ⊥";
            })
            .collect(Collectors.toCollection(ArrayList::new));
        clientElementSize = clientElementSet.size();
        extraInfo++;
    }
}
