package edu.alibaba.mpc4j.dp.stream.heavyhitter;

/**
 * Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
 *
 * @author Weiran Liu
 * @date 2022/11/20
 */
public interface HhgLdpHeavyHitter extends HgLdpHeavyHitter {
    /**
     * Get the value of α.
     *
     * @return the value of α.
     */
    double getAlpha();
}