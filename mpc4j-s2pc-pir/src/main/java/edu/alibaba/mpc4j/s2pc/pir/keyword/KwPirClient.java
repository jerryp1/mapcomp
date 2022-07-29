package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

import static edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirFactory.PirType;

/**
 * 关键词索引PIR协议客户端接口。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public interface KwPirClient extends TwoPartyPto, SecurePto {

    @Override
    PirType getPtoType();

    /**
     * 初始化协议。
     *
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init() throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param serverElementSet     服务端元素集合。
     * @param elementByteLength    元素字节长度。
     * @param labelByteLength      标签字节长度。
     * @param retrievalElementSize 单次查询元素数量。
     * @param retrievalNumber      查询次数
     * @return 查询元素和标签映射。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Map<ByteBuffer, ByteBuffer> pir(Set<ByteBuffer> serverElementSet, int elementByteLength, int labelByteLength,
                                    int retrievalElementSize, int retrievalNumber) throws MpcAbortException;
}
