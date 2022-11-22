package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.SecureBernoulliSampler;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Relaxed Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
 *
 * @author Weiran Liu
 * @date 2022/11/20
 */
public class RelaxHhgLdpHeavyHitter extends AdvHhgLdpHeavyHitter {
    /**
     * default α
     */
    private static final double DEFAULT_ALPHA = 1.0 / 2;

    RelaxHhgLdpHeavyHitter(Set<String> domainSet, int k, double windowEpsilon, Random hgRandom) {
        this(domainSet, k, windowEpsilon, DEFAULT_ALPHA, hgRandom);
    }

    RelaxHhgLdpHeavyHitter(Set<String> domainSet, int k, double windowEpsilon, double alpha, Random hgRandom) {
        super(domainSet, k, windowEpsilon, alpha, hgRandom);
        // recompute probabilities
        double alphaWindowEpsilon = windowEpsilon * alpha;
        double remainedWindowEpsilon = windowEpsilon - alphaWindowEpsilon;
        // compute p1 and p1
        double expAlphaWindowEpsilon = Math.exp(alphaWindowEpsilon);
        p1 = expAlphaWindowEpsilon / (expAlphaWindowEpsilon + 1);
        q1 = 1 / (expAlphaWindowEpsilon + 1);
        // compute p2 and q2
        double expWindowEpsilon = Math.exp(windowEpsilon);
        p2 = expWindowEpsilon / (expWindowEpsilon + k - 1);
        q2 = 1 / (expWindowEpsilon + k - 1);
        // compute p3 and q3
        double expRemainedWindowEpsilon = Math.exp(remainedWindowEpsilon);
        p3 = expRemainedWindowEpsilon / (expRemainedWindowEpsilon + d - k - 1);
        q3 = 1 / (expRemainedWindowEpsilon + d - k - 1);
    }

    @Override
    public LdpHeavyHitterFactory.LdpHeavyHitterType getType() {
        return LdpHeavyHitterFactory.LdpHeavyHitterType.RELAX_HG;
    }

    @Override
    protected double updateCount(double count) {
        return count - currentNum * gammaH * p1 * q2;
    }

    @Override
    public String randomize(Map<String, Double> currentHeavyGuardian, String item, Random random) {
        Preconditions.checkArgument(
            heavyHitterState.equals(HeavyHitterState.STATISTICS),
            "The heavy hitter must be %s: %s", HeavyHitterState.STATISTICS, heavyHitterState
        );
        Preconditions.checkArgument(
            domainSet.contains(item),
            "The input item is not in the domain: %s", item
        );
        Preconditions.checkArgument(
            currentHeavyGuardian.size() == k,
            "Current HeavyGuardian size must be equal to k: %s", currentHeavyGuardian.size()
        );
        if (d == k) {
            return userMechanism2(currentHeavyGuardian.keySet(), item, random);
        }
        // M1
        boolean flag = userMechanism1(currentHeavyGuardian.keySet(), item, random);
        // M2
        if (flag) {
            // v is determined as hot
            return userMechanism2(currentHeavyGuardian.keySet(), item, random);
        } else {
            // v is determined as code
            return userMechanism3(currentHeavyGuardian, item, random);
        }
    }

    @Override
    protected boolean userMechanism1(Set<String> currentHeavyHitterSet, String item, Random random) {
        if (!currentHeavyHitterSet.contains(item)) {
            return false;
        } else {
            // Let b = Ber(e^ε_1 / (e^ε_1 + 1))
            SecureBernoulliSampler bernoulliSampler = new SecureBernoulliSampler(random, p1);
            return bernoulliSampler.sample();
        }
    }

    @Override
    protected String userMechanism2(Set<String> currentHeavyHitterSet, String item, Random random) {
        ArrayList<String> currentHeavyHitterArrayList = new ArrayList<>(currentHeavyHitterSet);
        double randomSample = random.nextDouble();
        // Randomly sample an integer in [0, k)
        int randomIndex = random.nextInt(currentHeavyHitterSet.size());
        // if v ∈ HG, use random response
        if (randomSample > p2 - q2) {
            // answer a random item in the current heavy hitter
            return currentHeavyHitterArrayList.get(randomIndex);
        } else {
            // answer the true item
            return item;
        }
    }

    @Override
    public Map<String, Double> responseHeavyHitters() {
        // we only need to iterate items in the budget
        return heavyGuardian.keySet().stream().collect(Collectors.toMap(item -> item, this::response));
    }
}
