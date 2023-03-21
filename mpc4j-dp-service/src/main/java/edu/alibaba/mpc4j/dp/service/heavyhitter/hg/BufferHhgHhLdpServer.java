package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.ExpBernoulliSampler;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.dp.service.heavyhitter.AbstractHhLdpServer;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpServerState;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.BufferHhgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HgHhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.tool.BucketDomain;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Buffer Hot HeavyGuardian-based Heavy Hitter LDP server.
 *
 * @author Weiran Liu
 * @date 2023/3/21
 */
public class BufferHhgHhLdpServer extends AbstractHhLdpServer implements HhgHhLdpServer {
    /**
     * b = 1.08
     */
    private static final double B = 1.08;
    /**
     * ln(b)
     */
    private static final double LN_B = Math.log(B);
    /**
     * the non-cryptographic 32-bit hash function
     */
    private final IntHash intHash;
    /**
     * budget num
     */
    private final int w;
    /**
     * d in each bucket
     */
    protected final int[] bucketDs;
    /**
     * λ_h, i.e., the cell num in each bucket
     */
    private final int lambdaH;
    /**
     * λ_l, i.e., the buffer num in each bucket
     */
    private final int lambdaL;
    /**
     * w heavy buckets, each bucket has λ_h cells
     */
    private final ArrayList<Map<String, Double>> heavyBuckets;
    /**
     * w light buckets, each bucket has λ_l cells
     */
    private final ArrayList<Map<String, Double>> lightBuckets;
    /**
     * the HeavyGuardian random state
     */
    private final Random hgRandom;
    /**
     * current de-bias heavy nums for each budget
     */
    private final int[] currentHeavyNums;
    /**
     * current de-bias light nums for each budget
     */
    private final int[] currentLightNums;
    /**
     * p1 = e^ε_1 / (e^ε_1 + 1)
     */
    private final double p1;
    /**
     * q1 = 1 / (e^ε_1 + 1)
     */
    private final double q1;
    /**
     * p2 = e^ε_2 / (e^ε_2 + λ_h - 1)
     */
    private final double p2;
    /**
     * q2 = 1 / (e^ε_2 + λ_h - 1)
     */
    private final double q2;
    /**
     * p3 = e^ε_3 / (e^ε_3 + d - λ_h - 1)
     */
    private final double[] p3s;
    /**
     * q3 = 1 / (e^ε_3 + d - λ_h - 1)
     */
    private final double[] q3s;
    /**
     * γ_h, proportion of hot items
     */
    private double gammaH;
    /**
     * the total number of insert items
     */
    private int num;

