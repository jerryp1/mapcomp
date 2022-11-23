package edu.alibaba.mpc4j.dp.stream.heavyhitter.hg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.HeavyHitterState;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.HeavyHitterStructure;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHeavyHitterFactory;

import java.util.*;

/**
 * Basic HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public class BasicHgLdpHeavyHitter extends AbstractHgLdpHeavyHitter {
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    protected final double p;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    protected final double q;

    public BasicHgLdpHeavyHitter(Set<String> domainSet, Random heavyGuardianRandom,
                                 int k, double windowEpsilon) {
        this(domainSet, 1, k, 0, heavyGuardianRandom, k, windowEpsilon);
    }

    public BasicHgLdpHeavyHitter(Set<String> domainSet, int w, int lambdaH, int primeIndex, Random heavyGuardianRandom,
                                 int k, double windowEpsilon) {
        super(domainSet, w, lambdaH, primeIndex, heavyGuardianRandom, k, windowEpsilon);
        double expWindowEpsilon = Math.exp(windowEpsilon);
        p = expWindowEpsilon / (expWindowEpsilon + d - 1);
        q = 1 / (expWindowEpsilon + d - 1);
    }

    @Override
    public LdpHeavyHitterFactory.LdpHeavyHitterType getType() {
        return LdpHeavyHitterFactory.LdpHeavyHitterType.BASIC_HG;
    }

    @Override
    public void stopWarmup() {
        Preconditions.checkArgument(
            heavyHitterState.equals(HeavyHitterState.WARMUP),
            "The heavy hitter must be %s: %s", HeavyHitterState.WARMUP, heavyHitterState
        );
        // bias all counts
        for (Map<String, Double> bucket : buckets) {
            for (Map.Entry<String, Double> entry : bucket.entrySet()) {
                String item = entry.getKey();
                double value = entry.getValue();
                value = value * (p - q);
                bucket.put(item, value);
            }
        }
        heavyHitterState = HeavyHitterState.STATISTICS;
    }

    @Override
    protected double updateCount(int bucketIndex, double count) {
        return count - currentNums[bucketIndex] * q;
    }

    @Override
    protected double debiasCount(int bucketIndex, double count) {
        return updateCount(bucketIndex, count) / (p - q);
    }

    @Override
    public String randomize(HeavyHitterStructure currentHeavyHitterStructure, String item, Random random) {
        Preconditions.checkArgument(
            currentHeavyHitterStructure instanceof HgHeavyHitterStructure,
            "The heavy hitter structure must be %s: %s",
            HgHeavyHitterStructure.class.getSimpleName(), currentHeavyHitterStructure.getClass().getSimpleName()
        );
        Preconditions.checkArgument(
            heavyHitterState.equals(HeavyHitterState.STATISTICS),
            "The heavy hitter must be %s: %s", HeavyHitterState.STATISTICS, heavyHitterState
        );
        Preconditions.checkArgument(
            domainSet.contains(item),
            "The input item is not in the domain: %s", item
        );
        // basic HeavyGuardian solution does not consider the current data structure
        double randomSample = random.nextDouble();
        // Randomly sample an integer in [0, d)
        int randomIndex = random.nextInt(d);
        if (randomSample > p - q) {
            // answer a random item
            return domainArrayList.get(randomIndex);
        } else {
            // answer the true item
            return item;
        }
    }
}
