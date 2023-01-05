package edu.alibaba.mpc4j.dp.stream.heavyhitter.hg;

/**
 * Heavy Hitter client with Local Differential Privacy based on Hot HeavyGuardian.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public interface HhgLdpHhClient extends HgLdpHhClient {
    /**
     * Get the value of α.
     *
     * @return the value of α.
     */
    double getAlpha();
}
