package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpServerState;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HgHhLdpServerContext;

import java.util.Map;
import java.util.stream.IntStream;

/**
 * Direct Hot HeavyGuardian-based Heavy Hitter LDP server.
 *
 * @author Weiran Liu
 * @date 2023/3/20
 */
public class DirectHhgHhLdpServer extends AbstractHgHhLdpServer {
    /**
     * p= e^ε / (e^ε + (λ_h + 1) - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + (λ_h + 1) - 1)
     */
    private final double q;
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    private final double[] ps;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    private final double[] qs;

    public DirectHhgHhLdpServer(HhLdpConfig config) {
        super(config);
        // compute p = e^ε / (e^ε + ( + 1) - 1)
        double expWindowEpsilon = Math.exp(windowEpsilon);
        p = expWindowEpsilon / (expWindowEpsilon + (lambdaH + 1) - 1);
        q = 1 / (expWindowEpsilon + (lambdaH + 1) - 1);
        // compute ps and qs
        ps = IntStream.range(0, w)
            .mapToDouble(bucketIndex -> {
                int bucketD = bucketDs[bucketIndex];
                return expWindowEpsilon / (expWindowEpsilon + bucketD - 1);
            })
            .toArray();
        qs = IntStream.range(0, w)
            .mapToDouble(bucketIndex -> {
                int bucketD = bucketDs[bucketIndex];
                return 1 / (expWindowEpsilon + bucketD - 1);
            })
            .toArray();
        hhLdpServerState = HhLdpServerState.WARMUP;
    }

    @Override
    public void stopWarmup() {
        checkState(HhLdpServerState.WARMUP);
        for (int budgetIndex = 0; budgetIndex < w; budgetIndex++) {
            Map<String, Double> budget = buckets.get(budgetIndex);
            // bias all counts
            for (Map.Entry<String, Double> entry : budget.entrySet()) {
                String item = entry.getKey();
                double value = entry.getValue();
                value = value * (p - q);
                budget.put(item, value);
            }
            // note that here the bucket may contain # of elements that is less than lambdaH
        }
        hhLdpServerState = HhLdpServerState.STATISTICS;
    }

    @Override
    protected double updateCount(int bucketIndex, double count) {
        double debiasWeakCount = (currentWeakNums[bucketIndex]- qs[bucketIndex] * currentWeakNums[bucketIndex])
            / (ps[bucketIndex] - qs[bucketIndex]) * (p - q);
        return count - currentWeakNums[bucketIndex] + debiasWeakCount - currentStrongNums[bucketIndex] * q;
    }

    @Override
    protected double debiasCount(int bucketIndex, double count) {
        return updateCount(bucketIndex, count) / (p - q);
    }

    @Override
    public HgHhLdpServerContext getServerContext() {
        return new HgHhLdpServerContext(buckets);
    }
}
