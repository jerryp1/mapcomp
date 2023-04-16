package edu.alibaba.mpc4j.s2pc.aby.circuit.psm;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * private set membership test utilities.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
class PsmTestUtils {
    /**
     * private constructor
     */
    private PsmTestUtils() {
        // empty
    }

    static byte[][][] genSenderInputArrays(int l, int d, int num, SecureRandom secureRandom) {
        assert LongUtils.ceilLog2(d) <= l
            : "log(d) must be less than or equal to " + l + ", or we cannot generate distinct inputs: " + d;
        int byteL = CommonUtils.getByteLength(l);
        return IntStream.range(0, num)
            .parallel()
            .mapToObj(index -> {
                byte[][] inputArray = new byte[d][byteL];
                boolean success = false;
                while (!success) {
                    for (int i = 0; i < d; i++) {
                        inputArray[i] = BytesUtils.randomByteArray(byteL, l, secureRandom);
                    }
                    long distinctCount = Arrays.stream(inputArray).map(ByteBuffer::wrap).distinct().count();
                    success = (distinctCount == d);
                }
                return inputArray;
            })
            .toArray(byte[][][]::new);
    }

    static byte[][] genReceiverInputArray(int l, int d, byte[][][] inputArrays, SecureRandom secureRandom) {
        assert LongUtils.ceilLog2(d) <= l
            : "log(d) must be less than or equal to " + l + ", or we cannot generate distinct inputs: " + d;
        int byteL = CommonUtils.getByteLength(l);
        int num = inputArrays.length;
        return IntStream.range(0, num)
            .parallel()
            .mapToObj(index -> {
                boolean equal = (index % 2 == 0);
                if (equal) {
                    int randomD = secureRandom.nextInt(d);
                    return BytesUtils.clone(inputArrays[index][randomD]);
                } else {
                    return BytesUtils.randomByteArray(byteL, l, secureRandom);
                }
            })
            .toArray(byte[][]::new);
    }
}
