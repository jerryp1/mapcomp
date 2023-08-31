package edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.hint;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * abstract hint for SPAM with randomly generated cutoff.
 *
 * @author Weiran Liu
 * @date 2023/8/31
 */
public abstract class AbstractRandomCutoffSpamHint extends AbstractSpamHint {
    /**
     * the cutoff ^v
     */
    protected final double cutoff;

    protected AbstractRandomCutoffSpamHint(int chunkSize, int chunkNum, int l, SecureRandom secureRandom) {
        super(chunkSize, chunkNum, l);
        // sample ^v
        boolean success = false;
        double tryCutoff = 0.0;
        while (!success) {
            secureRandom.nextBytes(hintId);
            // compute V = [v_0, v_1, ..., v_{ChunkNum}]
            double[] vs = IntStream.range(0, chunkNum)
                .mapToDouble(this::getDouble)
                .toArray();
            // we need all v in vs are distinct
            long count = Arrays.stream(vs).distinct().count();
            if (count == vs.length) {
                // all v in vs are distinct, find the median
                Arrays.sort(vs);
                double left = vs[chunkNum / 2 - 1];
                double right = vs[chunkNum / 2];
                tryCutoff = (left + right) / 2;
                success = true;
            }
        }
        cutoff = tryCutoff;
    }

    @Override
    protected double getCutoff() {
        return cutoff;
    }
}
