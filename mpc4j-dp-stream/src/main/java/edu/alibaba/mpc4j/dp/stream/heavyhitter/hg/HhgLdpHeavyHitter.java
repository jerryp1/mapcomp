package edu.alibaba.mpc4j.dp.stream.heavyhitter.hg;

/**
 * Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy based on Hot HeavyGuardian.
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
