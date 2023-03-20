package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.ExpBernoulliSampler;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpServerState;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.tool.BucketDomain;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Abstract HeavyGuardian-based Heavy Hitter LDP server.
 *
 * @author Weiran Liu
 * @date 2022/11/19
 */
abstract class AbstractHgHhLdpServer implements HgHhLdpServer {
    /**
     * b = 1.08
     */
    private static final double B = 1.08;
    /**
     * ln(b)
     */
    private static final double LN_B = Math.log(B);
    /**
     * the config
     */
    private final HgHhLdpConfig hgHhLdpConfig;
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
     * d in each bucket
     */
    protected final int[] bucketDs;
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
     * the window size (w)
     */
    protected final int windowSize;
    /**
     * the HeavyGuardian random state
     */
    protected final Random hgRandom;
    /**
     * the total number of insert items
     */
    protected int num;
    /**
     * the state
     */
    protected HhLdpServerState hhLdpServerState;
    /**
     * current de-bias weak nums for each budget
     */
    protected int[] currentWeakNums;
    /**
     * current de-bias strong num for each budget
     */
    protected int[] currentStrongNums;

    AbstractHgHhLdpServer(HhLdpConfig config) {
        hgHhLdpConfig = (HgHhLdpConfig) config;
        d = hgHhLdpConfig.getD();
        k = hgHhLdpConfig.getK();
        w = hgHhLdpConfig.getW();
        lambdaH = hgHhLdpConfig.getLambdaH();
        // set |Ω| in each bucket, and insert empty elements in the bucket
        BucketDomain bucketDomain = new BucketDomain(hgHhLdpConfig.getDomainSet(), w, lambdaH);
        bucketDs = IntStream.range(0, w)
            .map(bucketDomain::getD)
            .toArray();
        windowEpsilon = hgHhLdpConfig.getWindowEpsilon();
        windowSize = hgHhLdpConfig.getWindowSize();
        hgRandom = hgHhLdpConfig.getHgRandom();
        // init buckets, full the budget with 0-count dummy items
        buckets = IntStream.range(0, w)
            .mapToObj(bucketIndex -> {
                ArrayList<String> bucketDomainArrayList = new ArrayList<>(bucketDomain.getBucketDomainSet(bucketIndex));
                Map<String, Double> bucket = new HashMap<>(lambdaH);
                assert bucketDomainArrayList.size() >= lambdaH;
                for (int i = 0; i < lambdaH; i++) {
                    bucket.put(bucketDomainArrayList.get(i), 0.0);
                }
                return bucket;
            })
            .collect(Collectors.toCollection(ArrayList::new));
        // init hash function
        intHash = IntHashFactory.fastestInstance();
        // init variables
        num = 0;
        currentWeakNums = new int[w];
        Arrays.fill(currentWeakNums, 0);
        currentStrongNums = new int[w];
        Arrays.fill(currentStrongNums, 0);
        hhLdpServerState = HhLdpServerState.WARMUP;
    }

    protected void checkState(HhLdpServerState expect) {
        Preconditions.checkArgument(hhLdpServerState.equals(expect), "The state must be %s: %s", expect, hhLdpServerState);
    }

    @Override
    public boolean warmupInsert(byte[] itemBytes) {
        checkState(HhLdpServerState.WARMUP);
        String item = new String(itemBytes, HhLdpFactory.DEFAULT_CHARSET);
        return insert(item);
    }

    @Override
    public boolean randomizeInsert(byte[] itemBytes) {
        checkState(HhLdpServerState.STATISTICS);
        String item = new String(itemBytes, HhLdpFactory.DEFAULT_CHARSET);
        return insert(item);
    }

    private Map.Entry<String, Double> weakestCell(int bucketIndex) {
        Map<String, Double> bucket = buckets.get(bucketIndex);
        List<Map.Entry<String, Double>> currentBucketList = new ArrayList<>(bucket.entrySet());
        currentBucketList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        return currentBucketList.get(0);
    }

