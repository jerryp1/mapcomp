package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.SecureBernoulliSampler;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
 *
 * @author Weiran Liu
 * @date 2022/11/19
 */
public class AdvHhgLdpHeavyHitter extends AbstractHgLdpHeavyHitter implements HhgLdpHeavyHitter {
    /**
     * default α
     */
    protected static final double DEFAULT_ALPHA = 1.0 / 3;
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
     * p2 = e^ε_2 / (e^ε_2 + k - 1)
     */
    protected double p2;
    /**
     * q2 = 1 / (e^ε_2 + k - 1)
     */
    protected double q2;
    /**
     * p3 = e^ε_3 / (e^ε_3 + d - k - 1)
     */
    protected double p3;
    /**
     * q3 = 1 / (e^ε_3 + d - k - 1)
     */
    protected double q3;
    /**
     * γ_h, proportion of hot items
     */
    protected double gammaH;

    AdvHhgLdpHeavyHitter(Set<String> domainSet, int k, double windowEpsilon, Random hgRandom) {
        this(domainSet, k, windowEpsilon, DEFAULT_ALPHA, hgRandom);
    }

    AdvHhgLdpHeavyHitter(Set<String> domainSet, int k, double windowEpsilon, double alpha, Random hgRandom) {
        super(domainSet, k, windowEpsilon, hgRandom);
        Preconditions.checkArgument(alpha > 0 && alpha < 1, "α must be in range (0, 1)", alpha);
        this.alpha = alpha;
        double alphaWindowEpsilon = windowEpsilon * alpha;
        double remainedWindowEpsilon = windowEpsilon - alphaWindowEpsilon;
        // compute p1 and p1
        double expAlphaWindowEpsilon = Math.exp(alphaWindowEpsilon);
        p1 = expAlphaWindowEpsilon / (expAlphaWindowEpsilon + 1);
        q1 = 1 / (expAlphaWindowEpsilon + 1);
        // compute p2 and q2
        double expRemainedWindowEpsilon = Math.exp(remainedWindowEpsilon);
        p2 = expRemainedWindowEpsilon / (expRemainedWindowEpsilon + k - 1);
        q2 = 1 / (expRemainedWindowEpsilon + k - 1);
        // compute p3 and q3
        p3 = expRemainedWindowEpsilon / (expRemainedWindowEpsilon + d - k - 1);
        q3 = 1 / (expRemainedWindowEpsilon + d - k - 1);
        warmupState = true;
        gammaH = 0;
    }

    @Override
    public LdpHeavyHitterFactory.LdpHeavyHitterType getType() {
        return LdpHeavyHitterFactory.LdpHeavyHitterType.ADVAN_HG;
    }

    @Override
    public void stopWarmup() {
        // bias all counts and calculate λ
        double lambdaH = 0;
        for (Map.Entry<String, Double> entry : heavyGuardian.entrySet()) {
            String item = entry.getKey();
            double value = entry.getValue();
            lambdaH += value;
            value = value * p1 * (p2 - q2);
            heavyGuardian.put(item, value);
        }
        gammaH = lambdaH / num;
        assert gammaH >= 0 && gammaH <= 1 : "γ_h must be in range [0, 1]: " + gammaH;
        // fill the HeavyGuardian with 0-count items
        if (heavyGuardian.size() < k) {
            Set<String> remainedDomain = new HashSet<>(domainSet);
            remainedDomain.removeAll(heavyGuardian.keySet());
            for (String remainedItem : remainedDomain) {
                if (heavyGuardian.size() == k) {
                    break;
                }
                heavyGuardian.put(remainedItem, 0.0);
            }
        }
        warmupState = false;
    }

    @Override
    protected double updateCount(double count) {
        return count - currentNum * (gammaH * p1 * q2 + (1 - gammaH) * q1 / k);
    }

    @Override
    public String randomize(Map<String, Double> currentHeavyGuardian, String item, Random random) {
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

    protected boolean userMechanism1(Set<String> currentHeavyHitterSet, String item, Random random) {
        // Let b = Ber(e^ε_1 / (e^ε_1 + 1))
        SecureBernoulliSampler bernoulliSampler = new SecureBernoulliSampler(random, p1);
        boolean b = bernoulliSampler.sample();
        // if b == 1: if v ∈ HG, flag = 1, else flag = 0, if b == 0: if v ∈ HG, flag = 0, else flag = 1
        // this is identical to (b XOR v ∈ HG)
        return b == currentHeavyHitterSet.contains(item);
    }

    protected String userMechanism2(Set<String> currentHeavyHitterSet, String item, Random random) {
        ArrayList<String> currentHeavyHitterArrayList = new ArrayList<>(currentHeavyHitterSet);
        double randomSample = random.nextDouble();
        // Randomly sample an integer in [0, k)
        int randomIndex = random.nextInt(currentHeavyHitterSet.size());
        if (currentHeavyHitterSet.contains(item)) {
            // if v ∈ HG, use random response
            if (randomSample > p2 - q2) {
                // answer a random item in the current heavy hitter
                return currentHeavyHitterArrayList.get(randomIndex);
            } else {
                // answer the true item
                return item;
            }
        } else {
            // if v ∉ HG, choose a random item in the current heavy hitter
            return currentHeavyHitterArrayList.get(randomIndex);
        }
    }

    protected String userMechanism3(Map<String, Double> currentHeavyGuardian, String item, Random random) {
        // find the weakest guardian
        List<Map.Entry<String, Double>> currentList = new ArrayList<>(currentHeavyGuardian.entrySet());
        currentList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Map.Entry<String, Double> weakestCurrentCell = currentList.get(0);
        double weakestCurrentCount = weakestCurrentCell.getValue();
        // Honestly creating a remained set and randomly picking an element is slow, here we use re-sample technique.
        if (weakestCurrentCount <= 1.0) {
            // an item in HG is about to be evicted
            if (!currentHeavyGuardian.containsKey(item)) {
                // if v ∉ HG, use random response
                double randomSample = random.nextDouble();
                if (randomSample > p3 - q3) {
                    // answer a random item in the remained domain
                    while (true) {
                        int randomIndex = random.nextInt(d);
                        String randomizedItem = domainArrayList.get(randomIndex);
                        if (!currentHeavyGuardian.containsKey(randomizedItem)) {
                            return randomizedItem;
                        }
                    }
                } else {
                    // answer the true item
                    return item;
                }
            } else {
                // if v ∈ HG, choose a random item in the remained domain
                while (true) {
                    int randomIndex = random.nextInt(d);
                    String randomizedItem = domainArrayList.get(randomIndex);
                    if (!currentHeavyGuardian.containsKey(randomizedItem)) {
                        return randomizedItem;
                    }
                }
            }
        } else {
            // return BOT
            return BOT;
        }
    }

    @Override
    protected double debiasCount(double count) {
        return updateCount(count) / (p1 * (p2 - q2));
    }

    @Override
    public Map<String, Double> responseHeavyHitters() {
        // we only need to iterate items in the budget
        return heavyGuardian.keySet().stream().collect(Collectors.toMap(item -> item, this::response));
    }

    @Override
    public double getAlpha() {
        return alpha;
    }
}
