package edu.alibaba.mpc4j.dp.stream.heavyhitter.hg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.ExpBernoulliSampler;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhFactory;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhServerState;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.config.HgLdpHhServerConfig;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Abstract Heavy Hitter server with Local Differential Privacy based on HeavyGuardian.
 *
 * @author Weiran Liu
 * @date 2022/11/19
 */
abstract class AbstractHgLdpHhServer implements HgLdpHhServer {
    /**
     * b = 1.08
     */
    private static final double B = 1.08;
    /**
     * ln(b)
     */
    private static final double LN_B = Math.log(B);
    /**
     * d = |Ω|
     */
    protected final int d;
    /**
     * the number of heavy hitters k
     */
    protected final int k;
    /**
     * the non-cryptographic 32-bit hash function
     */
    protected final IntHash intHash;
    /**
     * budget num
     */
    protected final int w;
    /**
     * λ_h, i.e., the cell num in each bucket
     */
    protected final int lambdaH;
    /**
     * w buckets, each bucket has λ_h cells
     */
    protected final ArrayList<Map<String, Double>> buckets;
    /**
     * the private parameter ε / w
     */
    protected final double windowEpsilon;
    /**
     * random state for HeavyGuardian
     */
    protected final Random random;
    /**
     * the total number of insert items
     */
    protected int num;
    /**
     * the state
     */
    protected LdpHhServerState ldpHhServerState;
    /**
     * current de-bias num for each budget
     */
    protected int[] currentNums;

    AbstractHgLdpHhServer(HgLdpHhServerConfig serverConfig) {
        d = serverConfig.getD();
        k = serverConfig.getK();
        w = serverConfig.getW();
        lambdaH = serverConfig.getLambdaH();
        windowEpsilon = serverConfig.getWindowEpsilon();
        random = serverConfig.getRandom();
        // init buckets
        buckets = IntStream.range(0, w)
            .mapToObj(bucketIndex -> new HashMap<String, Double>(lambdaH))
            .collect(Collectors.toCollection(ArrayList::new));
        // init hash function
        intHash = IntHashFactory.fastestInstance();
        // init variables
        num = 0;
        currentNums = new int[w];
        Arrays.fill(currentNums, 0);
        ldpHhServerState = LdpHhServerState.WARMUP;
    }

    protected void checkState(LdpHhServerState expect) {
        Preconditions.checkArgument(ldpHhServerState.equals(expect), "The state must be %s: %s", expect, ldpHhServerState);
    }

    @Override
    public boolean warmupInsert(String item) {
        checkState(LdpHhServerState.WARMUP);
        return insert(item);
    }

    @Override
    public boolean randomizeInsert(String randomizedItem) {
        checkState(LdpHhServerState.STATISTICS);
        return insert(randomizedItem);
    }