    private boolean insert(String item) {
        num++;
        // it first computes the hash function h(e) (1 ⩽ h(e) ⩽ w) to map e to bucket A[h(e)].
        int bucketIndex;
        if (item.startsWith(HhLdpFactory.BOT_PREFIX)) {
            bucketIndex = Integer.parseInt(item.substring(HhLdpFactory.BOT_PREFIX.length()));
        } else {
            byte[] itemByteArray = ObjectUtils.objectToByteArray(item);
            bucketIndex = Math.abs(intHash.hash(itemByteArray) % w);
        }
        // find the weakest guardian
        Map<String, Double> bucket = buckets.get(bucketIndex);
        Map.Entry<String, Double> weakestCell = weakestCell(bucketIndex);
        String weakestItem = weakestCell.getKey();
        double weakestCount = weakestCell.getValue();
        // Case 1: e is in one cell in the heavy part of A[h(e)] (being a king or a guardian).
        if (bucket.containsKey(item)) {
            // HeavyGuardian just increments the corresponding frequency (the count field) in the cell by 1.
            double itemCount = bucket.get(item);
            itemCount += insertCount(bucketIndex, weakestCell);
            bucket.put(item, itemCount);
            if (hhLdpServerState.equals(HhLdpServerState.STATISTICS)) {
                if (weakestCount <= 1.0) {
                    currentWeakNums[bucketIndex]++;
                } else {
                    currentStrongNums[bucketIndex]++;
                }
            }
            return true;
        }
        // Case 2: e is not in the heavy part of A[h(e)], and there are still empty cells.
        if (bucket.size() < lambdaH) {
            assert !item.startsWith(HhLdpFactory.BOT_PREFIX) : "the item must not be ⊥: " + item;
            // It inserts e into an empty cell, i.e., sets the ID field to e and sets the count field to 1.
            bucket.put(item, insertCount(bucketIndex, weakestCell));
            if (hhLdpServerState.equals(HhLdpServerState.STATISTICS)) {
                if (weakestCount <= 1.0) {
                    currentWeakNums[bucketIndex]++;
                } else {
                    currentStrongNums[bucketIndex]++;
                }
            }
            return true;
        }
        // Case 3: e is not in any cell in the heavy part of A[h(e)], and there is no empty cell.
        // We propose a novel technique named Exponential Decay: it decays (decrements) the count field of the weakest
        // guardian by 1 with probability P = b^{−C}, where b is a predefined constant number (e.g., b = 1.08), and C
        // is the value of the Count field of the weakest guardian.
        assert bucket.size() == lambdaH;
        // Sample a boolean value, with probability P = b^{−C}, the boolean value is 1
        // In LDP, the weakest count may be non-positive, if so, we do not need to sample, since it must be evicted.
        if (weakestCount > 0) {
            // Here we use the advanced Bernoulli(exp(−γ)) with γ = C * ln(b), and reverse the sample
            ExpBernoulliSampler expBernoulliSampler = new ExpBernoulliSampler(hgRandom, weakestCount * LN_B);
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
            if (hhLdpServerState.equals(HhLdpServerState.STATISTICS)) {
                // we partially de-bias the count for all items
                for (Map.Entry<String, Double> bucketEntry : bucket.entrySet()) {
                    bucketEntry.setValue(updateCount(bucketIndex, bucketEntry.getValue()));
                }
                currentWeakNums[bucketIndex] = 1;
                currentStrongNums[bucketIndex] = 0;
            }
            assert !item.startsWith(HhLdpFactory.BOT_PREFIX) : "the item must not be ⊥: " + item;
            bucket.put(item, insertCount(bucketIndex, weakestCell));
            return true;
        } else {
            bucket.put(weakestItem, weakestCount);
            if (hhLdpServerState.equals(HhLdpServerState.STATISTICS)) {
                if (weakestCount <= 1.0) {
                    currentWeakNums[bucketIndex]++;
                } else {
                    currentStrongNums[bucketIndex]++;
                }
            }
            return false;
        }
    }

    /**
     * Gets insert count.
     * @param bucketIndex the bucket index.
     * @param weakestCell the weakest cell.
     * @return the insert count.
     */
    protected abstract double insertCount(int bucketIndex, Map.Entry<String, Double> weakestCell);

    /**
     * update count when an item is evicted in the bucket while we are not in the warmup state.
     *
     * @param bucketIndex the bucket index.
     * @param count       the count stored in the bucket while we are not in the warmup state.
     * @return the updated count.
     */
    protected abstract double updateCount(int bucketIndex, double count);

    protected double response(String item) {
        byte[] itemByteArray = ObjectUtils.objectToByteArray(item);
        int bucketIndex = Math.abs(intHash.hash(itemByteArray) % w);
        // first, it checks the heavy part in bucket A[h(e)].
        Map<String, Double> bucket = buckets.get(bucketIndex);
        switch (hhLdpServerState) {
            case WARMUP:
                // return C
                return bucket.getOrDefault(item, 0.0);
            case STATISTICS:
                // return de-biased C
                return debiasCount(bucketIndex, bucket.getOrDefault(item, 0.0));
            default:
                throw new IllegalStateException("Invalid " + HhLdpServerState.class.getSimpleName() + ": " + hhLdpServerState);
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
    public Map<String, Double> heavyHitters() {
        Set<String> flatKeySet = buckets.stream()
            .map(Map::keySet)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
        // we first iterate items in each budget
        Map<String, Double> countMap = flatKeySet.stream()
            .collect(Collectors.toMap(item -> item, this::response));
        List<Map.Entry<String, Double>> countList = new ArrayList<>(countMap.entrySet());
        countList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Collections.reverse(countList);
        if (flatKeySet.size() <= k) {
            // the current key set is less than k, return all items
            return countList.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            return countList.subList(0, k).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    @Override
    public HhLdpFactory.HhLdpType getType() {
        return hgHhLdpConfig.getType();
    }

    @Override
    public double getWindowEpsilon() {
        return windowEpsilon;
    }

    @Override
    public int getWindowSize() {
        return windowSize;
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
