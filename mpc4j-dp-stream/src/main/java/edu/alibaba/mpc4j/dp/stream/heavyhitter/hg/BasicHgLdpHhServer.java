package edu.alibaba.mpc4j.dp.stream.heavyhitter.hg;

import edu.alibaba.mpc4j.dp.stream.heavyhitter.config.HgLdpHhServerConfig;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.utils.EmptyLdpHhServerContext;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.utils.LdpHhServerContext;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhServerState;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhFactory;

import java.util.*;

/**
 * Basic Heavy Hitter server with Local Differential Privacy based on HeavyGuardian.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public class BasicHgLdpHhServer extends AbstractHgLdpHhServer {
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    private final double q;

    public BasicHgLdpHhServer(HgLdpHhServerConfig serverConfig) {
        super(serverConfig);
        double expWindowEpsilon = Math.exp(windowEpsilon);
        p = expWindowEpsilon / (expWindowEpsilon + d - 1);
        q = 1 / (expWindowEpsilon + d - 1);
    }

    @Override
    public LdpHhFactory.LdpHhType getType() {
        return LdpHhFactory.LdpHhType.BASIC_HG;
    }

    @Override
    public void stopWarmup() {
        checkState(LdpHhServerState.WARMUP);
        // bias all counts
        for (Map<String, Double> bucket : buckets) {
            for (Map.Entry<String, Double> entry : bucket.entrySet()) {
                String item = entry.getKey();
                double value = entry.getValue();
                value = value * (p - q);
                bucket.put(item, value);
            }
        }
        ldpHhServerState = LdpHhServerState.STATISTICS;
    }

    @Override
    public LdpHhServerContext getServerContext() {
        return new EmptyLdpHhServerContext();
    }

    @Override
    protected double updateCount(int bucketIndex, double count) {
        return count - currentNums[bucketIndex] * q;
    }

    @Override
    protected double debiasCount(int bucketIndex, double count) {
        return updateCount(bucketIndex, count) / (p - q);
    }
}
