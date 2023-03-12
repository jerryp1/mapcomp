package edu.alibaba.mpc4j.s2pc.pir.batchindex;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;

/**
 * 批量索引PIR协议客户端接口。
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public interface BatchIndexPirClient extends TwoPartyPto {

    /**
     * 初始化协议。
     *
     * @param serverElementSize 服务端元素数量。
     * @param elementBitLength  元素比特长度。
     * @param maxRetrievalSize  最大查询数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int serverElementSize, int elementBitLength, int maxRetrievalSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param indices 检索值。
     * @return 检索结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Map<Integer, ByteBuffer> pir(ArrayList<Integer> indices) throws MpcAbortException;
}
