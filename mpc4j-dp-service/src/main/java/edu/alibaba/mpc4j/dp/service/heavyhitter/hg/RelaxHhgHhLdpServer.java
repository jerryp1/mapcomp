package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhgHhLdpConfig;

/**
 * Relaxed Hot HeavyGuardian-based Heavy Hitter LDP server.
 *
 * @author Weiran Liu
 * @date 2022/11/20
 */
public class RelaxHhgHhLdpServer extends AdvHhgHhLdpServer {

    public RelaxHhgHhLdpServer(HhgHhLdpConfig hhgHhLdpConfig) {
        super(hhgHhLdpConfig);
        // recompute p2 and q2
        double expWindowEpsilon = Math.exp(windowEpsilon);
        p2 = expWindowEpsilon / (expWindowEpsilon + lambdaH - 1);
        q2 = 1 / (expWindowEpsilon + lambdaH - 1);
    }

    @Override
    public HhLdpFactory.HhLdpType getType() {
        return HhLdpFactory.HhLdpType.RELAX_HG;
    }

    @Override
    protected double updateCount(int bucketIndex, double count) {
        return count - currentNums[bucketIndex] * gammaH * p1 * q2;
    }
}
