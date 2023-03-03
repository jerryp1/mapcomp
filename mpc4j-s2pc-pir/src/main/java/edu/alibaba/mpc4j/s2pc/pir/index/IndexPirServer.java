package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * 索引PIR协议服务端接口。
 *
 * @author Liqiang Peng
 * @date 2022/8/10
 */
public interface IndexPirServer extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param indexPirParams    索引PIR协议参数。
     * @param elementArrayList  元素列表。
     * @param elementByteLength 元素字节长度。
     */
    void init(IndexPirParams indexPirParams, ArrayList<ByteBuffer> elementArrayList, int elementByteLength);

    /**
     * 初始化协议。
     *
     * @param elementArrayList  元素列表。
     * @param elementByteLength 元素字节长度。
     */
    void init(ArrayList<ByteBuffer> elementArrayList, int elementByteLength);

    /**
     * 执行协议。
     *
     * @throws MpcAbortException 如果协议异常中止。
     */
    void pir() throws MpcAbortException;
}
