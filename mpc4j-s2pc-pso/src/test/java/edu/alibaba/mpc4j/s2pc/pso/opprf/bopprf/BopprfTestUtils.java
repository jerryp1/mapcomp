package edu.alibaba.mpc4j.s2pc.pso.opprf.bopprf;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.SimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Batched OPPRF test utilities.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
class BopprfTestUtils {
    /**
     * private constructor.
     */
    private BopprfTestUtils() {
        // empty
    }

    static byte[][][] generateSenderInputArrays(int l, int batchNum, int pointNum, SecureRandom secureRandom) {
        int byteL = CommonUtils.getByteLength(l);
        byte[][] keys = CommonUtils.generateRandomKeys(1, secureRandom);
        // use simple hash to place int into batched queries.
        SimpleIntHashBin simpleIntHashBin = new SimpleIntHashBin(EnvType.STANDARD, batchNum, pointNum, keys);
        simpleIntHashBin.insertItems(IntStream.range(0, pointNum).toArray());
        byte[][][] inputArrays = new byte[batchNum][][];
        for (int batchIndex = 0; batchIndex < batchNum; batchIndex++) {
            int batchPointNum = simpleIntHashBin.binSize(batchIndex);
            inputArrays[batchIndex] = new byte[batchPointNum][];
            for (int pointIndex = 0; pointIndex < batchPointNum; pointIndex++) {
                inputArrays[batchIndex][pointIndex] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            }
        }
        return inputArrays;
    }

    static byte[][][] generateSenderTargetArrays(int l, byte[][][] inputArrays, SecureRandom secureRandom) {
        int byteL = CommonUtils.getByteLength(l);
        int batchNum = inputArrays.length;
        byte[][][] targetArrays = new byte[batchNum][][];
        for (int batchIndex = 0; batchIndex < batchNum; batchIndex++) {
            int batchPointNum = inputArrays[batchIndex].length;
            targetArrays[batchIndex] = new byte[batchPointNum][];
            for (int pointIndex = 0; pointIndex < batchPointNum; pointIndex++) {
                targetArrays[batchIndex][pointIndex] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            }
        }
        return targetArrays;
    }

    static byte[][] generateReceiverInputArray(int l, byte[][][] inputArrays, SecureRandom secureRandom) {
        int byteL = CommonUtils.getByteLength(l);
        int batchNum = inputArrays.length;
        byte[][] inputArray = new byte[batchNum][];
        for (int batchIndex = 0; batchIndex < batchNum; batchIndex++) {
            int batchPointNum = inputArrays[batchIndex].length;
            if (batchPointNum > 0) {
                // batch point num is not zero, randomly select a point to be the target
                int pointIndex = secureRandom.nextInt(batchPointNum);
                inputArray[batchIndex] = BytesUtils.clone(inputArrays[batchIndex][pointIndex]);
            } else {
                // batch point num is zero, create a random input
                inputArray[batchIndex] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            }
        }
        return inputArray;
    }
}
