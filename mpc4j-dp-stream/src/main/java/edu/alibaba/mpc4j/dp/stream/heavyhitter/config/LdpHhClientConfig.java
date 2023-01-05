package edu.alibaba.mpc4j.dp.stream.heavyhitter.config;

import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhFactory;

import java.util.Set;

/**
 * Heavy hitter with local differential privacy config.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public interface LdpHhClientConfig {
    /**
     * Gets the type.
     *
     * @return the type.
     */
    LdpHhFactory.LdpHhType getType();

    /**
     * Gets the domain set.
     *
     * @return the domain set.
     */
    Set<String> getDomainSet();

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

    /**
     * Return the privacy parameter ε / w.
     *
     * @return the privacy parameter ε / w.
     */
    double getWindowEpsilon();
}
