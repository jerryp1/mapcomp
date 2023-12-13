package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgParty;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Triple test runner.
 *
 * @author Li Peng
 * @date 2023/8/3
 */
public class TripleTestRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripleTestRunner.class);
    /**
     * timer
     */
    private final StopWatch stopWatch;
    /**
     * party
     */
    private final Z2MtgParty party;
    /**
     * rpc
     */
    private final Rpc rpc;
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
    /**
     * test number.
     */
    private final int num;

    public TripleTestRunner(Z2MtgParty party, int totalRound, int num) {
        this.party = party;
        rpc = party.getRpc();
        stopWatch = new StopWatch();
        this.totalRound = totalRound;
        this.num = num;
    }

    public void init() throws MpcAbortException {
        party.init(num);
    }

    public void run() throws MpcAbortException {
        rpc.synchronize();
        rpc.reset();
        totalTime = 0L;
        totalPacketNum = 0L;
        totalPayloadByteLength = 0L;
        totalSendByteLength = 0L;
        // test repeatedly and record result.
        for (int round = 1; round <= totalRound; round++) {
            stopWatch.start();
            party.generate(num);
            stopWatch.stop();
            // record time
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            LOGGER.info("Round {}: Time = {}ms", round, time);
            totalTime += time;
        }
        totalPacketNum = rpc.getSendDataPacketNum();
        totalPayloadByteLength = rpc.getPayloadByteLength();
        totalSendByteLength = rpc.getSendByteLength();
        rpc.reset();
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

