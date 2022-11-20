package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import com.google.common.base.Preconditions;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Naive Heavy Hitter with Local Differential Privacy.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
class NaiveLdpHeavyHitter implements LdpHeavyHitter {
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
     * the bucket
     */
    private final Map<String, Double> budget;
    /**
     * the private parameter ε / w
     */
    private final double windowEpsilon;
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    private final double q;
    /**
     * the number of inserted items
     */
    private int num;
    /**
     * the warmup state
     */
    private boolean warmupState;

    NaiveLdpHeavyHitter(Set<String> domainSet, int k, double windowEpsilon) {
        d = domainSet.size();
        Preconditions.checkArgument(d > 1, "|Ω| must be greater than 1: %s", d);
        this.domainSet = domainSet;
        domainArrayList = new ArrayList<>(domainSet);
        Preconditions.checkArgument(k > 0 && k <= d, "k must be in range (0, %s]: %s", d, k);
        this.k = k;
        budget = new HashMap<>(d);
        Preconditions.checkArgument(windowEpsilon > 0, "ε / w must be greater than 0: %s", windowEpsilon);
        this.windowEpsilon = windowEpsilon;
        double expEpsilon = Math.exp(windowEpsilon);
        p = expEpsilon / (expEpsilon + d - 1);
        q = 1 / (expEpsilon + d - 1);
        num = 0;
        warmupState = true;
    }

    @Override
    public LdpHeavyHitterFactory.LdpHeavyHitterType getType() {
        return LdpHeavyHitterFactory.LdpHeavyHitterType.NAIVE_RR;
    }

    @Override
    public boolean warmupInsert(String item) {
        Preconditions.checkArgument(warmupState, "The heavy hitter must be in the warm-up state");
        num++;
        if (budget.containsKey(item)) {
            double itemCount = budget.get(item);
            itemCount += 1;
            budget.put(item, itemCount);
        } else {
            budget.put(item, 1.0);
        }
        return true;
    }

    @Override
    public void stopWarmup() {
        // bias all counts
        for(Map.Entry<String, Double> entry : budget.entrySet()) {
            String item = entry.getKey();
            double value = entry.getValue();
            value = value * (p - q) + num * q;
            budget.put(item, value);
        }
        warmupState = false;
    }

    @Override
    public Map<String, Double> getCurrentDataStructure() {
        return budget;
    }

    @Override
    public String randomize(Map<String, Double> currentDataStructure, String item, Random random) {
        Preconditions.checkArgument(domainSet.contains(item), "The input idem is not in the domain: %s", item);
        // naive solution does not consider the current data structure
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
    public boolean randomizeInsert(String randomizedItem) {
        Preconditions.checkArgument(!warmupState, "The heavy hitter must be not in the warm-up state");
        num++;
        if (budget.containsKey(randomizedItem)) {
            double itemCount = budget.get(randomizedItem);
            itemCount += 1;
            budget.put(randomizedItem, itemCount);
        } else {
            budget.put(randomizedItem, 1.0);
        }
        return true;
    }

    @Override
    public double response(String item) {
        if (warmupState) {
            // we do not need to debias in the warm-up state
            return budget.getOrDefault(item, 0.0);
        } else {
            return (budget.getOrDefault(item, 0.0) - num * q) / (p - q);
        }

    }

    @Override
    public Map<String, Double> responseHeavyHitters() {
        return responseOrderedDomain()
            .subList(0, k)
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
        return responseHeavyHitters().keySet();
    }
}
