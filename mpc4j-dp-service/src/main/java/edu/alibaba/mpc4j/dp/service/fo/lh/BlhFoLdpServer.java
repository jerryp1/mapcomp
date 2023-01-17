package edu.alibaba.mpc4j.dp.service.fo.lh;

import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Binary Local Hash (BLH) Frequency Oracle LDP server.
 *
 * @author Weiran Liu
 * @date 2023/1/17
 */
public class BlhFoLdpServer extends AbstractFoLdpServer {
    /**
     * IntHash
     */
    private final IntHash intHash;
    /**
     * the bucket
     */
    private final int[] budget;
    /**
     * p = e^ε / (e^ε + 1)
     */
    private final double p;

    public BlhFoLdpServer(FoLdpConfig config) {
        super(config);
        double expEpsilon = Math.exp(epsilon);
        p = expEpsilon / (expEpsilon + 1);
        intHash = IntHashFactory.fastestInstance();
        budget = new int[d];
    }

    @Override
    public void insert(byte[] itemBytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(itemBytes);
        int seed = byteBuffer.getInt();
        byte byteB = byteBuffer.get();
        assert byteB == 0x00 || byteB == 0x01;
        num++;
        // each reported ⟨H,b⟩ supports all values that are hashed by H to b, which are half of the input values.
        IntStream.range(0, d)
            .forEach(itemIndex -> {
                String item = domain.getIndexItem(itemIndex);
                byte itemB = (byte)(Math.abs(intHash.hash(item.getBytes(FoLdpFactory.DEFAULT_CHARSET), seed)) % 2);
                if (itemB == byteB) {
                    budget[itemIndex]++;
                }
            });
    }

    @Override
    public Map<String, Double> estimate() {
        return IntStream.range(0, d)
            .boxed()
            .collect(Collectors.toMap(
                domain::getIndexItem,
                itemIndex -> (budget[itemIndex] - num * 0.5) / (p - 0.5)
            ));
    }
}
