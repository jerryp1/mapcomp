package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.sbitmap.pto.SbitmapPtoParty;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;

import java.util.concurrent.TimeUnit;

/**
 * Sbitmap protocol runner.
 *
 * @author Li Peng
 * @date 2023/8/3
 */
public class SbitmapPtoRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SbitmapPtoRunner.class);
    /**
     * timer
     */
    private final StopWatch stopWatch;
    /**
     * party
     */
    private final SbitmapPtoParty slave;
    /**
     * rpc
     */
    private final Rpc slaveRpc;
    /**
     * config
     */
    private final SbitmapConfig slaveConfig;
    /**
     * total round
     */
    private final int totalRound;
    /**
     * dataset
     */
    private final DataFrame ownDataFrame;
    /**
     * total time
     */
    private long totalTime;
    /**
     * total package num
     */
    private long totalPacketNum;
    /**
     * total package length
     */
    private long totalPayloadByteLength;
    /**
     * total send byte length
     */
    private long totalSendByteLength;

    public SbitmapPtoRunner(SbitmapPtoParty slave, SbitmapConfig slaveConfig, int totalRound,
                            DataFrame ownDataFrame) {
        this.slave = slave;
        slaveRpc = slave.getRpc();
        this.slaveConfig = slaveConfig;
        stopWatch = new StopWatch();
        this.totalRound = totalRound;
        this.ownDataFrame = ownDataFrame;
    }

    public void init() throws MpcAbortException {

    }

    public void run() throws MpcAbortException {
        slaveRpc.synchronize();
        slaveRpc.reset();
        totalTime = 0L;
        totalPacketNum = 0L;
        totalPayloadByteLength = 0L;
        totalSendByteLength = 0L;
        // 重复实验，记录数据
        for (int round = 1; round <= totalRound; round++) {
            stopWatch.start();
            slave.run(ownDataFrame, slaveConfig);
            stopWatch.stop();
            // record time
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            LOGGER.info("Round {}: Slave Time = {}ms", round, time);
            totalTime += time;
        }
        totalPacketNum = slaveRpc.getSendDataPacketNum();
        totalPayloadByteLength = slaveRpc.getPayloadByteLength();
        totalSendByteLength = slaveRpc.getSendByteLength();
        slaveRpc.reset();
    }

    public void stop() {
        this.slave.stop();
    }

    public double getTime() {
        return (double) totalTime / totalRound;
    }

    public long getPacketNum() {
        return totalPacketNum / totalRound;
    }

    public long getPayloadByteLength() {
        return totalPayloadByteLength / totalRound;
    }

    public long getSendByteLength() {
        return totalSendByteLength / totalRound;
    }
}
