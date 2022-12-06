package edu.alibaba.mpc4j.s2pc.pjc.bitmap.main;

import org.apache.commons.lang3.time.StopWatch;

/**
 * Bitmap执行方抽象类
 *
 * @author Li Peng  
 * @date 2022/12/1
 */
public abstract class AbstractBitmapRunner implements BitmapRunner {
    /**
     * 计时器
     */
    protected final StopWatch stopWatch;
    /**
     * 总执行轮数
     */
    protected final int totalRound;
    /**
     * 总时间
     */
    protected long totalTime;

    /**
     * 数据包数量
     */
    protected long totalPacketNum;
    /**
     * 负载字节长度
     */
    protected long totalPayloadByteLength;
    /**
     * 发送字节长度
     */
    protected long totalSendByteLength;

    public AbstractBitmapRunner(int totalRound) {
        stopWatch = new StopWatch();
        this.totalRound = totalRound;
    }

    protected void reset() {
        totalTime = 0;
        totalPacketNum = 0;
        totalPayloadByteLength = 0;
        totalSendByteLength = 0;
    }

    @Override
    public double getTime() {
        return (double) totalTime / totalRound;
    }

    @Override
    public long getPacketNum() {
        return totalPacketNum / totalRound;
    }

    @Override
    public long getPayloadByteLength() {
        return totalPayloadByteLength / totalRound;
    }

    @Override
    public long getSendByteLength() {
        return totalSendByteLength / totalRound;
    }


}
