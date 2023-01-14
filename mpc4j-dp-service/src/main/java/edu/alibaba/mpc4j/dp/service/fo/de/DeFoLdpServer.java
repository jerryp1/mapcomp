package edu.alibaba.mpc4j.dp.service.fo.de;

import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Direct Encoding (DE) Frequency Oracle LDP server. DE is a generation of the Random Response technique.
 * See Section 4.1 of the following paper:
 * <p>
 * Wang, Tianhao, Jeremiah Blocki, Ninghui Li, and Somesh Jha. Locally differentially private protocols for frequency
 * estimation. In 26th USENIX Security Symposium (USENIX Security 17), pp. 729-745. 2017.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/1/10
 */
public class DeFoLdpServer extends AbstractFoLdpServer {
    /**
     * the bucket
     */
    private final int[] budget;
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    private final double q;

    public DeFoLdpServer(FoLdpConfig foLdpConfig) {
        super(foLdpConfig);
        budget = new int[d];
        double expEpsilon = Math.exp(epsilon);
        p = expEpsilon / (expEpsilon + d - 1);
        q = 1 / (expEpsilon + d - 1);
    }

    @Override
    public void aggregate(byte[] data) {
        int itemIndex = IntUtils.byteArrayToBoundedInt(data, d);
        num++;
        budget[itemIndex]++;
    }

    @Override
    public Map<String, Double> estimate() {
        return IntStream.range(0, d)
            .boxed()
            .collect(Collectors.toMap(
                domain::getIndexItem,
                itemIndex -> (budget[itemIndex] - num * q) / (p - q)
            ));
    }
}
