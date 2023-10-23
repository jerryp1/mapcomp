package edu.alibaba.mpc4j.dp.service.fo.uldp;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.config.RrUldpConfig;
import edu.alibaba.mpc4j.dp.service.tool.Domain;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility-optimized randomized response server.
 *
 * @author Li Peng
 * @date 2023/8/28
 */
public class RrUldpServer extends AbstractFoLdpServer {
    /**
     * d byte length
     */
    private final int dByteLength;
    /**
     * the bucket
     */
    private final int[] buckets;
    /**
     * p1 = (sensD + e^epsilon - 1) / (e^epsilon -1)
     */
    private final double p1;
    /**
     * p2 = 1 / (e^epsilon - 1)
     */
    private final double p2;
    /**
     * p3 = (sensD + e^epsilon - 1) / (e^epsilon - 1)
     */
    private final double p3;
    private final Domain sensDomain;

    public RrUldpServer(FoLdpConfig config) {
        super(config);
        RrUldpConfig rrUldpConfig = (RrUldpConfig) config;
        dByteLength = IntUtils.boundedNonNegIntByteLength(d);
        buckets = new int[d];
        double expEpsilon = Math.exp(epsilon);
        sensDomain = new Domain(rrUldpConfig.getSensDomainSet());
        int sensD = sensDomain.getD();
        p1 = (sensD + expEpsilon - 1) / (expEpsilon - 1);
        p2 = 1 / (expEpsilon - 1);
        p3 = (expEpsilon - 1) / (sensD + expEpsilon - 1);
    }

    @Override
    public void insert(byte[] itemBytes) {
        MathPreconditions.checkEqual(
            "actual byte length", "expect byte length", itemBytes.length, dByteLength
        );
        int itemIndex = IntUtils.byteArrayToBoundedNonNegInt(itemBytes, d);
        MathPreconditions.checkNonNegativeInRange("item index", itemIndex, d);
        buckets[itemIndex]++;
        num++;
    }

    @Override
    public Map<String, Double> estimate() {
        return IntStream.range(0, d)
            .boxed()
            .collect(Collectors.toMap(
                domain::getIndexItem,
                itemIndex -> {
                    if (sensDomain.contains(domain.getIndexItem(itemIndex))) {
                        return ((double) buckets[itemIndex] / num * p1 - p2) * num;
                    } else {
                        return ((double) buckets[itemIndex] / num * p3) * num;
                    }
                }
            ));
    }
}
