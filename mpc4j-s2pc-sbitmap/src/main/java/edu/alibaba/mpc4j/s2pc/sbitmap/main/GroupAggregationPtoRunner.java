package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggParty;
import edu.alibaba.mpc4j.s2pc.sbitmap.pto.GroupAggInputData;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Sbitmap protocol runner.
 *
 * @author Li Peng
 * @date 2023/8/3
 */
public class GroupAggregationPtoRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupAggregationPtoRunner.class);
    /**
     * timer
     */
    private final StopWatch stopWatch;
    /**
     * party
     */
    private final GroupAggParty party;
    /**
     * rpc
     */
    private final Rpc slaveRpc;
    /**
     * config
     */
    private final GroupAggConfig groupAggConfig;
    /**
     * total round
     */
    private final int totalRound;
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

    private GroupAggInputData groupAggInputData;

    private Properties properties;

    public GroupAggregationPtoRunner(GroupAggParty party, GroupAggConfig groupAggConfig, int totalRound,
                                     GroupAggInputData groupAggInputData, Properties properties) {
        this.party = party;
        slaveRpc = party.getRpc();
        this.groupAggConfig = groupAggConfig;
        stopWatch = new StopWatch();
        this.totalRound = totalRound;
//        this.ownDataFrame = ownDataFrame;
        this.groupAggInputData = groupAggInputData;
        this.properties = properties;
    }

    public void init() throws MpcAbortException {
        party.init(properties);
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
            party.groupAgg(groupAggInputData.getGroups(), groupAggInputData.getAggs(), groupAggInputData.getE());
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
        this.party.destroy();
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

