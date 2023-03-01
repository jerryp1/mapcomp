//package edu.alibaba.mpc4j.s2pc.pso.cpsi;
//
//import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
//import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
//import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
//
//import java.nio.ByteBuffer;
//import java.util.Set;
//
///**
// * Circuit PSI协议服务端接口。
// *
// * @author Liqiang Peng
// * @date 2023/1/30
// */
//public interface CpsiServer extends TwoPartyPto, SecurePto {
//    /**
//     * 返回PSI协议类型。
//     *
//     * @return PSI协议类型。
//     */
//    @Override
//    CpsiFactory.CpsiType getPtoType();
//
//    /**
//     * 初始化协议。
//     *
//     * @param maxServerElementSize 服务端最大元素数量。
//     * @param maxClientElementSize 客户端最大元素数量。
//     * @throws MpcAbortException 如果协议异常中止。
//     */
//    void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException;
//
//    /**
//     * 执行协议。
//     *
//     * @param serverElementSet  服务端元素集合。
//     * @param clientElementSize 客户端元素数量。
//     * @param elementByteLength 元素字节长度。
//     * @throws MpcAbortException 如果协议异常中止。
//     */
//    void psi(Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength) throws MpcAbortException;
//}