package edu.alibaba.mpc4j.dp.stream.heavyhitter.hg;

/**
 * Heavy Hitter server with Local Differential Privacy based on Hot HeavyGuardian.
 *
 * @author Weiran Liu
 * @date 2022/11/20
 */
public interface HhgLdpHhServer extends HgLdpHhServer {
    /**
     * Get the value of α.
     *
     * @return the value of α.
     */
    double getAlpha();
}