    public BufferHhgHhLdpServer(BufferHhgHhLdpConfig config) {
        super(config);
        w = config.getW();
        lambdaH = config.getLambdaH();
        lambdaL = config.getLambdaL();
        // set |Ω| in each bucket, and insert empty elements in the bucket
        BucketDomain bucketDomain = new BucketDomain(config.getDomainSet(), w, lambdaH);
        hgRandom = config.getHgRandom();
        // init heavy and light buckets
        bucketDs = new int[w];
        heavyBuckets = new ArrayList<>(w);
        lightBuckets = new ArrayList<>(w);
        IntStream.range(0, w).forEach(bucketIndex -> {
            ArrayList<String> bucketDomainArrayList = new ArrayList<>(bucketDomain.getBucketDomainSet(bucketIndex));
            int bucketD = bucketDomainArrayList.size();
            assert bucketD >= lambdaH;
            // init the bucket domain
            bucketDs[bucketIndex] = bucketD;
            // init the heavy bucket, full the budget with 0-count dummy items
            Map<String, Double> heavyBucket = new HashMap<>(lambdaH);
            for (int i = 0; i < lambdaH; i++) {
                heavyBucket.put(bucketDomainArrayList.get(i), 0.0);
            }
            heavyBuckets.add(heavyBucket);
            // init the light bucket
            Map<String, Double> lightBucket = new HashMap<>(lambdaL);
            lightBuckets.add(lightBucket);
        });
        // init hash function
        intHash = IntHashFactory.fastestInstance();
        // init variables
        num = 0;
        currentHeavyNums = new int[w];
        Arrays.fill(currentHeavyNums, 0);
        currentLightNums = new int[w];
        Arrays.fill(currentLightNums, 0);
        // set privacy parameters
        double alpha = config.getAlpha();
        double alphaWindowEpsilon = windowEpsilon * alpha;
        double remainedWindowEpsilon = windowEpsilon - alphaWindowEpsilon;
        // compute p1 and p1
        double expAlphaWindowEpsilon = Math.exp(alphaWindowEpsilon);
        p1 = expAlphaWindowEpsilon / (expAlphaWindowEpsilon + 1);
        q1 = 1 / (expAlphaWindowEpsilon + 1);
        // compute p2 and q2
        double expRemainedWindowEpsilon = Math.exp(remainedWindowEpsilon);
        p2 = expRemainedWindowEpsilon / (expRemainedWindowEpsilon + lambdaH - 1);
        q2 = 1 / (expRemainedWindowEpsilon + lambdaH - 1);
        // compute p3 and q3
        p3s = IntStream.range(0, w)
            .mapToDouble(bucketIndex -> {
                int bucketD = bucketDs[bucketIndex];
                return expRemainedWindowEpsilon / (expRemainedWindowEpsilon + bucketD - lambdaH - 1);
            })
            .toArray();
        q3s = IntStream.range(0, w)
            .mapToDouble(bucketIndex -> {
                int bucketD = bucketDomain.getD(bucketIndex);
                return 1 / (expRemainedWindowEpsilon + bucketD - lambdaH - 1);
            })
            .toArray();
        gammaH = config.getGammaH();
    }

    @Override
    public boolean warmupInsert(byte[] itemBytes) {
        checkState(HhLdpServerState.WARMUP);
        String item = new String(itemBytes, HhLdpFactory.DEFAULT_CHARSET);
        return heavyInsert(item);
    }

    @Override
    public void stopWarmup() {
        checkState(HhLdpServerState.WARMUP);
        double hotNum = 0;
        for (int budgetIndex = 0; budgetIndex < w; budgetIndex++) {
            Map<String, Double> budget = heavyBuckets.get(budgetIndex);
            // bias all counts and calculate λ
            for (Map.Entry<String, Double> entry : budget.entrySet()) {
                String item = entry.getKey();
                double value = entry.getValue();
                hotNum += value;
                value = value * getHeavyDebiasFactor();
                budget.put(item, value);
            }
        }
        // There are two ways of setting γ_H: (1) based on the priori knowledge; (2) warm-up setting.
        // If we manually set γ_H, it must be in range [0, 1], we do not need to update it. Otherwise, we compute it.
        if (gammaH < 0) {
            Preconditions.checkArgument(num > 0, "need warmup without manually set γ_H");
            gammaH = hotNum / num;
            assert gammaH >= 0 && gammaH <= 1 : "γ_h must be in range [0, 1]: " + gammaH;
        }
        hhLdpServerState = HhLdpServerState.STATISTICS;
    }

    @Override
    public boolean randomizeInsert(byte[] itemBytes) {
        checkState(HhLdpServerState.STATISTICS);
        String item = new String(itemBytes, HhLdpFactory.DEFAULT_CHARSET);
        byte[] itemByteArray = ObjectUtils.objectToByteArray(item);
        int bucketIndex = Math.abs(intHash.hash(itemByteArray) % w);

        if (heavyBuckets.get(bucketIndex).containsKey(item)) {
            // a hot item, insert heavy part
            return heavyInsert(item);
        } else {
            // a cold item, insert cold part
            boolean success = lightInsert(item);
            if (success) {
                trySwitch(bucketIndex);
            }
            return success;
        }
    }

