package edu.alibaba.mpc4j.s2pc.groupagg.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * Triple test runner.
 *
 * @author Li Peng
 * @date 2023/8/3
 */
public class TupleTestRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(TupleTestRunner.class);
    /**
     * timer
     */
    private final StopWatch stopWatch;
    /**
     * party
     */
    private final Bit2aParty party;
    /**
     * rpc
     */
    private final Rpc slaveRpc;
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
     * Test number.
     */
    private final int num;

    public TupleTestRunner(Bit2aParty party, int totalRound, int num) {
        this.party = party;
        slaveRpc = party.getRpc();
        stopWatch = new StopWatch();
        this.totalRound = totalRound;
        this.num = num;
    }

    public void init() throws MpcAbortException {
        party.init(64, num);
    }

    public void run() throws MpcAbortException {
        slaveRpc.synchronize();
        slaveRpc.reset();
        totalTime = 0L;
        totalPacketNum = 0L;
        totalPayloadByteLength = 0L;
        totalSendByteLength = 0L;
        // test repeatedly and record result.
        for (int round = 1; round <= totalRound; round++) {
            stopWatch.start();
            party.bit2a(SquareZ2Vector.createRandom(num, new SecureRandom()));
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

