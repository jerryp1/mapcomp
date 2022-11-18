package edu.alibaba.mpc4j.dp.stream.structure;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.ExpBernoulliSampler;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.dp.stream.tool.bobhash.BobIntHash;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * HeavyGuardian节点。
 *
 * @author Weiran Liu
 * @date 2022/11/15
 */
public class HeavyGuardian implements StreamCounter {
    /**
     * b = 1.08
     */
    private static final double B = 1.08;
    /**
     * ln(b)
     */
    private static final double LN_B = Math.log(B);
    /**
     * 哈希函数
     */
    private final BobIntHash bobIntHash;
    /**
     * 桶数量
     */
    private final int w;
    /**
     * λ_h, i.e., the cell num in the heavy part
     */
    private final int lambdaH;
    /**
     * heavy part, w buckets, each bucket has λ_h cells
     */
    private final ArrayList<Map<String, Integer>> heavyPart;
    /**
     * λ_l, i.e., the cell num in the light part
     */
    private final int lambdaL;
    /**
     * light part, w buckets, each bucket has λ_l cells
     */
    private final ArrayList<Map<String, Integer>> lightPart;
    /**
     * recorded item set
     */
    private final Set<String> recordItemSet;
    /**
     * the total number of insert items
     */
    private int insertNum;

    public HeavyGuardian(int w, int lambdaH, int lambdaL) {
        this(w, lambdaH, lambdaL, 0);
    }

    public HeavyGuardian(int w, int lambdaH, int lambdaL, int primeIndex) {
        Preconditions.checkArgument(w > 0,
            "w (# of buckets) must be greater than 0: %s", w);
        this.w = w;
        // init heavy part
        Preconditions.checkArgument(lambdaH > 0,
            "λ_h (# of heavy part) must be greater than 0: %s", lambdaH);
        this.lambdaH = lambdaH;
        heavyPart = IntStream.range(0, w)
            .mapToObj(bucketIndex -> new HashMap<String, Integer>(lambdaH))
            .collect(Collectors.toCollection(ArrayList::new));
        // init light part
        Preconditions.checkArgument(lambdaL >= 0,
            "λ_l (# of light part) must be greater than or equal to 0: %s", lambdaL);
        this.lambdaL = lambdaL;
        lightPart = IntStream.range(0, w)
            .mapToObj(bucketIndex -> new HashMap<String, Integer>(lambdaL))
            .collect(Collectors.toCollection(ArrayList::new));
        // init bob hash
        bobIntHash = new BobIntHash(primeIndex);
        // set the initial set size as w * (λ_h + λ_l)
        recordItemSet = new HashSet<>(w * (lambdaH + lambdaL));
        insertNum = 0;
    }