    private void debias(int bucketIndex) {
        Map<String, Double> heavyBucket = heavyBuckets.get(bucketIndex);
        // we partially de-bias the count for all items
        for (Map.Entry<String, Double> heavyBucketEntry : heavyBucket.entrySet()) {
            heavyBucketEntry.setValue(updateHeavyCount(bucketIndex, heavyBucketEntry.getValue()));
        }
        Map<String, Double> lightBucket = lightBuckets.get(bucketIndex);
        for (Map.Entry<String, Double> bucketEntry : lightBucket.entrySet()) {
            bucketEntry.setValue(updateLightCount(bucketIndex, bucketEntry.getValue()));
        }
        currentHeavyNums[bucketIndex] = 0;
        currentLightNums[bucketIndex] = 0;
    }

    private double updateHeavyCount(int bucketIndex, double count) {
        int n = currentHeavyNums[bucketIndex] + currentLightNums[bucketIndex];
        double nh = n * gammaH;
        // count = (count + Nh * (q1 / k - p1 * q2) - (q1 * n / k)) / (p1 * (p2 - q2))
        return count + nh * (q1 / k - p1 * q2) - n * q1 / lambdaH;
    }

    private double getHeavyDebiasFactor() {
        return p1 * (p2 - q2);
    }

    private double debiasHeavyCount(int bucketIndex, double count) {
        // count = (count + Nh * (q1 / k - p1 * q2) - (q1 * n / k)) / (p1 * (p2 - q2))
        return updateHeavyCount(bucketIndex, count) / getHeavyDebiasFactor();
    }

    private double getLightDebiasFactor(int bucketIndex) {
        double p3 = p3s[bucketIndex];
        double q3 = q3s[bucketIndex];
        return p1 * (p3 - q3);
    }

    private double updateLightCount(int bucketIndex, double count) {
        int n = currentHeavyNums[bucketIndex] + currentLightNums[bucketIndex];
        double nh = n * gammaH;
        double q3 = q3s[bucketIndex];
        int d = bucketDs[bucketIndex];
        // count = count - q3 * p1 * (n - Nh) - q1 * Nh / (d - k)) / (p1 * (p3 - q3)
        return count - q3 * p1 * (n - nh) - q1 * nh / (d - lambdaH);
    }

    private boolean heavyInsert(String item) {
        num++;
        // it first computes the hash function h(e) (1 ⩽ h(e) ⩽ w) to map e to bucket A[h(e)].
        byte[] itemByteArray = ObjectUtils.objectToByteArray(item);
        int bucketIndex = Math.abs(intHash.hash(itemByteArray) % w);
        Map<String, Double> heavyBucket = heavyBuckets.get(bucketIndex);
        // Case 1: e is in one cell in the heavy part of A[h(e)] (being a king or a guardian).
        if (heavyBucket.containsKey(item)) {
            // HeavyGuardian just increments the corresponding frequency (the count field) in the cell by 1.
            double itemCount = heavyBucket.get(item);
            itemCount += 1.0;
            heavyBucket.put(item, itemCount);
            if (hhLdpServerState.equals(HhLdpServerState.STATISTICS)) {
                currentHeavyNums[bucketIndex]++;
            }
            return true;
        }
        // Case 2: e is not in the heavy part of A[h(e)], and there are still empty cells.
        if (heavyBucket.size() < lambdaH) {
            // It inserts e into an empty cell, i.e., sets the ID field to e and sets the count field to 1.
            heavyBucket.put(item, 1.0);
            if (hhLdpServerState.equals(HhLdpServerState.STATISTICS)) {
                currentHeavyNums[bucketIndex]++;
            }
            return true;
        }
        // Case 3: e is not in any cell in the heavy part of A[h(e)], and there is no empty cell.
        // We propose a novel technique named Exponential Decay: it decays (decrements) the count field of the weakest
        // guardian by 1 with probability P = b^{−C}, where b is a predefined constant number (e.g., b = 1.08), and C
        // is the value of the Count field of the weakest guardian.
        assert heavyBucket.size() == lambdaH;
        // find the weakest guardian
        List<Map.Entry<String, Double>> heavyBucketList = new ArrayList<>(heavyBucket.entrySet());
        heavyBucketList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Map.Entry<String, Double> weakestHeavyCell = heavyBucketList.get(0);
        String weakestHeavyItem = weakestHeavyCell.getKey();
        double weakestHeavyCount = weakestHeavyCell.getValue();
        // Sample a boolean value, with probability P = b^{−C}, the boolean value is 1
        // In LDP, the weakest count may be non-positive, if so, we do not need to sample, since it must be evicted.
        if (weakestHeavyCount > 0) {
            // Here we use the advanced Bernoulli(exp(−γ)) with γ = C * ln(b), and reverse the sample
            ExpBernoulliSampler expBernoulliSampler = new ExpBernoulliSampler(hgRandom, weakestHeavyCount * LN_B);
            // decay (decrement) the count field of the weakest guardian by 1 with probability P = b^{−C}
            boolean sample = expBernoulliSampler.sample();
            if (!sample) {
                weakestHeavyCount--;
            }
        }
        // After decay, if the count field becomes 0, it replaces the ID field of the weakest guardian with e,
        // and sets the count field to 1
        if (weakestHeavyCount <= 0) {
            if (hhLdpServerState.equals(HhLdpServerState.STATISTICS)) {
                // we partially de-bias the count for all items
                debias(bucketIndex);
                currentHeavyNums[bucketIndex] = 1;
            }
            heavyBucket.remove(weakestHeavyItem);
            heavyBucket.put(item, 1.0);
            return true;
        } else {
            heavyBucket.put(weakestHeavyItem, weakestHeavyCount);
            if (hhLdpServerState.equals(HhLdpServerState.STATISTICS)) {
                currentHeavyNums[bucketIndex]++;
            }
            return false;
        }
    }

