package edu.alibaba.mpc4j.s2pc.pso.pmid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Map;

/**
 * PMID协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/5/6
 */
public interface PmidClient<T> extends TwoPartyPto, SecurePto {
    /**
     * 返回PMID协议类型。
     *
     * @return PMID协议类型。
     */
    @Override
    PmidFactory.PmidType getPtoType();

    /**
     * 初始化协议。
     *
     * @param maxClientSetSize 客户端最大元素数量。
     * @param maxClientU       客户端最大重复元素上界。
     * @param maxServerSetSize 服务端最大元素数量。
     * @param maxServerU       服务端最大重复元素上界。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxClientSetSize, int maxClientU, int maxServerSetSize, int maxServerU) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param clientElementMap 客户端元素与频次映射。
     * @param serverSetSize    服务端元素数量。
     * @return 协议输出结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    PmidPartyOutput<T> pmid(Map<T, Integer> clientElementMap, int serverSetSize) throws MpcAbortException;
}
