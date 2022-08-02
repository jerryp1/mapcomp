package edu.alibaba.mpc4j.s2pc.pso.upsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;


/**
 * 非平衡PSI协议客户端（接收方）线程。
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
public class UpsiClientThread extends Thread {
    /**
     * 非平衡PSI协议客户端
     */
    private final UpsiClient upsiClient;
    /**
     * 客户端集合
     */
    private final Set<ByteBuffer> clientElementSet;
    /**
     * 交集
     */
    private Set<ByteBuffer> intersectionSet;

    UpsiClientThread(UpsiClient upsiClient, Set<ByteBuffer> clientElementSet) {
        this.upsiClient = upsiClient;
        this.clientElementSet = clientElementSet;
    }

    Set<ByteBuffer> getIntersectionSet() {
        return intersectionSet;
    }

    @Override
    public void run() {
        try {
            upsiClient.getRpc().connect();
            upsiClient.init();
            intersectionSet = upsiClient.psi(clientElementSet);
            upsiClient.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
