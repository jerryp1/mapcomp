package edu.alibaba.mpc4j.dp.stream.heavyhitter.hg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.SecureBernoulliSampler;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.HeavyHitterState;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.HeavyHitterStructure;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHeavyHitterFactory;

import java.util.*;

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

    public RelaxHhgLdpHeavyHitter(Set<String> domainSet, Random heavyGuardianRandom,
                                  int k, double windowEpsilon) {
        this(domainSet, heavyGuardianRandom, k, windowEpsilon, DEFAULT_ALPHA);
    }

    public RelaxHhgLdpHeavyHitter(Set<String> domainSet, Random heavyGuardianRandom,
                                  int k, double windowEpsilon, double alpha) {
        this(domainSet, 1, k, 0, heavyGuardianRandom, k, windowEpsilon, alpha);
    }

    public RelaxHhgLdpHeavyHitter(Set<String> domainSet, int w, int lambdaH, int primeIndex, Random heavyGuardianRandom,
                                  int k, double windowEpsilon) {
        this(domainSet, w, lambdaH, primeIndex, heavyGuardianRandom, k, windowEpsilon, DEFAULT_ALPHA);
    }

    public RelaxHhgLdpHeavyHitter(Set<String> domainSet, int w, int lambdaH, int primeIndex, Random heavyGuardianRandom,
                                  int k, double windowEpsilon, double alpha) {
        super(domainSet, w, lambdaH, primeIndex, heavyGuardianRandom, k, windowEpsilon, alpha);
        // recompute p2 and q2
        double expWindowEpsilon = Math.exp(windowEpsilon);
        p2 = expWindowEpsilon / (expWindowEpsilon + lambdaH - 1);
        q2 = 1 / (expWindowEpsilon + lambdaH - 1);
    }

    @Override
    public LdpHeavyHitterFactory.LdpHeavyHitterType getType() {
        return LdpHeavyHitterFactory.LdpHeavyHitterType.RELAX_HG;
    }

    @Override
    protected double updateCount(int bucketIndex, double count) {
        return count - currentNums[bucketIndex] * gammaH * p1 * q2;
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
        int bucketIndex = Math.abs(bobIntHash.hash(itemByteArray) % w);
        assert bucketDomainSets.get(bucketIndex).contains(item);
        Map<String, Double> currentBucket = hgHeavyHitterStructure.getBudget(bucketIndex);
        Preconditions.checkArgument(
            currentBucket.size() == lambdaH,
            "Current bucket size must be equal to %s: %s", lambdaH, currentBucket.size()
        );
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

    @Override
    protected boolean userMechanism1(Set<String> currentBucketItemSet, String item, Random random) {
        if (!currentBucketItemSet.contains(item)) {
            return false;
        } else {
            // Let b = Ber(e^ε_1 / (e^ε_1 + 1))
            SecureBernoulliSampler bernoulliSampler = new SecureBernoulliSampler(random, p1);
            return bernoulliSampler.sample();
        }
    }

    @Override
    protected String userMechanism2(Set<String> currentBucketItemSet, String item, Random random) {
        ArrayList<String> currentHeavyHitterArrayList = new ArrayList<>(currentBucketItemSet);
        double randomSample = random.nextDouble();
        // Randomly sample an integer in [0, k)
        int randomIndex = random.nextInt(currentBucketItemSet.size());
        // if v ∈ HG, use random response
        if (randomSample > p2 - q2) {
            // answer a random item in the current heavy hitter
            return currentHeavyHitterArrayList.get(randomIndex);
        } else {
            // answer the true item
            return item;
        }
    }
}
