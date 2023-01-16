package edu.alibaba.mpc4j.dp.service.fo.ue;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Optimized Unary Encoding (OUE) Frequency Oracle LDP server.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
public class OueFoLdpServer extends AbstractFoLdpServer {
    /**
     * the bucket
     */
    private final int[] budget;
    /**
     * p = 1 / 2
     */
    private static final double CONSTANT_P = 1.0 / 2;
    /**
     * q = 1 / (e^ε + 1)
     */
    private final double q;

    public OueFoLdpServer(FoLdpConfig foLdpConfig) {
        super(foLdpConfig);
        budget = new int[d];
        q = 1 / (Math.exp(epsilon) + 1);
    }

    @Override
    public void insert(byte[] itemBytes) {
        BitVector bitVector = BitVectorFactory.create(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, d, itemBytes);
        num++;
        IntStream.range(0, d).forEach(bitIndex -> {
            if (bitVector.get(bitIndex)) {
                budget[bitIndex]++;
            }
        });
    }

    @Override
    public Map<String, Double> estimate() {
        return IntStream.range(0, d)
            .boxed()
            .collect(Collectors.toMap(
                domain::getIndexItem,
                itemIndex -> (budget[itemIndex] - num * q) / (CONSTANT_P - q)
            ));
    }
}
