package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Interface for Heavy Hitter with Local Differentially Privacy.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public interface LdpHeavyHitter {

    /**
     * Get the type of Heavy Hitter with Local Differential Privacy.
     *
     * @return the type of Heavy Hitter with Local Differential Privacy.
     */
    LdpHeavyHitterFactory.LdpHeavyHitterType getType();

    /**
     * Randomize the item.
     *
     * @param item the item.
     * @param random the random state.
     * @return the randomized item.
     */
    String randomize(String item, Random random);

    /**
     * Insert a randomized item.
     *
     * @param randomizedItem a randomized item.
     * @return return true if the randomized item is not ignored and successfully inserted.
     */
    boolean insert(String randomizedItem);

    /**
     * Response the counting value for the given item.
     *
     * @param item the item.
     * @return the counting value for that item.
     */
    double response(String item);

    /**
     * Response counting values for all items.
     *
     * @return counting values for all items.
     */
    default Map<String, Double> responseDomain() {
        return getDomainSet().stream().collect(Collectors.toMap(item -> item, this::response));
    }

    /**
     * Response counting values for all items with descending order list.
     *
     * @return counting values for all items with descending order list.
     */
    default List<Map.Entry<String, Double>> responseOrderedDomain() {
        Map<String, Double> countMap = responseDomain();
        List<Map.Entry<String, Double>> countList = new ArrayList<>(countMap.entrySet());
        // descending sort
        countList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Collections.reverse(countList);
        return countList;
    }

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
     * Response Heavy Hitters.
     *
     * @return Heavy Hitters.
     */
    Map<String, Double> responseHeavyHitters();

    /**
     * Return the privacy parameter ε.
     *
     * @return the privacy parameter ε.
     */
    double getEpsilon();

    /**
     * Get the number of Heavy Hitters k.
     *
     * @return the number of Heavy Hitters.
     */
    int getK();

    /**
     * Return the total insert item num.
     *
     * @return the total insert item num.
     */
    int getNum();

    /**
     * Get the data domain.
     *
     * @return the data domain.
     */
    Set<String> getDomainSet();

    /**
     * Get the heavy hitter set.
     *
     * @return the heavy hitter set.
     */
    Set<String> getHeavyHitterSet();
}