package edu.alibaba.mpc4j.dp.stream.heavyhitter.hg;

import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhServerState;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhFactory;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.config.HhgLdpHhServerConfig;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.utils.HgLdpHhServerContext;

import java.util.*;

/**
 * Advanced Heavy Hitter server with Local Differential Privacy based on Hot HeavyGuardian.
 *
 * @author Weiran Liu
 * @date 2022/11/19
 */
public class AdvHhgLdpHhServer extends AbstractHgLdpHhServer implements HhgLdpHhServer {
    /**
     * the privacy parameter allocation parameter α
     */
    private final double alpha;
    /**
     * p1 = e^ε_1 / (e^ε_1 + 1)
     */
    protected double p1;
    /**
     * q1 = 1 / (e^ε_1 + 1)
     */
    protected double q1;
    /**
     * p2 = e^ε_2 / (e^ε_2 + λ_h - 1)
     */
    protected double p2;
    /**
     * q2 = 1 / (e^ε_2 + λ_h - 1)
     */
    protected double q2;
    /**
     * γ_h, proportion of hot items
     */
    protected double gammaH;

    public AdvHhgLdpHhServer(HhgLdpHhServerConfig serverConfig) {
        super(serverConfig);
        alpha = serverConfig.getAlpha();
        double alphaWindowEpsilon = windowEpsilon * alpha;
        double remainedWindowEpsilon = windowEpsilon - alphaWindowEpsilon;
        // compute p1 and p1
        double expAlphaWindowEpsilon = Math.exp(alphaWindowEpsilon);
        p1 = expAlphaWindowEpsilon / (expAlphaWindowEpsilon + 1);
        q1 = 1 / (expAlphaWindowEpsilon + 1);
        // compute p2 and q2
        double expRemainedWindowEpsilon = Math.exp(remainedWindowEpsilon);
        p2 = expRemainedWindowEpsilon / (expRemainedWindowEpsilon + lambdaH - 1);
        q2 = 1 / (expRemainedWindowEpsilon + lambdaH - 1);
        ldpHhServerState = LdpHhServerState.WARMUP;
        gammaH = 0;
    }

    @Override
    public LdpHhFactory.LdpHhType getType() {
        return LdpHhFactory.LdpHhType.ADVAN_HG;
    }

    @Override
    public void stopWarmup() {
        checkState(LdpHhServerState.WARMUP);
        double hotNum = 0;
        for (int budgetIndex = 0; budgetIndex < w; budgetIndex++) {
            Map<String, Double> budget = buckets.get(budgetIndex);
            // bias all counts and calculate λ
            for (Map.Entry<String, Double> entry : budget.entrySet()) {
                String item = entry.getKey();
                double value = entry.getValue();
                hotNum += value;
                value = value * p1 * (p2 - q2);
                budget.put(item, value);
            }
            // note that here the bucket may contain # of elements that is less than lambdaH
        }
        gammaH = hotNum / num;
        assert gammaH >= 0 && gammaH <= 1 : "γ_h must be in range [0, 1]: " + gammaH;
        ldpHhServerState = LdpHhServerState.STATISTICS;
    }

    @Override
    protected double updateCount(int bucketIndex, double count) {
        return count - currentNums[bucketIndex] * (gammaH * p1 * q2 + (1 - gammaH) * q1 / k);
    }

    @Override
    protected double debiasCount(int bucketIndex, double count) {
        return updateCount(bucketIndex, count) / (p1 * (p2 - q2));
    }

    @Override
    public double getAlpha() {
        return alpha;
    }

    @Override
    public HgLdpHhServerContext getServerContext() {
        return new HgLdpHhServerContext(buckets);
    }
}
