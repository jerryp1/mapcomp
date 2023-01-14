package edu.alibaba.mpc4j.dp.service.fo;

/**
 * Frequency Oracle (FO) LDP server.
 *
 * @author Weiran Liu
 * @date 2023/1/10
 */
public interface FoLdpServer {
    /**
     * Aggregate a randomized item.
     *
     * @param data the encoded randomized item.
     */
    void aggregate(byte[] data);

    /**
     * Calculates frequency estimate of the given item.
     *
     * @param item item to estimate the frequency.
     * @return the frequency estimate result.
     */
    double estimate(String item);

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
