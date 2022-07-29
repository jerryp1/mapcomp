package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * 关键词索引PIR协议服务端线程。
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
public class KwPirServerThread extends Thread {
    /**
     * Keyword PIR协议服务端
     */
    private final KwPirServer pirServer;
    /**
     * 服务端元素和标签集合
     */
    private final Map<ByteBuffer, ByteBuffer> serverElementMap;
    /**
     * 元素字节长度
     */
    private final int elementByteLength;
    /**
     * 标签字节长度
     */
    private final int labelByteLength;

    private final int retrievalNumber;

    KwPirServerThread(KwPirServer pirServer, Map<ByteBuffer, ByteBuffer> serverElementMap, int elementByteLength,
                      int labelByteLength, int retrievalNumber) {
        this.pirServer = pirServer;
        this.serverElementMap = serverElementMap;
        this.elementByteLength = elementByteLength;
        this.labelByteLength = labelByteLength;
        this.retrievalNumber = retrievalNumber;
    }

    @Override
    public void run() {
        try {
            pirServer.getRpc().connect();
            pirServer.init(serverElementMap, elementByteLength, labelByteLength);
            pirServer.pir(retrievalNumber);
            pirServer.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
