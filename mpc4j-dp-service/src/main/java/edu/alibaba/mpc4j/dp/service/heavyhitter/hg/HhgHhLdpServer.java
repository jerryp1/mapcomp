package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

/**
 * Hot HeavyGuardian-based Heavy Hitter LDP client.
 *
 * @author Weiran Liu
 * @date 2022/11/20
 */
public interface HhgHhLdpServer extends HgHhLdpServer {
    /**
     * Gets the value of α.
     *
     * @return the value of α.
     */
    double getAlpha();

    /**
     * Gets the value of γ_h.
     *
     * @return the value of γ_h.
     */
    double getGammaH();

    /**
     * Gets λ_l, i.e., the buffer num in each bucket.
     *
     * @return λ_l.
     */
    int getLambdaL();
}
