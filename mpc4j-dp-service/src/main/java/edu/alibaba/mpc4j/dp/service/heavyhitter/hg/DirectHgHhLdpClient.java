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
public class DirectHgHhLdpClient extends AbstractHgHhLdpClient implements HgHhLdpClient {
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

    public DirectHgHhLdpClient(HhLdpConfig config) {
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
        assert currentBucket.size() == lambdaH;
        // now there are λ_h elements in the budget, randomize the item
        return mechanism(bucketIndex, currentBucket, item, random).getBytes(HhLdpFactory.DEFAULT_CHARSET);
    }

    private String mechanism(int bucketIndex, Map<String, Double> currentBucket, String item, Random random) {
        // find the weakest guardian
        List<Map.Entry<String, Double>> currentBucketList = new ArrayList<>(currentBucket.entrySet());
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
            ArrayList<String> sampleArrayList = new ArrayList<>(currentBucket.keySet());
            sampleArrayList.add(botItem);
            assert sampleArrayList.size() == lambdaH + 1;
            // if v ∈ HG, the target item is the item; otherwise, the target item is ⊥.
            String targetItem = currentBucket.containsKey(item) ? item : botItem;
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
