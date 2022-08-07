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
public interface KwPirClient<T> extends TwoPartyPto, SecurePto {

    @Override
    PirType getPtoType();

    /**
     * 初始化协议。
     *
     * @param labelByteLength    标签字节长度。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int labelByteLength) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param clientKeywordSet   客户端关键词集合。
     * @return 查询元素和标签映射。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Map<T, ByteBuffer> pir(Set<T> clientKeywordSet) throws MpcAbortException;
}
