package edu.alibaba.mpc4j.common.tool.benes;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkFactory.BenesNetworkType;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 贝奈斯网络测试类。测试实例来自：
 * Chang C, Melhem R. Arbitrary size benes networks[J]. Parallel Processing Letters, 1997, 7(03): 279-284.
 *
 * @author Weiran Liu
 * @date 2021/12/25
 */
@RunWith(Parameterized.class)
public class BenesNetworkTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BenesNetworkTest.class);
    /**
     * 随机测试次数
     */
    private static final int MAX_RANDOM_ROUND = 40;
    /**
     * 性能测试置换表大小
     */
    private static final int EFFICIENCY_PERMUTATION_NUM = 50000;
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0000.00");
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // JDK_BENES_NETWORK
        configurationParams.add(new Object[] {
            BenesNetworkType.JDK_BENES_NETWORK.name(), BenesNetworkType.JDK_BENES_NETWORK,
        });
        // NATIVE_BENES_NETWORK
        configurationParams.add(new Object[] {
            BenesNetworkType.NATIVE_BENES_NETWORK.name(), BenesNetworkType.NATIVE_BENES_NETWORK,
        });

        return configurationParams;
    }

    /**
     * 待测试的椭圆曲线类型
     */
    private final BenesNetworkType benesNetworkType;

    public BenesNetworkTest(String name, BenesNetworkType benesNetworkType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.benesNetworkType = benesNetworkType;
    }

    @Test
    public void testType() {
        int[] permutationMap = new int[] {7, 4, 8, 6, 2, 1, 0, 3, 5};
        BenesNetwork<Integer> benesNetwork = BenesNetworkFactory.createInstance(benesNetworkType, permutationMap);
        Assert.assertEquals(benesNetworkType, benesNetwork.getBenesNetworkType());
    }

    @Test
    public void testIntegerExample() {
        int[] permutationMap = new int[] {7, 4, 8, 6, 2, 1, 0, 3, 5};
        BenesNetwork<Integer> benesNetwork = BenesNetworkFactory.createInstance(benesNetworkType, permutationMap);
        assertIntegerBenesNetwork(permutationMap, benesNetwork);
    }

    @Test
    public void testByteBufferExample() {
        int[] permutationMap = new int[] {7, 4, 8, 6, 2, 1, 0, 3, 5};
        BenesNetwork<ByteBuffer> benesNetwork = BenesNetworkFactory.createInstance(benesNetworkType, permutationMap);
        assertByteBufferBenesNetwork(permutationMap, benesNetwork);
    }

    @Test
    public void testRandom() {
        // n = 2
        testIntegerRandom(2);
        // n = 3
        testIntegerRandom(3);
        // n = 2^k
        testIntegerRandom(1 << 10);
        // n != 2^k
        testIntegerRandom((1 << 10) - 1);
        testIntegerRandom((1 << 10) + 1);
    }

    private void testIntegerRandom(int n) {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            List<Integer> shufflePermutationMap = IntStream.range(0, n).boxed().collect(Collectors.toList());
            Collections.shuffle(shufflePermutationMap, SECURE_RANDOM);
            int[] permutationMap = shufflePermutationMap.stream().mapToInt(permutation -> permutation).toArray();
            BenesNetwork<Integer> benesNetwork = BenesNetworkFactory.createInstance(benesNetworkType, permutationMap);
            assertIntegerBenesNetwork(permutationMap, benesNetwork);
        }
    }

    @Test
    public void testByteBufferRandom() {
        // n = 2
        testByteBufferRandom(2);
        // n = 3
        testByteBufferRandom(3);
        // n = 2^k
        testByteBufferRandom(1 << 10);
        // n != 2^k
        testByteBufferRandom((1 << 10) - 1);
        testByteBufferRandom((1 << 10) + 1);
    }

    private void testByteBufferRandom(int n) {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            List<Integer> shufflePermutationMap = IntStream.range(0, n).boxed().collect(Collectors.toList());
            Collections.shuffle(shufflePermutationMap, SECURE_RANDOM);
            int[] permutationMap = shufflePermutationMap.stream().mapToInt(permutation -> permutation).toArray();
            BenesNetwork<ByteBuffer> benesNetwork = BenesNetworkFactory.createInstance(benesNetworkType, permutationMap);
            assertByteBufferBenesNetwork(permutationMap, benesNetwork);
        }
    }

    private void assertIntegerBenesNetwork(int[] permutationMap, BenesNetwork<Integer> benesNetwork) {
        int n = permutationMap.length;
        // 验证网络层数和宽度
        Assert.assertEquals(BenesNetworkUtils.getLevel(n), benesNetwork.getLevel());
        Assert.assertEquals(BenesNetworkUtils.getWidth(n), benesNetwork.getWidth());
        // 验证固定整数输入的置换输出
        Vector<Integer> inputVector = IntStream.range(0, n)
            .boxed().
            collect(Collectors.toCollection(Vector::new));
        Vector<Integer> expectOutputVector = BenesNetworkUtils.permutation(permutationMap, inputVector);
        Vector<Integer> actualOutputVector = benesNetwork.permutation(inputVector);
        Assert.assertEquals(expectOutputVector, actualOutputVector);
        // 验证随机整数输入的置换输出
        Vector<Integer> randomInputVector = IntStream.range(0, n)
            .map(position -> SECURE_RANDOM.nextInt(n))
            .boxed()
            .collect(Collectors.toCollection(Vector::new));
        Vector<Integer> expectRandomOutputVector = BenesNetworkUtils.permutation(permutationMap, randomInputVector);
        Vector<Integer> actualRandomOutputVector = benesNetwork.permutation(randomInputVector);
        Assert.assertEquals(expectRandomOutputVector, actualRandomOutputVector);
    }

    private void assertByteBufferBenesNetwork(int[] permutationMap, BenesNetwork<ByteBuffer> benesNetwork) {
        int n = permutationMap.length;
        // 验证网络层数和宽度
        Assert.assertEquals(BenesNetworkUtils.getLevel(n), benesNetwork.getLevel());
        Assert.assertEquals(BenesNetworkUtils.getWidth(n), benesNetwork.getWidth());
        // 验证固定整数输入的置换输出
        Vector<ByteBuffer> sequentialInputVector = IntStream.range(0, n)
            .mapToObj(IntUtils::intToByteArray)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(Vector::new));
        Vector<ByteBuffer> expectOutputVector = BenesNetworkUtils.permutation(permutationMap, sequentialInputVector);
        Vector<ByteBuffer> actualOutputVector = benesNetwork.permutation(sequentialInputVector);
        Assert.assertEquals(expectOutputVector, actualOutputVector);
        // 验证随机字节数组输入的置换输出
        Vector<ByteBuffer> randomInputVector = IntStream.range(0, n)
            .mapToObj(position -> {
                byte[] input = new byte[CommonConstants.STATS_BYTE_LENGTH];
                SECURE_RANDOM.nextBytes(input);
                return ByteBuffer.wrap(input);
            })
            .collect(Collectors.toCollection(Vector::new));
        Vector<ByteBuffer> expectRandomOutputVector = BenesNetworkUtils.permutation(permutationMap, randomInputVector);
        Vector<ByteBuffer> actualRandomOutputVector = benesNetwork.permutation(randomInputVector);
        Assert.assertEquals(expectRandomOutputVector, actualRandomOutputVector);
    }

    @Test
    public void testEfficiency() {
        LOGGER.info("-----测试{}性能，元素数量 = {}-----", benesNetworkType.name(), EFFICIENCY_PERMUTATION_NUM);
        List<Integer> shufflePermutationMap = IntStream.range(0, EFFICIENCY_PERMUTATION_NUM)
            .boxed()
            .collect(Collectors.toList());
        Collections.shuffle(shufflePermutationMap, SECURE_RANDOM);
        int[] permutationMap = shufflePermutationMap.stream()
            .mapToInt(permutation -> permutation)
            .toArray();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        BenesNetwork<Integer> benesNetwork = BenesNetworkFactory.createInstance(benesNetworkType, permutationMap);
        stopWatch.stop();
        double createTime = (double)stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();

        Vector<Integer> inputVector = IntStream.range(0, EFFICIENCY_PERMUTATION_NUM)
            .boxed().
            collect(Collectors.toCollection(Vector::new));
        stopWatch.start();
        benesNetwork.permutation(inputVector);
        stopWatch.stop();
        double permuteTime = (double)stopWatch.getTime(TimeUnit.MILLISECONDS);
        LOGGER.info("{}\t{}", "create(ms)", "permute(ms)");
        LOGGER.info("{}\t{}", TIME_DECIMAL_FORMAT.format(createTime), TIME_DECIMAL_FORMAT.format(permuteTime));
    }
}
