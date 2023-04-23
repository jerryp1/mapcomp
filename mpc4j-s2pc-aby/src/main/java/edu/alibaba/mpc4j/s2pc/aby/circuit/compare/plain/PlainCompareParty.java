package edu.alibaba.mpc4j.s2pc.aby.circuit.compare.plain;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * 比较协议参与方接口。
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/11
 */
public interface PlainCompareParty extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxBitNum 最大比特数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxBitNum) throws MpcAbortException;

    /**
     * 将数据x与对方数据y进行比较
     *
     * @param x 已方输入数据
     * @return x是否小于对方数据y
     * @throws MpcAbortException 如果协议异常中止。
     */
    boolean lessThan(int x) throws MpcAbortException;
}