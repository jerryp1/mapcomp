package edu.alibaba.mpc4j.dp.stream.heavyhitter.fo;

import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhServerState;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.config.LdpHhServerConfig;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.utils.EmptyLdpHhServerContext;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.utils.LdpHhServerContext;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Direct Encoding Heavy Hitter server with Local Differential Privacy.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public class DeFoLdpHhServer extends AbstractFoLdpHhServer {
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

    public DeFoLdpHhServer(LdpHhServerConfig serverConfig) {
        super(serverConfig);
        budget = new HashMap<>(d);
        double expEpsilon = Math.exp(windowEpsilon);
        p = expEpsilon / (expEpsilon + d - 1);
        q = 1 / (expEpsilon + d - 1);
    }

    @Override
    public LdpHhFactory.LdpHhType getType() {
        return LdpHhFactory.LdpHhType.DE_FO;
    }

    @Override
    public boolean warmupInsert(String item) {
        checkState(LdpHhServerState.WARMUP);
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
        checkState(LdpHhServerState.WARMUP);
        // bias all counts
        for(Map.Entry<String, Double> entry : budget.entrySet()) {
            String item = entry.getKey();
            double value = entry.getValue();
            value = value * (p - q) + num * q;
            budget.put(item, value);
        }
        ldpHhServerState = LdpHhServerState.STATISTICS;
    }

    @Override
    public LdpHhServerContext getServerContext() {
        return new EmptyLdpHhServerContext();
    }

    @Override
    public boolean randomizeInsert(String randomizedItem) {
        checkState(LdpHhServerState.STATISTICS);
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
        switch (ldpHhServerState) {
            case WARMUP:
                // we do not need to debias in the warm-up state
                return budget.getOrDefault(item, 0.0);
            case STATISTICS:
                return (budget.getOrDefault(item, 0.0) - num * q) / (p - q);
            default:
                throw new IllegalStateException("Invalid " + LdpHhServerState.class.getSimpleName() + ": " + ldpHhServerState);

        }
    }

    @Override
    public Map<String, Double> responseHeavyHitters() {
        return responseOrderedDomain(budget.keySet())
            .subList(0, k)
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