    @Override
    public boolean insert(String item) {
        insertNum++;
        // it first computes the hash function h(e) (1 ⩽ h(e) ⩽ w) to map e to bucket A[h(e)].
        byte[] itemByteArray = ObjectUtils.objectToByteArray(item);
        int bucketIndex = Math.abs(bobIntHash.hash(itemByteArray) % w);
        Map<String, Integer> heavyPartBucket = heavyPart.get(bucketIndex);
        // We first try to insert e into the heavy part. If failed, then we insert it into the light part.
        // Case 1: e is in one cell in the heavy part of A[h(e)] (being a king or a guardian).
        if (heavyPartBucket.containsKey(item)) {
            // HeavyGuardian just increments the corresponding frequency (the count field) in the cell by 1.
            int itemCount = heavyPartBucket.get(item);
            itemCount++;
            heavyPartBucket.put(item, itemCount);
            return true;
        }
        // Case 2: e is not in the heavy part of A[h(e)], and there are still empty cells.
        if (heavyPartBucket.size() < lambdaH) {
            // It inserts e into an empty cell, i.e., sets the ID field to e and sets the count field to 1.
            heavyPartBucket.put(item, 1);
            recordItemSet.add(item);
            return true;
        }
        // Case 3: e is not in any cell in the heavy part of A[h(e)], and there is no empty cell.
        // We propose a novel technique named Exponential Decay: it decays (decrements) the count field of the weakest
        // guardian by 1 with probability P = b^{−C}, where b is a predefined constant number (e.g., b = 1.08), and C
        // is the value of the Count field of the weakest guardian.
        assert heavyPartBucket.size() == lambdaH;
        // find the weakest guardian
        List<Map.Entry<String, Integer>> heavyPartList = new ArrayList<>(heavyPartBucket.entrySet());
        heavyPartList.sort(Comparator.comparingInt(Map.Entry::getValue));
        Map.Entry<String, Integer> weakestHeavyPartCell = heavyPartList.get(0);
        String weakestHeavyPartItem = weakestHeavyPartCell.getKey();
        int weakestHeavyPartCount = weakestHeavyPartCell.getValue();
        // Sample a boolean value, with probability P = b^{−C}, the boolean value is 1
        // Here we use the advanced Bernoulli(exp(−γ)) with γ = C * ln(b), and reverse the sample
        ExpBernoulliSampler expBernoulliSampler = new ExpBernoulliSampler(weakestHeavyPartCount * LN_B);
        // decay (decrement) the count field of the weakest guardian by 1 with probability P = b^{−C}
        boolean sample = expBernoulliSampler.sample();
        if (!sample) {
            weakestHeavyPartCount--;
        }
        // After decay, if the count field becomes 0, it replaces the ID field of the weakest guardian with e,
        // and sets the count field to 1
        if (weakestHeavyPartCount == 0) {
            heavyPartBucket.remove(weakestHeavyPartItem);
            recordItemSet.remove(weakestHeavyPartItem);
            heavyPartBucket.put(item, 1);
            recordItemSet.add(item);
            return true;
        } else {
            heavyPartBucket.put(weakestHeavyPartItem, weakestHeavyPartCount);
        }
        // otherwise, it inserts e into the light part.
        // To insert an item e to the light part, it first computes another hash function h′(e), and then increments
        // counter A[h(e)][h′(e)]_l in the light part of the bucket by 1.
        Map<String, Integer> lightPartBucket = lightPart.get(bucketIndex);
        // Case 1: e is in one cell in the light part of A[h(e)]_l
        if (lightPartBucket.containsKey(item)) {
            // HeavyGuardian just increments the corresponding frequency (the count field) in the cell by 1.
            int itemCount = lightPartBucket.get(item);
            itemCount++;
            lightPartBucket.put(item, itemCount);
            return true;
        }
        // Case 2: e is not in the light part of A[h(e)], and there are still empty cells.
        if (lightPartBucket.size() < lambdaL) {
            // It inserts e into an empty cell, i.e., sets the ID field to e and sets the count field to 1.
            lightPartBucket.put(item, 1);
            recordItemSet.add(item);
            return true;
        }
        // Case 3: e is not in any cell in the heavy part of A[h(e)], and there is no empty cell, return false
        return false;
    }

    @Override
    public int query(String item) {
        byte[] itemByteArray = ObjectUtils.objectToByteArray(item);
        int bucketIndex = Math.abs(bobIntHash.hash(itemByteArray) % w);
        // first, it checks the heavy part in bucket A[h(e)].
        Map<String, Integer> heavyPartBucket = heavyPart.get(bucketIndex);
        if (heavyPartBucket.containsKey(item)) {
            // If e matches a cell in the bucket, it reports the corresponding count field
            return heavyPartBucket.get(item);
        }
        // if e matches no cell, it reports counter A[h(e)][h′(e)]_l in the light part.
        Map<String, Integer> lightPartBucket = lightPart.get(bucketIndex);
        if (lightPartBucket.containsKey(item)) {
            return lightPartBucket.get(item);
        }
        return 0;
    }

    @Override
    public int getInsertNum() {
        return insertNum;
    }

    @Override
    public Set<String> getRecordItemSet() {
        return recordItemSet;
    }

    /**
     * Return the bucket num w.
     *
     * @return the bucket num w.
     */
    public int getW() {
        return w;
    }

    /**
     * Return the cell num λ_h in the heavy part.
     *
     * @return the cell num λ_h in the heavy part.
     */
    public int getLambdaH() {
        return lambdaH;
    }

    /**
     * Return the cell num λ_l in the light part.
     *
     * @return the cell num λ_l in the light part.
     */
    public int getLambdaL() {
        return lambdaL;
    }
}
