package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Map;

import static edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirFactory.PirType;

/**
 * 关键词索引PIR协议服务端接口。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public interface KwPirServer extends TwoPartyPto, SecurePto {

    @Override
    PirType getPtoType();

    /**
     * 初始化协议。
     *
     * @param serverElementMap  服务端关键字和标签映射。
     * @param keywordByteLength 关键字字节长度。
     * @param labelByteLength   标签字节长度。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(Map<ByteBuffer, ByteBuffer> serverElementMap, int keywordByteLength, int labelByteLength)
        throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param retrievalNum 查询次数。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void pir(int retrievalNum) throws MpcAbortException;

}
