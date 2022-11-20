package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.ExpBernoulliSampler;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
 *
 * @author Weiran Liu
 * @date 2022/11/19
 */
abstract class AbstractHgLdpHeavyHitter implements HgLdpHeavyHitter {
    /**
     * the empty item ⊥
     */
    protected static final String BOT = "⊥";
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
    protected final Set<String> domainSet;
    /**
     * the domain array list
     */
    protected final ArrayList<String> domainArrayList;
    /**
     * d = |Ω|
     */
    protected final int d;
    /**
     * the number of heavy hitters k, which is equal to the cell num in the heavy part λ_h
     */
    protected final int k;
    /**
     * the HeavyGuardian, has k cells, identical to the heavy part in HeavyGuardian with w = 1 buckets, and λ_h cells
     */
    protected final Map<String, Double> heavyGuardian;
    /**
     * the private parameter ε / w
     */
    private final double windowEpsilon;
    /**
     * random state for HeavyGuardian
     */
    protected final Random heavyGuardianRandom;
    /**
     * the total number of insert items
     */
    protected int num;
    /**
     * the warmup state
     */
    protected boolean warmupState;
    /**
     * current de-bias num
     */
    protected int currentNum;

    AbstractHgLdpHeavyHitter(Set<String> domainSet, int k, double windowEpsilon, Random heavyGuardianRandom) {
        d = domainSet.size();
        Preconditions.checkArgument(d > 1, "|Ω| must be greater than 1: %s", d);
        this.domainSet = domainSet;
        domainArrayList = new ArrayList<>(domainSet);
        Preconditions.checkArgument(k > 0 && k <= d, "k must be in range (0, %s]: %s", d, k);
        this.k = k;
        heavyGuardian = new HashMap<>(k);
        Preconditions.checkArgument(windowEpsilon > 0, "ε must be greater than 0: %s", windowEpsilon);
        this.windowEpsilon = windowEpsilon;
        this.heavyGuardianRandom = heavyGuardianRandom;
        num = 0;
        currentNum = 0;
        warmupState = true;
    }

    @Override
    public boolean warmupInsert(String item) {
        Preconditions.checkArgument(warmupState, "The heavy hitter must be in the warm-up state");
        Preconditions.checkArgument(domainSet.contains(item), "The item is not in the domain set: %s", item);
        return insert(item);
    }

    @Override
    public boolean randomizeInsert(String randomizedItem) {
        Preconditions.checkArgument(!warmupState, "The heavy hitter must be not in the warm-up state");
        Preconditions.checkArgument(
            domainSet.contains(randomizedItem) || randomizedItem.equals(BOT),
            "The item is not in the domain set and not ⊥: %s", randomizedItem);
        return insert(randomizedItem);
    }

    private boolean insert(String item) {
        num++;
        // Case 1: e is in one cell in the heavy part of A[h(e)] (being a king or a guardian).
        if (heavyGuardian.containsKey(item)) {
            // HeavyGuardian just increments the corresponding frequency (the count field) in the cell by 1.
            double itemCount = heavyGuardian.get(item);
            itemCount += 1;
            heavyGuardian.put(item, itemCount);
            if (!warmupState) {
                currentNum++;
            }
            return true;
        }
        // Case 2: e is not in the heavy part of A[h(e)], and there are still empty cells.
        if (heavyGuardian.size() < k) {
            assert !item.equals(BOT) : "the item must not be ⊥: " + item;
            // It inserts e into an empty cell, i.e., sets the ID field to e and sets the count field to 1.
            heavyGuardian.put(item, 1.0);
            if (!warmupState) {
                currentNum++;
            }
            return true;
        }
        // Case 3: e is not in any cell in the heavy part of A[h(e)], and there is no empty cell.
        // We propose a novel technique named Exponential Decay: it decays (decrements) the count field of the weakest
        // guardian by 1 with probability P = b^{−C}, where b is a predefined constant number (e.g., b = 1.08), and C
        // is the value of the Count field of the weakest guardian.
        assert heavyGuardian.size() == k;
        // find the weakest guardian
        List<Map.Entry<String, Double>> heavyGuardianList = new ArrayList<>(heavyGuardian.entrySet());
        heavyGuardianList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Map.Entry<String, Double> weakestCell = heavyGuardianList.get(0);
        String weakestItem = weakestCell.getKey();
        double weakestCount = weakestCell.getValue();
        // Sample a boolean value, with probability P = b^{−C}, the boolean value is 1
        // In LDP, the weakest count may be non-positive, if so, we do not need to sample, since it must be evicted.
        if (weakestCount > 0) {
            // Here we use the advanced Bernoulli(exp(−γ)) with γ = C * ln(b), and reverse the sample
            ExpBernoulliSampler expBernoulliSampler = new ExpBernoulliSampler(heavyGuardianRandom, weakestCount * LN_B);
            // decay (decrement) the count field of the weakest guardian by 1 with probability P = b^{−C}
            boolean sample = expBernoulliSampler.sample();
            if (!sample) {
                weakestCount--;
            }
        }
        // After decay, if the count field becomes 0, it replaces the ID field of the weakest guardian with e,
        // and sets the count field to 1
        if (weakestCount <= 0) {
            heavyGuardian.remove(weakestItem);
            if (!warmupState) {
                // we partially de-bias the count for all items
                for (Map.Entry<String, Double> budgetEntry : heavyGuardian.entrySet()) {
                    budgetEntry.setValue(updateCount(budgetEntry.getValue()));
                }
                currentNum = 1;
            }
            assert !item.equals(BOT) : "the item must not be ⊥: " + item;
            heavyGuardian.put(item, 1.0);
            return true;
        } else {
            heavyGuardian.put(weakestItem, weakestCount);
            if (!warmupState) {
                currentNum++;
            }
            return false;
        }
    }

    /**
     * update count when an item is evicted in the HeavyGuardian while we are not in the warmup state.
     *
     * @param count the count stored in the HeavyGuardian while we are not in the warmup state.
     * @return the updated count.
     */
    protected abstract double updateCount(double count);


    @Override
    public double response(String item) {
        if (warmupState) {
            // return C
            return heavyGuardian.getOrDefault(item, 0.0);
        } else {
            // return de-biased C
            return debiasCount(heavyGuardian.getOrDefault(item, 0.0));
        }
    }

    /**
     * De-bias count in response.
     *
     * @param count the biased count stored in the HeavyGuardian.
     * @return the de-biased count.
     */
    protected abstract double debiasCount(double count);

    @Override
    public Map<String, Double> responseHeavyHitters() {
        // we only need to iterate items in the budget
        return heavyGuardian.keySet().stream().collect(Collectors.toMap(item -> item, this::response));
    }

    @Override
    public double getWindowEpsilon() {
        return windowEpsilon;
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
        return heavyGuardian.keySet();
    }

    @Override
    public Map<String, Double> getCurrentDataStructure() {
        return heavyGuardian;
    }
}
