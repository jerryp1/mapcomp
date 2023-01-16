package edu.alibaba.mpc4j.dp.service.fo.de;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Direct Encoding (DE) Frequency Oracle LDP server. The item is encoded via index.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
public class DeIndexFoLdpServer extends AbstractFoLdpServer {
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

    public DeIndexFoLdpServer(FoLdpConfig foLdpConfig) {
        super(foLdpConfig);
        budget = new int[d];
        double expEpsilon = Math.exp(epsilon);
        p = expEpsilon / (expEpsilon + d - 1);
        q = 1 / (expEpsilon + d - 1);
    }

    @Override
    public void insert(byte[] itemBytes) {
        int itemIndex = IntUtils.byteArrayToBoundedInt(itemBytes, d);
        MathPreconditions.checkNonNegativeInRange("item index", itemIndex, d);
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
