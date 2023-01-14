package edu.alibaba.mpc4j.dp.service.fo;

import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;

import java.util.Map;

/**
 * Frequency Oracle (FO) LDP server.
 *
 * @author Weiran Liu
 * @date 2023/1/10
 */
public interface FoLdpServer {
    /**
     * Get the type.
     *
     * @return the type.
     */
    FoLdpFactory.FoLdpType getType();

    /**
     * Aggregate a randomized item.
     *
     * @param data the encoded randomized item.
     */
    void aggregate(byte[] data);

    /**
     * Calculate frequency estimates for all items in the domain.
     *
     * @return the frequency estimates for all items in the domain.
     */
    Map<String, Double> estimate();

    /**
     * Return the privacy parameter ε.
     *
     * @return the privacy parameter ε.
     */
    double getEpsilon();

    /**
     * Gets the domain size d, i.e., |Ω|.
     *
     * @return the domain size d.
     */
    int getD();

    /**
     * Returns the total insert item num.
     *
     * @return the total insert item num.
     */
    int getNum();
}
