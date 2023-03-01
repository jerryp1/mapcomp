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
// * Circuit PSI协议服务端。
// *
// * @author Liqiang Peng
// * @date 2023/1/30
// */
//public abstract class AbstractCpsiServer extends AbstractSecureTwoPartyPto implements CpsiServer {
//    /**
//     * 配置项
//     */
//    private final CpsiConfig config;
//    /**
//     * 服务端最大元素数量
//     */
//    private int maxServerElementSize;
//    /**
//     * 客户端最大元素数量
//     */
//    private int maxClientElementSize;
//    /**
//     * 服务端元素数组
//     */
//    protected ArrayList<ByteBuffer> serverElementArrayList;
//    /**
//     * 服务端元素数量
//     */
//    protected int serverElementSize;
//    /**
//     * 客户端元素数量
//     */
//    protected int clientElementSize;
//    /**
//     * 元素字节长度
//     */
//    protected int elementByteLength;
//    /**
//     * 特殊空元素字节缓存区
//     */
//    protected ByteBuffer botElementByteBuffer;
//
//    protected AbstractCpsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, CpsiConfig config) {
//        super(ptoDesc, serverRpc, clientParty, config);
//        this.config = config;
//    }
//
//    @Override
//    public CpsiFactory.CpsiType getPtoType() {
//        return config.getPtoType();
//    }
//
//    protected void setInitInput(int maxServerElementSize, int maxClientElementSize) {
//        assert maxServerElementSize > 0 : "max server element size must be greater than 0: " + maxServerElementSize;
//        this.maxServerElementSize = maxServerElementSize;
//        assert maxClientElementSize > 0 : "mac client element size must be greater than 0: " + maxClientElementSize;
//        this.maxClientElementSize = maxClientElementSize;
//        extraInfo++;
//        initialized = false;
//    }
//
//    protected void setPtoInput(Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength) {
//        if (!initialized) {
//            throw new IllegalStateException("Need init...");
//        }
//        assert elementByteLength > 0;
//        this.elementByteLength = elementByteLength;
//        // 设置特殊空元素
//        byte[] botElementByteArray = new byte[elementByteLength];
//        Arrays.fill(botElementByteArray, (byte)0xFF);
//        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
//        assert serverElementSet.size() > 0 && serverElementSet.size() <= maxServerElementSize
//            : "server element size must be in range (0, " + maxServerElementSize + "]: " + serverElementSet.size();
//        serverElementSize = serverElementSet.size();
//        serverElementArrayList = serverElementSet.stream()
//            .peek(element -> {
//                assert element.array().length == elementByteLength;
//                assert !element.equals(botElementByteBuffer) : " input equals ⊥";
//            })
//            .collect(Collectors.toCollection(ArrayList::new));
//        assert clientElementSize > 0 && clientElementSize <= maxClientElementSize
//            : "client element size must be in range (0, " + maxClientElementSize + "]: " + clientElementSize;
//        this.clientElementSize = clientElementSize;
//        extraInfo++;
//    }
//}