    private boolean lightInsert(String item) {
        num++;
        // it first computes the hash function h(e) (1 ⩽ h(e) ⩽ w) to map e to bucket A[h(e)].
        byte[] itemByteArray = ObjectUtils.objectToByteArray(item);
        int bucketIndex = Math.abs(intHash.hash(itemByteArray) % w);
        Map<String, Double> lightBucket = lightBuckets.get(bucketIndex);
        // Case 1: e is in one cell in the heavy part of A[h(e)] (being a king or a guardian).
        if (lightBucket.containsKey(item)) {
            // HeavyGuardian just increments the corresponding frequency (the count field) in the cell by 1.
            double itemCount = lightBucket.get(item);
            itemCount += 1.0;
            lightBucket.put(item, itemCount);
            if (hhLdpServerState.equals(HhLdpServerState.STATISTICS)) {
                currentLightNums[bucketIndex]++;
            }
            return true;
        }
        // Case 2: e is not in the heavy part of A[h(e)], and there are still empty cells.
        if (lightBucket.size() < lambdaL) {
            // It inserts e into an empty cell, i.e., sets the ID field to e and sets the count field to 1.
            lightBucket.put(item, 1.0);
            if (hhLdpServerState.equals(HhLdpServerState.STATISTICS)) {
                currentLightNums[bucketIndex]++;
            }
            return true;
        }
        // Case 3: e is not in any cell in the heavy part of A[h(e)], and there is no empty cell.
        // We propose a novel technique named Exponential Decay: it decays (decrements) the count field of the weakest
        // guardian by 1 with probability P = b^{−C}, where b is a predefined constant number (e.g., b = 1.08), and C
        // is the value of the Count field of the weakest guardian.
        assert lightBucket.size() == lambdaL;
        // find the weakest guardian
        List<Map.Entry<String, Double>> listBucketList = new ArrayList<>(lightBucket.entrySet());
        listBucketList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Map.Entry<String, Double> weakestLightCell = listBucketList.get(0);
        String weakestLightItem = weakestLightCell.getKey();
        double weakestLightCount = weakestLightCell.getValue();
        // Sample a boolean value, with probability P = b^{−C}, the boolean value is 1
        // In LDP, the weakest count may be non-positive, if so, we do not need to sample, since it must be evicted.
        if (weakestLightCount > 0) {
            // Here we use the advanced Bernoulli(exp(−γ)) with γ = C * ln(b), and reverse the sample
            ExpBernoulliSampler expBernoulliSampler = new ExpBernoulliSampler(hgRandom, weakestLightCount * LN_B);
            // decay (decrement) the count field of the weakest guardian by 1 with probability P = b^{−C}
            boolean sample = expBernoulliSampler.sample();
            if (!sample) {
                weakestLightCount--;
            }
        }
        // After decay, if the count field becomes 0, it replaces the ID field of the weakest guardian with e,
        // and sets the count field to 1
        if (weakestLightCount <= 0) {
            if (hhLdpServerState.equals(HhLdpServerState.STATISTICS)) {
                debias(bucketIndex);
                currentLightNums[bucketIndex] = 1;
            }
            lightBucket.remove(weakestLightItem);
            lightBucket.put(item, 1.0);
            return true;
        } else {
            lightBucket.put(weakestLightItem, weakestLightCount);
            if (hhLdpServerState.equals(HhLdpServerState.STATISTICS)) {
                currentLightNums[bucketIndex]++;
            }
            return false;
        }
    }

