package edu.alibaba.mpc4j.dp.stream.heavyhitter.hg;

import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhClient;

/**
 * Heavy Hitter client with Local Differential Privacy.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public interface HgLdpHhClient extends LdpHhClient {
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
