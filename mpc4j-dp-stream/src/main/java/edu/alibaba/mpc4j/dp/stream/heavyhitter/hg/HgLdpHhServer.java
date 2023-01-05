package edu.alibaba.mpc4j.dp.stream.heavyhitter.hg;

import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhServer;

/**
 * Heavy Hitter server with Local Differential Privacy based on HeavyGuardian.
 *
 * @author Weiran Liu
 * @date 2022/11/20
 */
public interface HgLdpHhServer extends LdpHhServer {
    /**
     * Return the bucket num w.
     *
     * @return the bucket num w.
     */
    int getW();

    /**
     * Return the cell num λ_h in the heavy part.
     *
     * @return the cell num λ_h in the heavy part.
     */
    int getLambdaH();
}
