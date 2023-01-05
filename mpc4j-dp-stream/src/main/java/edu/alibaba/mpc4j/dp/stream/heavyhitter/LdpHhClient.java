package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import edu.alibaba.mpc4j.dp.stream.heavyhitter.utils.LdpHhServerContext;

import java.util.Random;

/**
 * Heavy Hitter client with Local Differential Privacy.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public interface LdpHhClient {
    /**
     * Get the type of Heavy Hitter with Local Differential Privacy.
     *
     * @return the type of Heavy Hitter with Local Differential Privacy.
     */
    LdpHhFactory.LdpHhType getType();
    /**
     * randomize the item based on the current data structure.
     *
     * @param serverContext the server context.
     * @param item          the item.
     * @param random        the random state.
     * @return the randomized item.
     */
    String randomize(LdpHhServerContext serverContext, String item, Random random);

    /**
     * randomize the item based on the current data structure.
     *
     * @param serverContext the server context.
     * @param item          the item.
     * @return the randomized item.
     */
    default String randomize(LdpHhServerContext serverContext, String item) {
        return randomize(serverContext, item, new Random());
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
     * Get the number of Heavy Hitters k.
     *
     * @return the number of Heavy Hitters.
     */
    int getK();
}
