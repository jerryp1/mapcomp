package edu.alibaba.mpc4j.dp.service.fo.uldp;

import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.config.RrUldpConfig;
import edu.alibaba.mpc4j.dp.service.tool.Domain;

import java.util.Random;

/**
 * Utility-optimized randomized response client.
 *
 * @author Li Peng
 * @date 2023/8/28
 */
public class RrUldpClient extends AbstractFoLdpClient {
    /**
     * Domain of sensitive values.
     */
    private final Domain sensDomain;
    /**
     * Domain size of sensitive values.
     */
    private final int sensD;
    /**
     * c1
     */
    private final double c1;
    /**
     * c2
     */
    private final double c2;
    /**
     * c3
     */
    private final double c3;

    public RrUldpClient(FoLdpConfig config) {
        super(config);
        RrUldpConfig rrUldpConfig = (RrUldpConfig) config;
        sensDomain = new Domain(rrUldpConfig.getSensDomainSet());
        sensD = sensDomain.getD();
        double expEpsilon = Math.exp(epsilon);
        int sensitivitySize = sensDomain.getD();
        c1 = expEpsilon / (expEpsilon + sensitivitySize - 1);
        c2 = 1 / (expEpsilon + sensitivitySize - 1);
        c3 = 1 - sensitivitySize * c2;
    }

    @Override
    public byte[] randomize(String item, Random random) {
        checkItemInDomain(item);
        // the item is sensitive.
        if (sensDomain.contains(item)) {
            double randomSample = random.nextDouble();
            // Randomly sample an integer in [0, sensD)
            int randomIndex = random.nextInt(sensD);
            if (randomSample > c1 - c2) {
                // answer a random item
                return IntUtils.boundedNonNegIntToByteArray(domain.getItemIndex(
                    sensDomain.getIndexItem(randomIndex)), d);
            } else {
                // answer the true item
                return IntUtils.boundedNonNegIntToByteArray(domain.getItemIndex(item), d);
            }
        } else {
            // the item is not sensitive.
            double randomSample = random.nextDouble();
            // Randomly sample an integer in [0, sensD)
            int randomIndex = random.nextInt(sensD);
            if (randomSample > c3) {
                // answer a random item
                return IntUtils.boundedNonNegIntToByteArray(domain.getItemIndex(
                    sensDomain.getIndexItem(randomIndex)), d);
            } else {
                // answer the true item
                return IntUtils.boundedNonNegIntToByteArray(domain.getItemIndex(item), d);
            }
        }
    }
}
