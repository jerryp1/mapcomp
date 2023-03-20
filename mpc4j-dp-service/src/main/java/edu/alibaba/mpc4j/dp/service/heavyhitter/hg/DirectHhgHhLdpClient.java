package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HgHhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HhLdpServerContext;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Direct Hot HeavyGuardian-based Heavy Hitter LDP client.
 *
 * @author Weiran Liu
 * @date 2023/3/18
 */
public class DirectHhgHhLdpClient extends AbstractHgHhLdpClient implements HgHhLdpClient {
    /**
     * p= e^ε / (e^ε + (λ_h + 1) - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + (λ_h + 1) - 1)
     */
    private final double q;
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    private final double[] ps;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    private final double[] qs;

    public DirectHhgHhLdpClient(HhLdpConfig config) {
        super(config);
        // compute p = e^ε / (e^ε + ( + 1) - 1)
        double expWindowEpsilon = Math.exp(windowEpsilon);
        p = expWindowEpsilon / (expWindowEpsilon + (lambdaH + 1) - 1);
        q = 1 / (expWindowEpsilon + (lambdaH + 1) - 1);
        // compute ps and qs
        ps = IntStream.range(0, w)
            .mapToDouble(bucketIndex -> {
                int bucketD = bucketDomain.getD(bucketIndex);
                return expWindowEpsilon / (expWindowEpsilon + bucketD - 1);
            })
            .toArray();
        qs = IntStream.range(0, w)
            .mapToDouble(bucketIndex -> {
                int bucketD = bucketDomain.getD(bucketIndex);
                return 1 / (expWindowEpsilon + bucketD - 1);
            })
            .toArray();
    }

    @Override
    public byte[] randomize(HhLdpServerContext serverContext, String item, Random random) {
        Preconditions.checkArgument(serverContext instanceof HgHhLdpServerContext);
        HgHhLdpServerContext hgServerContext = (HgHhLdpServerContext) serverContext;
        checkItemInDomain(item);
        int bucketIndex = bucketDomain.getItemBucket(item);
        assert bucketDomain.getBucketDomainSet(bucketIndex).contains(item);
        Map<String, Double> currentBucket = hgServerContext.getBudget(bucketIndex);
        Map<String, Double> copyCurrentBucket = new HashMap<>(currentBucket);
        // fill the budget with 0-count dummy items
        if (copyCurrentBucket.size() < lambdaH) {
            Set<String> remainedBudgetDomainSet = new HashSet<>(bucketDomain.getBucketDomainSet(bucketIndex));
            remainedBudgetDomainSet.removeAll(currentBucket.keySet());
            for (String remainedBudgetDomainItem : remainedBudgetDomainSet) {
                if (copyCurrentBucket.size() == lambdaH) {
                    break;
                }
                copyCurrentBucket.put(remainedBudgetDomainItem, 0.0);
            }
        }
        // now there are λ_h elements in the budget, randomize the item
        return mechanism(bucketIndex, copyCurrentBucket, item, random).getBytes(HhLdpFactory.DEFAULT_CHARSET);
    }

    private String mechanism(int bucketIndex, Map<String, Double> currentBudget, String item, Random random) {
        // find the weakest guardian
        List<Map.Entry<String, Double>> currentBucketList = new ArrayList<>(currentBudget.entrySet());
        currentBucketList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Map.Entry<String, Double> weakestCurrentCell = currentBucketList.get(0);
        double weakestCurrentCount = weakestCurrentCell.getValue();
        if (weakestCurrentCount <= 1.0) {
            // an item in HG is about to be evicted, use basic mechanism to response
            int bucketD = bucketDomain.getD(bucketIndex);
            double randomSample = random.nextDouble();
            if (randomSample > ps[bucketIndex] - qs[bucketIndex]) {
                // answer a random item in the budget domain
                int randomIndex = random.nextInt(bucketD);
                return bucketDomain.getBucketIndexItem(bucketIndex, randomIndex);
            } else {
                // answer the true item
                return item;
            }
        } else {
            // no item in HG will be evicted, response using {h_1, ..., k_{λ_h}, ⊥}
            String botItem = HhLdpFactory.BOT_PREFIX + bucketIndex;
            ArrayList<String> sampleArrayList = new ArrayList<>(currentBudget.keySet());
            sampleArrayList.add(botItem);
            assert sampleArrayList.size() == lambdaH + 1;
            // if v ∈ HG, the target item is the item; otherwise, the target item is ⊥.
            String targetItem = currentBudget.containsKey(item) ? item : botItem;
            double randomSample = random.nextDouble();
            if (randomSample > p - q) {
                // answer a random item in {h_1, ..., k_{λ_h}, ⊥}
                int randomIndex = random.nextInt(lambdaH + 1);
                return sampleArrayList.get(randomIndex);
            } else {
                // answer the target item
                return targetItem;
            }
        }
    }
}
