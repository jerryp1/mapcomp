package edu.alibaba.mpc4j.s2pc.pso.pmid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * PMID服务端。
 *
 * @author Weiran Liu
 * @date 2022/5/6
 */
public interface PmidServer<T> extends TwoPartyPto, SecurePto {
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
     * @param maxServerSetSize 服务端集合最大元素数量。
     * @param maxServerU       服务端最大重复元素上界。
     * @param maxClientSetSize 客户端集合最大元素数量。
     * @param maxClientU       客户端最大重复元素上界。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxServerSetSize, int maxServerU, int maxClientSetSize, int maxClientU) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param serverElementSet 服务端元素集合。
     * @param clientSetSize    客户端元素数量。
     * @param clientK          客户端重复元素上界。
     * @return 协议输出结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    PmidPartyOutput<T> pmid(Set<T> serverElementSet, int clientSetSize, int clientK) throws MpcAbortException;
}
