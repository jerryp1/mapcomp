package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * XPIR协议客户端线程。
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class XPirClientThread extends Thread {
    /**
     * XPIR协议客户端
     */
    private final Mbfk16IndexPirClient client;
    /**
     * XPIR参数
     */
    private final Mbfk16IndexPirParams indexPirParams;
    /**
     * 元素字节长度
     */
    private final int elementByteLength;
    /**
     * 检索值列表
     */
    private final ArrayList<Integer> retrievalIndexList;
    /**
     * 服务端元素数量
     */
    private final int serverElementSize;
    /**
     * 检索次数
     */
    private final int repeatTime;
    /**
     * 索引结果
     */
    private final ArrayList<ByteBuffer> indexPirResult;

    XPirClientThread(Mbfk16IndexPirClient client, Mbfk16IndexPirParams indexPirParams,
                     ArrayList<Integer> retrievalIndexList, int serverElementSize, int elementByteLength, int repeatTime) {
        assert repeatTime == retrievalIndexList.size();
        this.client = client;
        this.indexPirParams = indexPirParams;
        this.retrievalIndexList = retrievalIndexList;
        this.serverElementSize = serverElementSize;
        this.elementByteLength = elementByteLength;
        this.repeatTime = repeatTime;
        indexPirResult = new ArrayList<>(repeatTime);
    }

    public ArrayList<ByteBuffer> getRetrievalResult() {
        return indexPirResult;
    }

    @Override
    public void run() {
        try {
            client.init(indexPirParams, serverElementSize, elementByteLength);
            client.getRpc().synchronize();
            for (int i = 0; i < repeatTime; i++) {
                indexPirResult.add(ByteBuffer.wrap(client.pir(retrievalIndexList.get(i))));
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
