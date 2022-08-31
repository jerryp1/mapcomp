package edu.alibaba.mpc4j.s2pc.pso.pmid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Set;

/**
 * 单方多集合PMID协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/05/10
 */
class OneSidePmidServerThread extends Thread {
    /**
     * PMID发送方
     */
    private final PmidServer<String> pmidServer;
    /**
     * 发送方集合
     */
    private final Set<String> serverElementSet;
    /**
     * 接收方元素数量
     */
    private final int clientSetSize;
    /**
     * 客户端重复元素上界
     */
    private final int maxClientK;
    /**
     * 客户端重复元素数量
     */
    private final int clientK;
    /**
     * PMID输出结果
     */
    private PmidPartyOutput<String> serverOutput;

    OneSidePmidServerThread(PmidServer<String> pmidServer,
                            Set<String> serverElementSet,
                            int clientSetSize, int maxClientK, int ClientK) {
        this.pmidServer = pmidServer;
        this.serverElementSet = serverElementSet;
        this.clientSetSize = clientSetSize;
        this.maxClientK = maxClientK;
        this.clientK = ClientK;
    }

    PmidPartyOutput<String> getServerOutput() {
        return serverOutput;
    }

    @Override
    public void run() {
        try {
            pmidServer.getRpc().connect();
            pmidServer.init(serverElementSet.size(), clientSetSize, maxClientK);
            serverOutput = pmidServer.pmid(serverElementSet, clientSetSize, clientK);
            pmidServer.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
