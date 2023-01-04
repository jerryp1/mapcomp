package edu.alibaba.mpc4j.dp.stream.heavyhitter.naive;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.HeavyHitterState;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.HeavyHitterStructure;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHeavyHitter;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHeavyHitterFactory;

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
    private Set<String> domainSet;
    /**
     * the domain array list
     */
    private ArrayList<String> domainArrayList;
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
     * the state
     */
    private HeavyHitterState heavyHitterState;

    public NaiveLdpHeavyHitter(Set<String> domainSet, int k, double windowEpsilon) {
        d = domainSet.size();
        MathPreconditions.checkGreater("|Ω|", d, 1);
        this.domainSet = domainSet;
        domainArrayList = new ArrayList<>(domainSet);
        MathPreconditions.checkPositiveInRangeClosed("k", k, d);
        this.k = k;
        budget = new HashMap<>(d);
        MathPreconditions.checkPositive("ε / w", windowEpsilon);
        this.windowEpsilon = windowEpsilon;
        double expEpsilon = Math.exp(windowEpsilon);
        p = expEpsilon / (expEpsilon + d - 1);
        q = 1 / (expEpsilon + d - 1);
        num = 0;
        heavyHitterState = HeavyHitterState.WARMUP;
    }

    @Override
    public LdpHeavyHitterFactory.LdpHeavyHitterType getType() {
        return LdpHeavyHitterFactory.LdpHeavyHitterType.NAIVE_RR;
    }

    @Override
    public boolean warmupInsert(String item) {
        Preconditions.checkArgument(
            heavyHitterState.equals(HeavyHitterState.WARMUP),
            "The heavy hitter must be %s: %s", HeavyHitterState.WARMUP, heavyHitterState
        );
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
        Preconditions.checkArgument(
            heavyHitterState.equals(HeavyHitterState.WARMUP),
            "The heavy hitter must be %s: %s", HeavyHitterState.WARMUP, heavyHitterState
        );
        // bias all counts
        for(Map.Entry<String, Double> entry : budget.entrySet()) {
            String item = entry.getKey();
            double value = entry.getValue();
            value = value * (p - q) + num * q;
            budget.put(item, value);
        }
        heavyHitterState = HeavyHitterState.STATISTICS;
    }

    @Override
    public HeavyHitterStructure getCurrentHeavyHitterStructure() {
        return new NaiveHeavyHitterStructure(budget);
    }

    @Override
    public String randomize(HeavyHitterStructure currentHeavyHitterStructure, String item, Random random) {
        Preconditions.checkArgument(
            currentHeavyHitterStructure instanceof NaiveHeavyHitterStructure,
            "The heavy hitter structure must be %s: %s",
            NaiveHeavyHitterStructure.class.getSimpleName(), currentHeavyHitterStructure.getClass().getSimpleName()
        );
        Preconditions.checkArgument(
            heavyHitterState.equals(HeavyHitterState.STATISTICS),
            "The heavy hitter must be %s: %s", HeavyHitterState.STATISTICS, heavyHitterState
        );
        Preconditions.checkArgument(
            domainSet.contains(item),
            "The input idem is not in the domain: %s", item
        );
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
        Preconditions.checkArgument(
            heavyHitterState.equals(HeavyHitterState.STATISTICS),
            "The heavy hitter must be %s: %s", HeavyHitterState.STATISTICS, heavyHitterState
        );
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
        switch (heavyHitterState) {
            case WARMUP:
                // we do not need to debias in the warm-up state
                return budget.getOrDefault(item, 0.0);
            case STATISTICS:
            case CLEAN:
                return (budget.getOrDefault(item, 0.0) - num * q) / (p - q);
            default:
                throw new IllegalStateException("Invalid " + HeavyHitterState.class.getSimpleName() + ": " + heavyHitterState);

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
    public void cleanDomainSet() {
        Preconditions.checkArgument(
            heavyHitterState.equals(HeavyHitterState.WARMUP) || heavyHitterState.equals(HeavyHitterState.STATISTICS),
            "The heavy hitter must be %s or %s: %s", HeavyHitterState.WARMUP, HeavyHitterState.STATISTICS, heavyHitterState
        );
        domainSet = null;
        domainArrayList = null;
        heavyHitterState = HeavyHitterState.CLEAN;
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
        Preconditions.checkArgument(
            heavyHitterState.equals(HeavyHitterState.WARMUP) || heavyHitterState.equals(HeavyHitterState.STATISTICS),
            "The heavy hitter must be %s or %s: %s", HeavyHitterState.WARMUP, HeavyHitterState.STATISTICS, heavyHitterState
        );
        return domainSet;
    }
}
