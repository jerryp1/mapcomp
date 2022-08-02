package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
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
     * 关键词PIR协议客户端
     */
    private final KwPirClient pirClient;
    /**
     * 客户端关键词集合
     */
    private final ArrayList<Set<ByteBuffer>> clientKeywordSets;
    /**
     * 标签字节长度
     */
    private final int labelByteLength;
    /**
     * PIR结果
     */
    private final Map<ByteBuffer, ByteBuffer> pirResult = new HashMap<>();
    /**
     * 检索次数
     */
    private final int retrievalNumber;

    KwPirClientThread(KwPirClient pirClient, ArrayList<Set<ByteBuffer>> clientKeywordSets, int labelByteLength) {
        this.pirClient = pirClient;
        this.clientKeywordSets = clientKeywordSets;
        this.labelByteLength = labelByteLength;
        this.retrievalNumber = clientKeywordSets.size();
    }

    public Map<ByteBuffer, ByteBuffer> getPirResult() {
        return pirResult;
    }

    @Override
    public void run() {
        try {
            // 随机选取
            pirClient.getRpc().connect();
            pirClient.init(labelByteLength);
            for (int i = 0; i < retrievalNumber; i++) {
                pirResult.putAll(pirClient.pir(clientKeywordSets.get(i)));
            }
            pirClient.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}