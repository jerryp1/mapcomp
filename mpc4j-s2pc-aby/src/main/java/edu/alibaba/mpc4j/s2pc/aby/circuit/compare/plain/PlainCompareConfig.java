package edu.alibaba.mpc4j.s2pc.aby.circuit.compare.plain;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * 比较协议配置项。
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/11
 */
public interface PlainCompareConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    PlainCompareFactory.PlainCompareType getPtoType();

    /**
     * 返回底层协议支持的最大比特数量。
     *
     * @return 底层协议支持的最大比特数量。
     */
    int maxAllowBitNum();
}

