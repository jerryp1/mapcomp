//package edu.alibaba.mpc4j.s2pc.pso.cpsi;
//
//import edu.alibaba.mpc4j.common.rpc.Party;
//import edu.alibaba.mpc4j.common.rpc.Rpc;
//import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
//import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
//
//import java.nio.ByteBuffer;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Set;
//import java.util.stream.Collectors;
//
///**
// * Circuit PSI协议客户端。
// *
// * @author Liqiang Peng
// * @date 2023/1/30
// */
//public abstract class AbstractCpsiClient extends AbstractSecureTwoPartyPto implements CpsiClient {
//    /**
//     * 配置项
//     */
//    private final CpsiConfig config;
//    /**
//     * 客户端最大元素数量
//     */
//    private int maxClientElementSize;
//    /**
//     * 服务端最大元素数量
//     */
//    private int maxServerElementSize;
//    /**
//     * 客户端元素集合
//     */
//    protected ArrayList<ByteBuffer> clientElementArrayList;
//    /**
//     * 客户端元素数量
//     */
//    protected int clientElementSize;
//    /**
//     * 服务端元素数量
//     */
//    protected int serverElementSize;
//    /**
//     * 元素字节长度
//     */
//    protected int elementByteLength;
//    /**
//     * 特殊空元素字节缓存区
//     */
//    protected ByteBuffer botElementByteBuffer;
//
//    protected AbstractCpsiClient(PtoDesc ptoDesc, Rpc rpc, Party otherParty, CpsiConfig config) {
//        super(ptoDesc, rpc, otherParty, config);
//        this.config = config;
//    }
//
//    @Override
//    public CpsiFactory.CpsiType getPtoType() {
//        return config.getPtoType();
//    }
//
//    protected void setInitInput(int maxClientElementSize, int maxServerElementSize) {
//        assert maxClientElementSize > 0 : "max client element size must be greater than 0: " + maxClientElementSize;
//        this.maxClientElementSize = maxClientElementSize;
//        assert maxServerElementSize > 0 : "max server element size must be greater than 0: " + maxServerElementSize;
//        this.maxServerElementSize = maxServerElementSize;
//        extraInfo++;
//        initialized = false;
//    }
//
//    protected void setPtoInput(Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength) {
//        if (!initialized) {
//            throw new IllegalStateException("Need init...");
//        }
//        assert elementByteLength > 0;
//        this.elementByteLength = elementByteLength;
//        // 设置特殊空元素
//        byte[] botElementByteArray = new byte[elementByteLength];
//        Arrays.fill(botElementByteArray, (byte)0xFF);
//        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
//        assert clientElementSet.size() > 0 && clientElementSet.size() <= maxClientElementSize
//            : "client element size must be in range (0, " + maxServerElementSize + "]: " + clientElementSet.size();
//        clientElementSize = clientElementSet.size();
//        clientElementArrayList = clientElementSet.stream()
//            .peek(element -> {
//                assert element.array().length == elementByteLength;
//                assert !element.equals(botElementByteBuffer) : " input equals ⊥";
//            })
//            .collect(Collectors.toCollection(ArrayList::new));
//        assert serverElementSize > 0 && serverElementSize <= maxServerElementSize
//            : "server element size must be in range (0, " + maxServerElementSize + "]: " + serverElementSize;
//        this.serverElementSize = serverElementSize;
//        extraInfo++;
//    }
//}
