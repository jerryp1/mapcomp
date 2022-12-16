package edu.alibaba.mpc4j.s2pc.pjc.bitmap.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Bitmap执行接口
 *
 * @author Li Peng  
 * @date 2022/12/1
 */
public interface BitmapRunner {
    /**
     * 执行协议。
     *
     * @throws MpcAbortException 如果协议异常中止。
     */
    void run() throws MpcAbortException;

    /**
     * 返回（平均）执行时间。
     *
     * @return 执行时间。
     */
    double getTime();

    /**
     * 返回（平均）数据包数量。
     *
     * @return 数据包数量。
     */
    long getPacketNum();

    /**
     * 返回（平均）负载字节长度。
     *
     * @return 负载字节长度。
     */
    long getPayloadByteLength();

    /**
     * 返回（平均）发送字节长度。
     *
     * @return 发送字节长度。
     */
    long getSendByteLength();
}
