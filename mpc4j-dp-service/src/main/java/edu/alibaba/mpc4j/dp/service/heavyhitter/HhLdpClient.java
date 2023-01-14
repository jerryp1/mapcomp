package edu.alibaba.mpc4j.dp.service.heavyhitter;

import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HhLdpServerContext;

import java.util.Random;

/**
 * Heavy Hitter LDP client.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public interface HhLdpClient {
    /**
     * Get the type of Heavy Hitter with Local Differential Privacy.
     *
     * @return the type of Heavy Hitter with Local Differential Privacy.
     */
    HhLdpFactory.HhLdpType getType();
    /**
     * randomize the item based on the current data structure.
     *
     * @param serverContext the server context.
     * @param item          the item.
     * @param random        the random state.
     * @return the randomized item.
     */
    String randomize(HhLdpServerContext serverContext, String item, Random random);

    /**
     * randomize the item based on the current data structure.
     *
     * @param serverContext the server context.
     * @param item          the item.
     * @return the randomized item.
     */
    default String randomize(HhLdpServerContext serverContext, String item) {
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
