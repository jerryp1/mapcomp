package edu.alibaba.mpc4j.dp.stream.heavyhitter.hg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.SecureBernoulliSampler;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.HeavyHitterState;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.HeavyHitterStructure;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.HhgLdpHeavyHitter;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHeavyHitterFactory;

import java.util.*;
import java.util.stream.IntStream;

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
    private static final double DEFAULT_ALPHA = 1.0 / 3;
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
     * p3 = e^ε_3 / (e^ε_3 + d - λ_h - 1)
     */
    protected double[] p3s;
    /**
     * q3 = 1 / (e^ε_3 + d - λ_h - 1)
     */
    protected double[] q3s;
    /**
     * γ_h, proportion of hot items
     */
    protected double gammaH;

    public AdvHhgLdpHeavyHitter(Set<String> domainSet, Random heavyGuardianRandom,
                                int k, double windowEpsilon) {
        this(domainSet, heavyGuardianRandom, k, windowEpsilon, DEFAULT_ALPHA);
    }

    public AdvHhgLdpHeavyHitter(Set<String> domainSet, Random heavyGuardianRandom,
                                int k, double windowEpsilon, double alpha) {
        this(domainSet, 1, k, heavyGuardianRandom, k, windowEpsilon, alpha);
    }

    public AdvHhgLdpHeavyHitter(Set<String> domainSet, int w, int lambdaH, Random heavyGuardianRandom,
                                int k, double windowEpsilon) {
        this(domainSet, w, lambdaH, heavyGuardianRandom, k, windowEpsilon, DEFAULT_ALPHA);
    }

    public AdvHhgLdpHeavyHitter(Set<String> domainSet, int w, int lambdaH, Random heavyGuardianRandom,
                                int k, double windowEpsilon, double alpha) {
        super(domainSet, w, lambdaH, heavyGuardianRandom, k, windowEpsilon);
        MathPreconditions.checkPositiveInRange("α", alpha, 1);
        this.alpha = alpha;
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
        // compute p3 and q3
        p3s = IntStream.range(0, w)
            .mapToDouble(bucketIndex -> {
                int bucketD = bucketDs[bucketIndex];
                return expRemainedWindowEpsilon / (expRemainedWindowEpsilon + bucketD - lambdaH - 1);
            })
            .toArray();
        q3s = IntStream.range(0, w)
            .mapToDouble(bucketIndex -> {
                int bucketD = bucketDs[bucketIndex];
                return 1 / (expRemainedWindowEpsilon + bucketD - lambdaH - 1);
            })
            .toArray();
        heavyHitterState = HeavyHitterState.WARMUP;
        gammaH = 0;
    }

    @Override
    public LdpHeavyHitterFactory.LdpHeavyHitterType getType() {
        return LdpHeavyHitterFactory.LdpHeavyHitterType.ADVAN_HG;
    }

    @Override
    public void stopWarmup() {
        Preconditions.checkArgument(
            heavyHitterState.equals(HeavyHitterState.WARMUP),
            "The heavy hitter must be %s: %s", HeavyHitterState.WARMUP, heavyHitterState
        );
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
            // fill the budget with 0-count items
            if (budget.size() < lambdaH) {
                Set<String> remainedBudgetDomainSet = new HashSet<>(bucketDomainSets.get(budgetIndex));
                remainedBudgetDomainSet.removeAll(budget.keySet());
                for (String remainedBudgetDomainItem : remainedBudgetDomainSet) {
                    if (budget.size() == lambdaH) {
                        break;
                    }
                    budget.put(remainedBudgetDomainItem, 0.0);
                }
            }
        }
        gammaH = hotNum / num;
        assert gammaH >= 0 && gammaH <= 1 : "γ_h must be in range [0, 1]: " + gammaH;
        heavyHitterState = HeavyHitterState.STATISTICS;
    }

    @Override
    protected double updateCount(int bucketIndex, double count) {
        return count - currentNums[bucketIndex] * (gammaH * p1 * q2 + (1 - gammaH) * q1 / k);
    }

    @Override
    public String randomize(HeavyHitterStructure currentHeavyHitterStructure, String item, Random random) {
        Preconditions.checkArgument(
            currentHeavyHitterStructure instanceof HgHeavyHitterStructure,
            "The heavy hitter structure must be %s: %s",
            HgHeavyHitterStructure.class.getSimpleName(), currentHeavyHitterStructure.getClass().getSimpleName()
        );
        HgHeavyHitterStructure hgHeavyHitterStructure = (HgHeavyHitterStructure)currentHeavyHitterStructure;
        Preconditions.checkArgument(
            heavyHitterState.equals(HeavyHitterState.STATISTICS),
            "The heavy hitter must be %s: %s", HeavyHitterState.STATISTICS, heavyHitterState
        );
        Preconditions.checkArgument(
            domainSet.contains(item),
            "The input item is not in the domain: %s", item
        );
        byte[] itemByteArray = ObjectUtils.objectToByteArray(item);
        int bucketIndex = Math.abs(intHash.hash(itemByteArray) % w);
        assert bucketDomainSets.get(bucketIndex).contains(item);
        Map<String, Double> currentBucket = hgHeavyHitterStructure.getBudget(bucketIndex);
        MathPreconditions.checkEqual("Current bucket size", "λ_h", currentBucket.size(), lambdaH);
        if (bucketDs[bucketIndex] == lambdaH) {
            return userMechanism2(currentBucket.keySet(), item, random);
        }
        // M1
        boolean flag = userMechanism1(currentBucket.keySet(), item, random);
        // M2
        if (flag) {
            // v is determined as hot
            return userMechanism2(currentBucket.keySet(), item, random);
        } else {
            // v is determined as code
            return userMechanism3(bucketIndex, currentBucket, item, random);
        }
    }

    protected boolean userMechanism1(Set<String> currentBucketItemSet, String item, Random random) {
        // Let b = Ber(e^ε_1 / (e^ε_1 + 1))
        SecureBernoulliSampler bernoulliSampler = new SecureBernoulliSampler(random, p1);
        boolean b = bernoulliSampler.sample();
        // if b == 1: if v ∈ HG, flag = 1, else flag = 0, if b == 0: if v ∈ HG, flag = 0, else flag = 1
        // this is identical to (b XOR v ∈ HG)
        return b == currentBucketItemSet.contains(item);
    }

    protected String userMechanism2(Set<String> currentBucketItemSet, String item, Random random) {
        ArrayList<String> currentBudgetItemArrayList = new ArrayList<>(currentBucketItemSet);
        double randomSample = random.nextDouble();
        // Randomly sample an integer in [0, λ_h)
        int randomIndex = random.nextInt(lambdaH);
        if (currentBucketItemSet.contains(item)) {
            // if v ∈ HG, use random response
            if (randomSample > p2 - q2) {
                // answer a random item in the current heavy hitter
                return currentBudgetItemArrayList.get(randomIndex);
            } else {
                // answer the true item
                return item;
            }
        } else {
            // if v ∉ HG, choose a random item in the current heavy hitter
            return currentBudgetItemArrayList.get(randomIndex);
        }
    }

    protected String userMechanism3(int bucketIndex, Map<String, Double> currentBudget, String item, Random random) {
        ArrayList<String> budgetDomainArrayList = bucketDomainArrayLists.get(bucketIndex);
        int bucketD = bucketDs[bucketIndex];
        // find the weakest guardian
        List<Map.Entry<String, Double>> currentBucketList = new ArrayList<>(currentBudget.entrySet());
        currentBucketList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Map.Entry<String, Double> weakestCurrentCell = currentBucketList.get(0);
        double weakestCurrentCount = weakestCurrentCell.getValue();
        // Honestly creating a remained set and randomly picking an element is slow, here we use re-sample technique.
        if (weakestCurrentCount <= 1.0) {
            // an item in HG is about to be evicted
            if (!currentBudget.containsKey(item)) {
                // if v ∉ HG, use random response
                double randomSample = random.nextDouble();
                if (randomSample > p3s[bucketIndex] - q3s[bucketIndex]) {
                    // answer a random item in the remained domain
                    while (true) {
                        int randomIndex = random.nextInt(bucketD);
                        String randomizedItem = budgetDomainArrayList.get(randomIndex);
                        if (!currentBudget.containsKey(randomizedItem)) {
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
                    int randomIndex = random.nextInt(bucketD);
                    String randomizedItem = budgetDomainArrayList.get(randomIndex);
                    if (!currentBudget.containsKey(randomizedItem)) {
                        return randomizedItem;
                    }
                }
            }
        } else {
            // return BOT
            return BOT_PREFIX + bucketIndex;
        }
    }

    @Override
    protected double debiasCount(int bucketIndex, double count) {
        return updateCount(bucketIndex, count) / (p1 * (p2 - q2));
    }

    @Override
    public double getAlpha() {
        return alpha;
    }
}
