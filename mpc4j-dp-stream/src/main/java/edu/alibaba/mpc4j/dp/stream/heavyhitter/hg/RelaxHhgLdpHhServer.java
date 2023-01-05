package edu.alibaba.mpc4j.dp.stream.heavyhitter.hg;

import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhFactory;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.config.HhgLdpHhServerConfig;

/**
 * Relaxed Heavy Hitter server with Local Differential Privacy based on Hot HeavyGuardian.
 *
 * @author Weiran Liu
 * @date 2022/11/20
 */
public class RelaxHhgLdpHhServer extends AdvHhgLdpHhServer {

    public RelaxHhgLdpHhServer(HhgLdpHhServerConfig serverConfig) {
        super(serverConfig);
        // recompute p2 and q2
        double expWindowEpsilon = Math.exp(windowEpsilon);
        p2 = expWindowEpsilon / (expWindowEpsilon + lambdaH - 1);
        q2 = 1 / (expWindowEpsilon + lambdaH - 1);
    }

    @Override
    public LdpHhFactory.LdpHhType getType() {
        return LdpHhFactory.LdpHhType.RELAX_HG;
    }

    @Override
    protected double updateCount(int bucketIndex, double count) {
        return count - currentNums[bucketIndex] * gammaH * p1 * q2;
    }
}