    private boolean insert(String item) {
        num++;
        // it first computes the hash function h(e) (1 ⩽ h(e) ⩽ w) to map e to bucket A[h(e)].
        int bucketIndex;
        if (item.startsWith(LdpHhFactory.BOT_PREFIX)) {
            bucketIndex = Integer.parseInt(item.substring(LdpHhFactory.BOT_PREFIX.length()));
        } else {
            byte[] itemByteArray = ObjectUtils.objectToByteArray(item);
            bucketIndex = Math.abs(intHash.hash(itemByteArray) % w);
        }
        Map<String, Double> bucket = buckets.get(bucketIndex);
        // Case 1: e is in one cell in the heavy part of A[h(e)] (being a king or a guardian).
        if (bucket.containsKey(item)) {
            // HeavyGuardian just increments the corresponding frequency (the count field) in the cell by 1.
            double itemCount = bucket.get(item);
            itemCount += 1;
            bucket.put(item, itemCount);
            if (ldpHhServerState.equals(LdpHhServerState.STATISTICS)) {
                currentNums[bucketIndex]++;
            }
            return true;
        }
        // Case 2: e is not in the heavy part of A[h(e)], and there are still empty cells.
        if (bucket.size() < lambdaH) {
            assert !item.startsWith(LdpHhFactory.BOT_PREFIX) : "the item must not be ⊥: " + item;
            // It inserts e into an empty cell, i.e., sets the ID field to e and sets the count field to 1.
            bucket.put(item, 1.0);
            if (ldpHhServerState.equals(LdpHhServerState.STATISTICS)) {
                currentNums[bucketIndex]++;
            }
            return true;
        }
        // Case 3: e is not in any cell in the heavy part of A[h(e)], and there is no empty cell.
        // We propose a novel technique named Exponential Decay: it decays (decrements) the count field of the weakest
        // guardian by 1 with probability P = b^{−C}, where b is a predefined constant number (e.g., b = 1.08), and C
        // is the value of the Count field of the weakest guardian.
        assert bucket.size() == lambdaH;
        // find the weakest guardian
        List<Map.Entry<String, Double>> bucketList = new ArrayList<>(bucket.entrySet());
        bucketList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Map.Entry<String, Double> weakestCell = bucketList.get(0);
        String weakestItem = weakestCell.getKey();
        double weakestCount = weakestCell.getValue();
        // Sample a boolean value, with probability P = b^{−C}, the boolean value is 1
        // In LDP, the weakest count may be non-positive, if so, we do not need to sample, since it must be evicted.
        if (weakestCount > 0) {
            // Here we use the advanced Bernoulli(exp(−γ)) with γ = C * ln(b), and reverse the sample
            ExpBernoulliSampler expBernoulliSampler = new ExpBernoulliSampler(random, weakestCount * LN_B);
            // decay (decrement) the count field of the weakest guardian by 1 with probability P = b^{−C}
            boolean sample = expBernoulliSampler.sample();
            if (!sample) {
                weakestCount--;
            }
        }
        // After decay, if the count field becomes 0, it replaces the ID field of the weakest guardian with e,
        // and sets the count field to 1
        if (weakestCount <= 0) {
            bucket.remove(weakestItem);
            if (ldpHhServerState.equals(LdpHhServerState.STATISTICS)) {
                // we partially de-bias the count for all items
                for (Map.Entry<String, Double> bucketEntry : bucket.entrySet()) {
                    bucketEntry.setValue(updateCount(bucketIndex, bucketEntry.getValue()));
                }
                currentNums[bucketIndex] = 1;
            }
            assert !item.startsWith(LdpHhFactory.BOT_PREFIX) : "the item must not be ⊥: " + item;
            bucket.put(item, 1.0);
            return true;
        } else {
            bucket.put(weakestItem, weakestCount);
            if (ldpHhServerState.equals(LdpHhServerState.STATISTICS)) {
                currentNums[bucketIndex]++;
            }
            return false;
        }
    }

    /**
     * update count when an item is evicted in the bucket while we are not in the warmup state.
     *
     * @param bucketIndex the bucket index.
     * @param count       the count stored in the bucket while we are not in the warmup state.
     * @return the updated count.
     */
    protected abstract double updateCount(int bucketIndex, double count);

    @Override
    public double response(String item) {
        byte[] itemByteArray = ObjectUtils.objectToByteArray(item);
        int bucketIndex = Math.abs(intHash.hash(itemByteArray) % w);
        // first, it checks the heavy part in bucket A[h(e)].
        Map<String, Double> bucket = buckets.get(bucketIndex);
        switch (ldpHhServerState) {
            case WARMUP:
                // return C
                return bucket.getOrDefault(item, 0.0);
            case STATISTICS:
                // return de-biased C
                return debiasCount(bucketIndex, bucket.getOrDefault(item, 0.0));
            default:
                throw new IllegalStateException("Invalid " + LdpHhServerState.class.getSimpleName() + ": " + ldpHhServerState);
        }
    }

    /**
     * De-bias count in response.
     *
     * @param bucketIndex the bucket index.
     * @param count       the biased count stored in the budget.
     * @return the de-biased count.
     */
    protected abstract double debiasCount(int bucketIndex, double count);

    @Override
    public Map<String, Double> responseHeavyHitters() {
        // we first iterate items in each budget
        Map<String, Double> countMap = buckets.stream()
            .map(Map::keySet)
            .flatMap(Set::stream)
            .collect(Collectors.toMap(item -> item, this::response));
        List<Map.Entry<String, Double>> countList = new ArrayList<>(countMap.entrySet());
        countList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Collections.reverse(countList);
        return countList.subList(0, k).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public double getWindowEpsilon() {
        return windowEpsilon;
    }

    @Override
    public int getD() {
        return d;
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
    public int getW() {
        return w;
    }

    @Override
    public int getLambdaH() {
        return lambdaH;
    }
}
