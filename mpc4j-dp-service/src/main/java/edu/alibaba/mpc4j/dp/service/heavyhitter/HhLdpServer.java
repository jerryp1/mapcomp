package edu.alibaba.mpc4j.dp.service.heavyhitter;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HhLdpServerContext;

import java.util.*;

/**
 * Heavy Hitter LDP server.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public interface HhLdpServer {
    /**
     * Get the type of Heavy Hitter with Local Differential Privacy.
     *
     * @return the type of Heavy Hitter with Local Differential Privacy.
     */
    HhLdpFactory.HhLdpType getType();

    /**
     * Insert an item during the warmup state.
     *
     * @param item the item.
     * @return true if the randomized item is not ignored and successfully inserted.
     */
    @CanIgnoreReturnValue
    boolean warmupInsert(String item);

    /**
     * Stop warming up.
     *
     * @throws IllegalStateException If warming up is not enough.
     */
    void stopWarmup();

    /**
     * Return the server context.
     *
     * @return the server context.
     */
    HhLdpServerContext getServerContext();

    /**
     * Insert a randomized item.
     *
     * @param randomizedItem the randomized item.
     * @return true if the randomized item is not ignored and successfully inserted.
     */
    @CanIgnoreReturnValue
    boolean randomizeInsert(String randomizedItem);

    /**
     * Response Heavy Hitters.
     *
     * @return Heavy Hitters.
     */
    Map<String, Double> responseHeavyHitters();

    /**
     * Response Heavy Hitters with descending order list.
     *
     * @return the heavy hitter map.
     */
    default List<Map.Entry<String, Double>> responseOrderedHeavyHitters() {
        // Iterate all items in the domain set, then choose the top-k items.
        Map<String, Double> heavyHitterMap = responseHeavyHitters();
        List<Map.Entry<String, Double>> heavyHitterOrderedList = new ArrayList<>(heavyHitterMap.entrySet());
        // descending sort
        heavyHitterOrderedList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Collections.reverse(heavyHitterOrderedList);

        return heavyHitterOrderedList;
    }

    /**
     * Return the privacy parameter ε / w.
     *
     * @return the privacy parameter ε / w.
     */
    double getWindowEpsilon();

    /**
     * Gets the domain size d, i.e., |Ω|.
     *
     * @return the domain size d.
     */
    int getD();

    /**
     * Gets the number of Heavy Hitters k.
     *
     * @return the number of Heavy Hitters.
     */
    int getK();

    /**
     * Returns the total insert item num.
     *
     * @return the total insert item num.
     */
    int getNum();
}
