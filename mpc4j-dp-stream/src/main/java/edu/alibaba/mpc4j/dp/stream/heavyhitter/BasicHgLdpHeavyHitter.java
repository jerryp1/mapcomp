package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import com.google.common.base.Preconditions;

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

    BasicHgLdpHeavyHitter(Set<String> domainSet, int k, double windowEpsilon, Random heavyGuardianRandom) {
        super(domainSet, k, windowEpsilon, heavyGuardianRandom);
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
        // bias all counts
        for (Map.Entry<String, Double> entry : heavyGuardian.entrySet()) {
            String item = entry.getKey();
            double value = entry.getValue();
            value = value * (p - q);
            heavyGuardian.put(item, value);
        }
        warmupState = false;
    }

    @Override
    protected double updateCount(double count) {
        return count - currentNum * q;
    }

    @Override
    protected double debiasCount(double count) {
        return updateCount(count) / (p - q);
    }

    @Override
    public String randomize(Map<String, Double> currentDataStructure, String item, Random random) {
        Preconditions.checkArgument(domainSet.contains(item), "The input item is not in the domain: %s", item);
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
