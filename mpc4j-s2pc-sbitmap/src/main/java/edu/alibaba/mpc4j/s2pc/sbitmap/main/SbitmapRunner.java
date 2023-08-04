package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Sbitmap Runner.
 *
 * @author Li Peng
 * @date 2023/08/02
 */
public interface SbitmapRunner {
    /**
     * Run protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void run() throws MpcAbortException;

    /**
     * Return average running time。
     *
     * @return running。
     */
    double getTime();

    /**
     * Return average number of package。
     *
     * @return number of package。
     */
    long getPacketNum();

    /**
     * Return average payload byte length.
     *
     * @return payload byte length.
     */
    long getPayloadByteLength();

    /**
     * Return sender byte length.
     *
     * @return sender byte length.
     */
    long getSendByteLength();
}





