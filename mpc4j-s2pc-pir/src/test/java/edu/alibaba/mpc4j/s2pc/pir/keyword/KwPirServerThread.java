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
     * 关键词PIR协议服务端
     */
    private final KwPirServer pirServer;
    /**
     * 服务端关键词和标签映射
     */
    private final Map<ByteBuffer, ByteBuffer> serverKeywordLabelMap;
    /**
     * 标签字节长度
     */
    private final int labelByteLength;
    /**
     * 检索次数
     */
    private final int retrievalNumber;

    KwPirServerThread(KwPirServer pirServer, Map<ByteBuffer, ByteBuffer> serverKeywordLabelMap, int labelByteLength,
                      int retrievalNumber) {
        this.pirServer = pirServer;
        this.serverKeywordLabelMap = serverKeywordLabelMap;
        this.labelByteLength = labelByteLength;
        this.retrievalNumber = retrievalNumber;
    }

    @Override
    public void run() {
        try {
            pirServer.getRpc().connect();
            pirServer.init(serverKeywordLabelMap, labelByteLength);
            for (int i = 0; i < retrievalNumber; i++) {
                pirServer.pir();
            }
            pirServer.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}