package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.ExpBernoulliSampler;

import java.security.SecureRandom;
import java.util.*;

/**
 * Basic HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public class BasicHgLdpHeavyHitter implements LdpHeavyHitter {
    /**
     * b = 1.08
     */
    private static final double B = 1.08;
    /**
     * ln(b)
     */
    private static final double LN_B = Math.log(B);
    /**
     * the domain set
     */
    private final Set<String> domainSet;
    /**
     * the domain array list
     */
    private final ArrayList<String> domainArrayList;
    /**
     * d = |Ω|
     */
    private final int d;
    /**
     * the number of heavy hitters k, which is equal to the cell num in the heavy part λ_h
     */
    private final int k;
    /**
     * the bucket, has k cells, identical to the heavy part in HeavyGuardian with w = 1 buckets, and λ_h cells
     */
    private final Map<String, Double> budget;
    /**
     * ε
     */
    private final double epsilon;
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    private final double q;
    /**
     * random
     */
    private final Random random;
    /**
     * the total number of insert items
     */
    private int num;
    /**
     * current debias num
     */
    private int currentNum;

    public BasicHgLdpHeavyHitter(Set<String> domainSet, int k, double epsilon) {
        this(domainSet, k, epsilon, new SecureRandom());
    }

    public BasicHgLdpHeavyHitter(Set<String> domainSet, int k, double epsilon, Random random) {
        Preconditions.checkArgument(domainSet.size() > 1, "|Ω| must be greater than 1: %s", domainSet.size());
        this.domainSet = domainSet;
        domainArrayList = new ArrayList<>(domainSet);
        d = domainArrayList.size();
        Preconditions.checkArgument(k > 0 && k <= domainSet.size(), "k must be in range (0, %s]: %s", domainSet.size(), k);
        this.k = k;
        budget = new HashMap<>(k);
        Preconditions.checkArgument(epsilon > 0, "ε must be greater than 0: %s", epsilon);
        this.epsilon = epsilon;
        double expEpsilon = Math.exp(epsilon);
        p = expEpsilon / (expEpsilon + d - 1);
        q = 1 / (expEpsilon + d - 1);
        this.random = random;
        num = 0;
        currentNum = 0;
    }

    @Override
    public LdpHeavyHitterFactory.LdpHeavyHitterType getType() {
        return LdpHeavyHitterFactory.LdpHeavyHitterType.BASIC_HEAVY_GUARDIAN;
    }

    @Override
    public String randomize(String item, Random random) {
        Preconditions.checkArgument(domainSet.contains(item), "The input idem is not in the domain: %s", item);
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

    @Override
    public boolean insert(String randomizedItem) {
        num++;
        // Case 1: e is in one cell in the heavy part of A[h(e)] (being a king or a guardian).
        if (budget.containsKey(randomizedItem)) {
            // HeavyGuardian just increments the corresponding frequency (the count field) in the cell by 1.
            double itemCount = budget.get(randomizedItem);
            itemCount += 1;
            budget.put(randomizedItem, itemCount);
            currentNum++;
            return true;
        }
        // Case 2: e is not in the heavy part of A[h(e)], and there are still empty cells.
        if (budget.size() < k) {
            // It inserts e into an empty cell, i.e., sets the ID field to e and sets the count field to 1.
            budget.put(randomizedItem, 1.0);
            currentNum++;
            return true;
        }
        // Case 3: e is not in any cell in the heavy part of A[h(e)], and there is no empty cell.
        // We propose a novel technique named Exponential Decay: it decays (decrements) the count field of the weakest
        // guardian by 1 with probability P = b^{−C}, where b is a predefined constant number (e.g., b = 1.08), and C
        // is the value of the Count field of the weakest guardian.
        assert budget.size() == k;
        // find the weakest guardian
        List<Map.Entry<String, Double>> heavyPartList = new ArrayList<>(budget.entrySet());
        heavyPartList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Map.Entry<String, Double> weakestHeavyPartCell = heavyPartList.get(0);
        String weakestHeavyPartItem = weakestHeavyPartCell.getKey();
        double weakestHeavyPartCount = weakestHeavyPartCell.getValue();
        // Sample a boolean value, with probability P = b^{−C}, the boolean value is 1
        // Here we use the advanced Bernoulli(exp(−γ)) with γ = C * ln(b), and reverse the sample
        ExpBernoulliSampler expBernoulliSampler = new ExpBernoulliSampler(random, weakestHeavyPartCount * LN_B);
        // decay (decrement) the count field of the weakest guardian by 1 with probability P = b^{−C}
        boolean sample = expBernoulliSampler.sample();
        if (!sample) {
            weakestHeavyPartCount--;
        }
        // After decay, if the count field becomes 0, it replaces the ID field of the weakest guardian with e,
        // and sets the count field to 1
        if (weakestHeavyPartCount <= 0) {
            budget.remove(weakestHeavyPartItem);
            // we partially de-bias the count for all items
            for (Map.Entry<String, Double> budgetEntry : budget.entrySet()) {
                budgetEntry.setValue(budgetEntry.getValue() - currentNum * q);
            }
            currentNum = 0;
            budget.put(randomizedItem, 1.0);
            currentNum++;
            return true;
        } else {
            budget.put(weakestHeavyPartItem, weakestHeavyPartCount);
            currentNum++;
            return false;
        }
    }

    @Override
    public double response(String item) {
        // (C - num * q) / (p − q)
        return (budget.getOrDefault(item, 0.0) - currentNum * q) / (p - q);
    }

    @Override
    public double getEpsilon() {
        return epsilon;
    }

    @Override
    public int getK() {
        return k;
    }

    @Override
    public int getNum() {
        return num;
    }

    @Override
    public Set<String> getDomainSet() {
        return domainSet;
    }

    @Override
    public Set<String> getHeavyHitterSet() {
        return budget.keySet();
    }
}
