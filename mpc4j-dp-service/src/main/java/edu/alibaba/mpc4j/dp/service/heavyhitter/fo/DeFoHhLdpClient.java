package edu.alibaba.mpc4j.dp.service.heavyhitter.fo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.EmptyHhLdpServerContext;

import java.util.Random;

/**
 * Direct Encoding Heavy Hitter client with Local Differential Privacy.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class DeFoHhLdpClient extends AbstractFoHhLdpClient {
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    private final double q;

    public DeFoHhLdpClient(HhLdpConfig hhLdpConfig) {
        super(hhLdpConfig);
        double expEpsilon = Math.exp(windowEpsilon);
        p = expEpsilon / (expEpsilon + d - 1);
        q = 1 / (expEpsilon + d - 1);
    }

    @Override
    public HhLdpFactory.HhLdpType getType() {
        return HhLdpFactory.HhLdpType.DE_FO;
    }

    @Override
    public byte[] warmup(String item) {
        checkItemInDomain(item);
        return item.getBytes(HhLdpFactory.DEFAULT_CHARSET);
    }

    @Override
    public byte[] randomize(HhLdpServerContext serverContext, String item, Random random) {
        Preconditions.checkArgument(serverContext instanceof EmptyHhLdpServerContext);
        checkItemInDomain(item);
        // naive solution does not consider the current data structure
        double randomSample = random.nextDouble();
        // Randomly sample an integer in [0, d)
        int randomIndex = random.nextInt(d);
        if (randomSample > p - q) {
            // answer a random item
            return domain.getIndexItem(randomIndex).getBytes(HhLdpFactory.DEFAULT_CHARSET);
        } else {
            // answer the true item
            return item.getBytes(HhLdpFactory.DEFAULT_CHARSET);
        }
    }
}
