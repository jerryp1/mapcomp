package edu.alibaba.mpc4j.dp.service.heavyhitter.fo;

import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpServerState;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.EmptyHhLdpServerContext;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Direct Encoding Heavy Hitter server with Local Differential Privacy.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public class DeFoHhLdpServer extends AbstractFoHhLdpServer {
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

    public DeFoHhLdpServer(HhLdpConfig hhLdpConfig) {
        super(hhLdpConfig);
        budget = new HashMap<>(d);
        double expEpsilon = Math.exp(windowEpsilon);
        p = expEpsilon / (expEpsilon + d - 1);
        q = 1 / (expEpsilon + d - 1);
    }

    @Override
    public boolean warmupInsert(byte[] itemBytes) {
        checkState(HhLdpServerState.WARMUP);
        String item = new String(itemBytes, HhLdpFactory.DEFAULT_CHARSET);
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
        checkState(HhLdpServerState.WARMUP);
        // bias all counts
        for(Map.Entry<String, Double> entry : budget.entrySet()) {
            String item = entry.getKey();
            double value = entry.getValue();
            value = value * (p - q) + num * q;
            budget.put(item, value);
        }
        hhLdpServerState = HhLdpServerState.STATISTICS;
    }

    @Override
    public HhLdpServerContext getServerContext() {
        return new EmptyHhLdpServerContext();
    }

    @Override
    public boolean randomizeInsert(byte[] itemBytes) {
        checkState(HhLdpServerState.STATISTICS);
        String item = new String(itemBytes, HhLdpFactory.DEFAULT_CHARSET);
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

    private double response(String item) {
        switch (hhLdpServerState) {
            case WARMUP:
                // we do not need to debias in the warm-up state
                return budget.getOrDefault(item, 0.0);
            case STATISTICS:
                return (budget.getOrDefault(item, 0.0) - num * q) / (p - q);
            default:
                throw new IllegalStateException("Invalid " + HhLdpServerState.class.getSimpleName() + ": " + hhLdpServerState);

        }
    }

    @Override
    public Map<String, Double> responseHeavyHitters() {
        Map<String, Double> domainCountMap = budget.keySet()
            .stream()
            .collect(Collectors.toMap(item -> item, this::response));
        List<Map.Entry<String, Double>> countList = new ArrayList<>(domainCountMap.entrySet());
        // descending sort
        countList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Collections.reverse(countList);

        if (budget.keySet().size() <= k) {
            // the current key set is less than k, return all items
            return countList.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            return countList
                .subList(0, k)
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }
}
