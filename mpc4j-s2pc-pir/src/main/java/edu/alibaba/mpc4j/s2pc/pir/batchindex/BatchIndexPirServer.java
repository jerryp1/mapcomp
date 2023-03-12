package edu.alibaba.mpc4j.s2pc.pir.batchindex;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * 批量索引PIR协议服务端接口。
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public interface BatchIndexPirServer extends TwoPartyPto {

    /**
     * 初始化协议。
     *
     * @param elementArrayList 元素列表。
     * @param elementBitLength 元素比特长度。
     * @param maxRetrievalSize 最大查询数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(ArrayList<ByteBuffer> elementArrayList, int elementBitLength, int maxRetrievalSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @throws MpcAbortException 如果协议异常中止。
     */
    void pir() throws MpcAbortException;
}
