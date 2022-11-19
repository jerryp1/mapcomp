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
public class NaiveLdpHeavyHitter implements LdpHeavyHitter {
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
     * the total number of insert items
     */
    private int num;

    public NaiveLdpHeavyHitter(Set<String> domainSet, int k, double epsilon) {
        Preconditions.checkArgument(domainSet.size() > 1, "|Ω| must be greater than 1: %s", domainSet.size());
        this.domainSet = domainSet;
        domainArrayList = new ArrayList<>(domainSet);
        d = domainArrayList.size();
        Preconditions.checkArgument(k > 0 && k <= domainSet.size(), "k must be in range (0, %s]: %s", domainSet.size(), k);
        this.k = k;
        budget = new HashMap<>(d);
        Preconditions.checkArgument(epsilon > 0, "ε must be greater than 0: %s", epsilon);
        this.epsilon = epsilon;
        double expEpsilon = Math.exp(epsilon);
        p = expEpsilon / (expEpsilon + d - 1);
        q = 1 / (expEpsilon + d - 1);
        num = 0;
    }

    @Override
    public LdpHeavyHitterFactory.LdpHeavyHitterType getType() {
        return LdpHeavyHitterFactory.LdpHeavyHitterType.NAIVE;
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
        return (budget.getOrDefault(item, 0.0) - num * q) / (p - q);
    }

    @Override
    public Map<String, Double> responseHeavyHitters() {
        return responseOrderedDomain()
            .subList(0, k)
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
        return responseHeavyHitters().keySet();
    }
}
