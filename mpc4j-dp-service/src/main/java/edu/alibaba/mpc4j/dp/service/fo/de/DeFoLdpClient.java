package edu.alibaba.mpc4j.dp.service.fo.de;

import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Random;

/**
 * Direct Encoding (DE) Frequency Oracle LDP client. DE is a generation of the Random Response technique.
 * See Section 4.1 of the following paper:
 * <p>
 * Wang, Tianhao, Jeremiah Blocki, Ninghui Li, and Somesh Jha. Locally differentially private protocols for frequency
 * estimation. In 26th USENIX Security Symposium (USENIX Security 17), pp. 729-745. 2017.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/1/14
 */
public class DeFoLdpClient extends AbstractFoLdpClient {
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    private final double q;

    public DeFoLdpClient(FoLdpConfig foLdpConfig) {
        super(foLdpConfig);
        double expEpsilon = Math.exp(epsilon);
        p = expEpsilon / (expEpsilon + d - 1);
        q = 1 / (expEpsilon + d - 1);
    }

    @Override
    public byte[] randomize(String item, Random random) {
        checkItemInDomain(item);
        // naive solution does not consider the current data structure
        double randomSample = random.nextDouble();
        // Randomly sample an integer in [0, d)
        int randomIndex = random.nextInt(d);
        if (randomSample > p - q) {
            // answer a random item
            return IntUtils.boundedIntToByteArray(randomIndex, d);
        } else {
            // answer the true item
            return IntUtils.boundedIntToByteArray(domain.getItemIndex(item), d);
        }
    }
}
