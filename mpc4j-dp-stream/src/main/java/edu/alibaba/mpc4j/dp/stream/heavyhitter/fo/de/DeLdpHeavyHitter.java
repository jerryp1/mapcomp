package edu.alibaba.mpc4j.dp.stream.heavyhitter.fo.de;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.HeavyHitterState;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.HeavyHitterStructure;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHeavyHitterFactory;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.fo.AbstractFoLdpHeavyHitter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Direct Encoding Heavy Hitter with Local Differential Privacy.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public class DeLdpHeavyHitter extends AbstractFoLdpHeavyHitter {
    /**
     * the bucket
     */
    private final Map<String, Double> budget;
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    private final double q;

    public DeLdpHeavyHitter(Set<String> domainSet, int k, double windowEpsilon) {
        super(domainSet, k, windowEpsilon);
        budget = new HashMap<>(d);
        double expEpsilon = Math.exp(windowEpsilon);
        p = expEpsilon / (expEpsilon + d - 1);
        q = 1 / (expEpsilon + d - 1);
    }

    @Override
    public LdpHeavyHitterFactory.LdpHeavyHitterType getType() {
        return LdpHeavyHitterFactory.LdpHeavyHitterType.DE_FO;
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
        return new DeHeavyHitterStructure(budget);
    }

    @Override
    public String randomize(HeavyHitterStructure currentHeavyHitterStructure, String item, Random random) {
        Preconditions.checkArgument(
            currentHeavyHitterStructure instanceof DeHeavyHitterStructure,
            "The heavy hitter structure must be %s: %s",
            DeHeavyHitterStructure.class.getSimpleName(), currentHeavyHitterStructure.getClass().getSimpleName()
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
}
