package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

/**
 * 关键词索引PIR协议客户端线程。
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
public class KwPirClientThread extends Thread {
    /**
     * Keyword PIR协议客户端
     */
    private final KwPirClient pirClient;
    /**
     * 服务端元素集合
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * 元素字节长度
     */
    private final int elementByteLength;
    /**
     * 标签字节长度
     */
    private final int labelByteLength;
    /**
     * 查询元素数目
     */
    private final int retrievalElementSize;
    /**
     * PIR结果
     */
    private Map<ByteBuffer, ByteBuffer> pirResult;

    private final int retrievalNumber;

    KwPirClientThread(KwPirClient pirClient, Set<ByteBuffer> serverElementSet, int elementByteLength, int labelByteLength,
                      int retrievalElementSize, int retrievalNumber) {
        this.pirClient = pirClient;
        this.serverElementSet = serverElementSet;
        this.elementByteLength = elementByteLength;
        this.labelByteLength = labelByteLength;
        this.retrievalElementSize = retrievalElementSize;
        this.retrievalNumber = retrievalNumber;
    }

    public Map<ByteBuffer, ByteBuffer> getPirResult() {
        return pirResult;
    }

    @Override
    public void run() {
        try {
            // 随机选取
            pirClient.getRpc().connect();
            pirClient.init();
            pirResult = pirClient.pir(serverElementSet, elementByteLength, labelByteLength, retrievalElementSize,
                retrievalNumber);
            pirClient.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
