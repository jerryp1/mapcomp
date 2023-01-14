package edu.alibaba.mpc4j.dp.service.fo;

/**
 * Frequency Oracle (FO) LDP client.
 *
 * @author Weiran Liu
 * @date 2023/1/10
 */
public interface FoLdpClient {
    /**
     * randomizes and encodes the user's item.
     *
     * @param item the user's item.
     * @return the encoded randomized item.
     */
    byte[] randomize(String item);

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
}
