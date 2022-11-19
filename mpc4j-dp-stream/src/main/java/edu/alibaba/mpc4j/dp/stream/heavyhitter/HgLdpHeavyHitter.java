package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import java.util.Random;
import java.util.Set;

/**
 * Interface for HeavyGuardian-based Heavy Hitter with Local Differentially Privacy.
 *
 * @author Weiran Liu
 * @date 2022/11/19
 */
public interface HgLdpHeavyHitter extends LdpHeavyHitter {
    /**
     * randomize the item based on the current heavy hitter set.
     *
     * @param currentHeavyHitterSet the current heavy hitter set.
     * @param item the item.
     * @param random the random state.
     * @return the randomized item.
     */
    String randomize(Set<String> currentHeavyHitterSet, String item, Random random);

    /**
     * Return if the user uses the optimized randomize procedure (randomize based on the current heavy hitter set.)
     *
     * @return return true if the user uses the optimized randomize procedure.
     */
    boolean optimizeRandomize();

    /**
     * Return the number of items the server used for warming up.
     *
     * @return the number of items the server used for warming up.
     */
    int getWarmupNum();
}
