package edu.alibaba.mpc4j.s2pc.sbitmap.pto;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapConfig;
import smile.data.DataFrame;

/**
 * Sbitmap protocol interface.
 *
 * @author Li Peng
 * @date 2023/8/4
 */
public interface SbitmapPtoParty {
    /**
     * 初始化协议。
     *
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init() throws MpcAbortException;

    /**
     * Run protocol.
     *
     * @param dataFrame
     * @param config
     */
    void run(DataFrame dataFrame, SbitmapConfig config) throws MpcAbortException;

    /**
     * Get rpc.
     *
     * @return rpc.
     */
    Rpc getRpc();

    /**
     * Stop.
     */
    void stop();
}
