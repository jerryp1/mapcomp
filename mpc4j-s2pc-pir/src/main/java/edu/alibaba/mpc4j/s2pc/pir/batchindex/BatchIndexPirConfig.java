package edu.alibaba.mpc4j.s2pc.pir.batchindex;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * 批量索引PIR协议配置项接口。
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public interface BatchIndexPirConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    BatchIndexPirFactory.BatchIndexPirType getProType();
}
