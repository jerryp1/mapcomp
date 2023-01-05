package edu.alibaba.mpc4j.dp.stream.heavyhitter.fo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhFactory;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.config.LdpHhClientConfig;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.utils.EmptyLdpHhServerContext;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.utils.LdpHhServerContext;

import java.util.Random;

/**
 * Direct Encoding Heavy Hitter client with Local Differential Privacy.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class DeFoLdpHhClient extends AbstractFoLdpHhClient {
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    private final double q;

    public DeFoLdpHhClient(LdpHhClientConfig clientConfig) {
        super(clientConfig);
        double expEpsilon = Math.exp(windowEpsilon);
        p = expEpsilon / (expEpsilon + d - 1);
        q = 1 / (expEpsilon + d - 1);
    }

    @Override
    public LdpHhFactory.LdpHhType getType() {
        return LdpHhFactory.LdpHhType.DE_FO;
    }

    @Override
    public String randomize(LdpHhServerContext serverContext, String item, Random random) {
        Preconditions.checkArgument(serverContext instanceof EmptyLdpHhServerContext);
        checkItemInDomain(item);
        // naive solution does not consider the current data structure
        double randomSample = random.nextDouble();
        // Randomly sample an integer in [0, d)
        int randomIndex = random.nextInt(d);
        if (randomSample > p - q) {
            // answer a random item
            return domain.getIndexItem(randomIndex);
        } else {
            // answer the true item
            return item;
        }
    }
}