    private void trySwitch(int bucketIndex) {
        // debias
        debias(bucketIndex);
        Map<String, Double> heavyBucket = heavyBuckets.get(bucketIndex);
        Map<String, Double> lightBucket = lightBuckets.get(bucketIndex);
        // find the weakest heavy cell
        List<Map.Entry<String, Double>> heavyBucketList = new ArrayList<>(heavyBucket.entrySet());
        heavyBucketList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Map.Entry<String, Double> weakestHeavyCell = heavyBucketList.get(0);
        double heavyDebiasFactor = getHeavyDebiasFactor();
        double weakestHeavyCount = weakestHeavyCell.getValue();
        // find the strongest light cell
        List<Map.Entry<String, Double>> lightBucketList = new ArrayList<>(lightBucket.entrySet());
        lightBucketList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Map.Entry<String, Double> strongestLightCell = lightBucketList.get(lightBucketList.size() - 1);
        double lightDebiasFactor = getLightDebiasFactor(bucketIndex);
        double strongestLightCount = strongestLightCell.getValue();
        if (weakestHeavyCount < strongestLightCount) {
            // switch
            heavyBucket.remove(weakestHeavyCell.getKey());
            heavyBucket.put(strongestLightCell.getKey(), strongestLightCount / lightDebiasFactor * heavyDebiasFactor);
            lightBucket.remove(strongestLightCell.getKey());
            lightBucket.put(weakestHeavyCell.getKey(), weakestHeavyCount / heavyDebiasFactor * lightDebiasFactor);
        }
    }

    @Override
    public Map<String, Double> heavyHitters() {
        Set<String> flatKeySet = heavyBuckets.stream()
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

    private double response(String item) {
        byte[] itemByteArray = ObjectUtils.objectToByteArray(item);
        int bucketIndex = Math.abs(intHash.hash(itemByteArray) % w);
        // first, it checks the heavy part in bucket A[h(e)].
        Map<String, Double> heavyBucket = heavyBuckets.get(bucketIndex);
        switch (hhLdpServerState) {
            case WARMUP:
                // return C
                return heavyBucket.getOrDefault(item, 0.0);
            case STATISTICS:
                // return de-biased C
                return debiasHeavyCount(bucketIndex, heavyBucket.getOrDefault(item, 0.0));
            default:
                throw new IllegalStateException("Invalid " + HhLdpServerState.class.getSimpleName() + ": " + hhLdpServerState);
        }
    }

    @Override
    public int getNum() {
        return num;
    }

    @Override
    public double getGammaH() {
        return gammaH;
    }

    @Override
    public HgHhLdpServerContext getServerContext() {
        return new HgHhLdpServerContext(heavyBuckets);
    }
}
