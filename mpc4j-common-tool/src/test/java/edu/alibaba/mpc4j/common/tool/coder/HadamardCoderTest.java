package edu.alibaba.mpc4j.common.tool.coder;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.coder.linear.HadamardCoder;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Hadamard coder test.
 *
 * @author Weiran Liu
 * @date 2021/12/14
 */
@RunWith(Parameterized.class)
public class HadamardCoderTest {
    /**
     * maximal number of datawords used in the test.
     */
    private static final int MAX_CODE_NUM = 1 << Byte.SIZE;

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // k = 1
        configurations.add(new Object[] {"k = 1", new HadamardCoder(1), });
        // k = 2
        configurations.add(new Object[] {"k = 2", new HadamardCoder(2), });
        // k = 3
        configurations.add(new Object[] {"k = 3", new HadamardCoder(3), });
        // k = 4
        configurations.add(new Object[] {"k = 4", new HadamardCoder(4), });
        // k = 7
        configurations.add(new Object[] {"k = 7", new HadamardCoder(7), });
        // k = 8
        configurations.add(new Object[] {"k = 8", new HadamardCoder(8), });
        // k = 10
        configurations.add(new Object[] {"k = 10", new HadamardCoder(10), });

        return configurations;
    }

    /**
     * the Hadamard coder
     */
    private final HadamardCoder coder;
    /**
     * the number of datawords used in the tests.
     */
    private final int datawordNum;

    public HadamardCoderTest(String name, HadamardCoder coder) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.coder = coder;
        datawordNum = Math.min(MAX_CODE_NUM, 1 << coder.getDatawordBitLength());
    }

    @Test
    public void testEncode() {
        byte[][] datawords = IntStream.range(0, datawordNum)
            .mapToObj(dataword -> IntUtils.nonNegIntToFixedByteArray(dataword, coder.getDatawordByteLength()))
            .toArray(byte[][]::new);
        byte[][] codewords = Arrays.stream(datawords)
            .map(coder::encode)
            .toArray(byte[][]::new);
        // verify the codeword length
        Arrays.stream(codewords).forEach(codeword ->
            Assert.assertTrue(BytesUtils.isFixedReduceByteArray(
                codeword, coder.getCodewordByteLength(), coder.getCodewordBitLength()
            )
        ));
        // verify the hamming distance
        for (int i = 0; i < codewords.length; i++) {
            for (int j = i + 1; j < codewords.length; j++) {
                int distance = BytesUtils.hammingDistance(codewords[i], codewords[j]);
                Assert.assertEquals(coder.getMinimalHammingDistance(), distance);
            }
        }
    }

    @Test
    public void testParallel() {
        byte[][] datawords = IntStream.range(0, datawordNum)
            .mapToObj(index -> IntUtils.nonNegIntToFixedByteArray(0, coder.getDatawordByteLength()))
            .toArray(byte[][]::new);
        Set<ByteBuffer> codewordSet = Arrays.stream(datawords)
            .parallel()
            .map(coder::encode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, codewordSet.size());
    }
}
