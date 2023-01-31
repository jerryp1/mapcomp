package edu.alibaba.mpc4j.dp.service.fo.hadamard;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory.BitVectorType;
import edu.alibaba.mpc4j.common.tool.coder.linear.HadamardCoder;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Hadamard Mechanism (HM) Frequency Oracle LDP server. This is the optimized Hadamard Mechanism. See paper:
 * <p>
 * Cormode, Graham, Samuel Maddock, and Carsten Maple. "Frequency estimation under local differential privacy.
 * VLDB 2021, no. 11, pp. 2046-2058.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/1/30
 */
public class HmHighEpsFoLdpServer extends AbstractFoLdpServer {
    /**
     * the Hadamard matrix size, the smallest exponent of 2 that is bigger than d
     */
    private final int n;
    /**
     * 2^t - 1 = e^ε, so that t = log_2(e^ε + 1).
     */
    private final int t;
    /**
     * p = e^ε / (e^ε + 2^t - 1)
     */
    private final double p;
    /**
     * the budgets
     */
    private final int[] budgets;

    public HmHighEpsFoLdpServer(FoLdpConfig config) {
        super(config);
        // the smallest exponent of 2 which is bigger than d
        int k = LongUtils.ceilLog2(d + 1);
        n = 1 << k;
        double expEpsilon = Math.exp(epsilon);
        // the optimal t = log_2(e^ε + 1)
        t = (int)Math.ceil(DoubleUtils.log2(expEpsilon) + 1);
        assert t >= 1 : "t must be greater than or equal to 1: " + t;
        // p = e^ε / (e^ε + 2^t - 1)
        p = expEpsilon / (expEpsilon + (1 << t) - 1);
        budgets = new int[n];
    }

    @Override
    public void insert(byte[] itemBytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(itemBytes);
        byte[] jBytes = new byte[IntUtils.boundedNonNegIntByteLength(n)];
        int[] jArray = new int[t];
        for (int i = 0; i < t; i++) {
            byteBuffer.get(jBytes);
            jArray[i] = IntUtils.byteArrayToBoundedNonNegInt(jBytes, n);
        }
        byte[] coefficientBytes = new byte[CommonUtils.getByteLength(t)];
        byteBuffer.get(coefficientBytes);
        BitVector coefficients = BitVectorFactory.create(BitVectorType.BYTES_BIT_VECTOR, t, coefficientBytes);
        for (int i = 0; i < t; i++) {
            int hadamardCoefficient = coefficients.get(i) ? 1 : -1;
            budgets[jArray[i]] += hadamardCoefficient;
        }

    }

    @Override
    public Map<String, Double> estimate() {
        int[] cs = HadamardCoder.fastWalshHadamardTrans(budgets);
        return IntStream.range(0, d)
            .boxed()
            .collect(Collectors.toMap(
                domain::getIndexItem,
                itemIndex -> {
                    // map to x
                    int x = itemIndex + 1;
                    // map to C(x)
                    int cx = cs[x];
                    // p(x) = C(x) / (2p - 1) / t
                    return cx / (2 * p - 1) / t;
                }
            ));
    }
}
