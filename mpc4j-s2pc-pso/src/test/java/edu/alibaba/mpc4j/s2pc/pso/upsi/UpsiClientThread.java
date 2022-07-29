package edu.alibaba.mpc4j.s2pc.pso.upsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;


/**
 * 非平衡PSI协议接收方线程。
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
public class UpsiClientThread extends Thread {
    /**
     * 非平衡PSI协议接收方
     */
    private final UpsiClient upsiClient;
    /**
     * 接收方集合
     */
    private final Set<ByteBuffer> clientElementSet;
    /**
     * 元素字节长度
     */
    private final int elementByteLength;
    /**
     * 客户端交集
     */
    private Set<ByteBuffer> intersectionSet;

    UpsiClientThread(UpsiClient upsiClient, Set<ByteBuffer> clientElementSet, int elementByteLength) {
        this.upsiClient = upsiClient;
        this.clientElementSet = clientElementSet;
        this.elementByteLength = elementByteLength;
    }

    Set<ByteBuffer> getIntersectionSet() {
        return intersectionSet;
    }

    @Override
    public void run() {
        try {
            upsiClient.getRpc().connect();
            upsiClient.init();
            intersectionSet = upsiClient.psi(clientElementSet, elementByteLength);
            upsiClient.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
