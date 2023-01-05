package edu.alibaba.mpc4j.dp.stream.heavyhitter.hg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhFactory;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.config.HgLdpHhClientConfig;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.utils.EmptyLdpHhServerContext;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.utils.LdpHhServerContext;

import java.util.Random;

/**
 * Basic Heavy Hitter client with Local Differential Privacy based on HeavyGuardian.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class BasicHgLdpHhClient extends AbstractHgLdpHhClient {
    /**
     * the universal domain size d, i.e., |Ω|.
     */
    private final int d;
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    private final double q;

    public BasicHgLdpHhClient(HgLdpHhClientConfig clientConfig) {
        super(clientConfig);
        double expWindowEpsilon = Math.exp(windowEpsilon);
        d = bucketDomain.getUniversalD();
        p = expWindowEpsilon / (expWindowEpsilon + d - 1);
        q = 1 / (expWindowEpsilon + d - 1);
    }

    @Override
    public LdpHhFactory.LdpHhType getType() {
        return LdpHhFactory.LdpHhType.BASIC_HG;
    }

    @Override
    public String randomize(LdpHhServerContext serverContext, String item, Random random) {
        Preconditions.checkArgument(serverContext instanceof EmptyLdpHhServerContext);
        checkItemInDomain(item);
        // basic HeavyGuardian solution does not consider the current data structure
        double randomSample = random.nextDouble();
        // Randomly sample an integer in [0, d)
        int randomIndex = random.nextInt(d);
        if (randomSample > p - q) {
            // answer a random item
            return bucketDomain.getUniversalIndexItem(randomIndex);
        } else {
            // answer the true item
            return item;
        }
    }
}
