package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.regression.RegressionResults;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Cuckoo hash bin parameters test. Here we text statistical security parameters for small bin num, and use linear
 * regression to estimate bin num for the desired security parameter σ = 40.
 *
 * @author Weiran Liu
 * @date 2023/7/27
 */
@Ignore
@RunWith(Parameterized.class)
public class CuckooHashBinParamsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CuckooHashBinParamsTest.class);
    /**
     * max round
     */
    private static final int MAX_ROUND = 1 << 22;
    /**
     * target σ = 40
     */
    private static final int TARGET_SIGMA = CommonConstants.STATS_BIT_LENGTH;
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * regression point num
     */
    private static final int REGRESSION_POINT_NUM = 5;
    /**
     * ε
     */
    private static final double EPSILON = 1.3;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // NO_STASH_NAIVE
        configurations.add(new Object[]{
            CuckooHashBinType.NO_STASH_NAIVE.name(), CuckooHashBinType.NO_STASH_NAIVE
        });
        // NO_STASH_PSZ18_3_HASH
        configurations.add(new Object[]{
            CuckooHashBinType.NO_STASH_PSZ18_3_HASH.name(), CuckooHashBinType.NO_STASH_PSZ18_3_HASH
        });
        // NO_STASH_PSZ18_4_HASH
        configurations.add(new Object[]{
            CuckooHashBinType.NO_STASH_PSZ18_4_HASH.name(), CuckooHashBinType.NO_STASH_PSZ18_4_HASH
        });
        // NO_STASH_PSZ18_5_HASH
        configurations.add(new Object[]{
            CuckooHashBinType.NO_STASH_PSZ18_5_HASH.name(), CuckooHashBinType.NO_STASH_PSZ18_5_HASH
        });

        return configurations;
    }

    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType type;

    public CuckooHashBinParamsTest(String name, CuckooHashBinType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testLogNum1() {
        testLogNum(1);
    }

    @Test
    public void testLogNum2() {
        testLogNum(2);
    }

    @Test
    public void testLogNum3() {
        testLogNum(3);
    }

    @Test
    public void testLogNum4() {
        testLogNum(4);
    }

    @Test
    public void testLogNum5() {
        testLogNum(5);
    }

    @Test
    public void testLogNum6() {
        testLogNum(6);
    }

    @Test
    public void testLogNum7() {
        testLogNum(7);
    }

    private void testLogNum(int logNum) {
        MathPreconditions.checkPositive("logNum", logNum);
        int num = 1 << logNum;
        List<ByteBuffer> items = IntStream.range(0, num)
            .mapToObj(IntUtils::intToByteArray)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
        int minBinNum = num + 2;
        int maxBinNum = (int) Math.ceil(minBinNum * EPSILON);
        if (maxBinNum - minBinNum < REGRESSION_POINT_NUM) {
            maxBinNum = minBinNum + REGRESSION_POINT_NUM;
        }
        int binInterval = (maxBinNum - minBinNum) / REGRESSION_POINT_NUM;
        double[][] points = new double[REGRESSION_POINT_NUM][2];
        int pointIndex = 0;
        for (int binNum = minBinNum; binNum <= maxBinNum && pointIndex < REGRESSION_POINT_NUM; binNum += binInterval) {
            int finalBinNum = binNum;
            // for each bin num, test its security parameter
            int noStashCount = IntStream.range(0, MAX_ROUND).parallel()
                .map(round -> {
                    // try to insert items and see if it is no stash
                    try {
                        byte[][] keys = CommonUtils.generateRandomKeys(CuckooHashBinFactory.getHashNum(type), SECURE_RANDOM);
                        NoStashCuckooHashBin<ByteBuffer> hashBin = CuckooHashBinFactory.createNoStashCuckooHashBin(
                            EnvType.STANDARD, type, num, finalBinNum, keys
                        );
                        hashBin.insertItems(items);
                        return 1;
                    } catch (ArithmeticException e) {
                        return 0;
                    }
                })
                .sum();
            double sigma = -1 * DoubleUtils.log2(1 - (double) noStashCount / MAX_ROUND);
            points[pointIndex][0] = binNum;
            points[pointIndex][1] = sigma;
            pointIndex++;
            LOGGER.info("log(num) = {}, bin = {}, σ = {}", logNum, binNum, sigma);
        }
        // linear regression
        SimpleRegression regression = new SimpleRegression();
        regression.addData(points);
        RegressionResults regressionResults = regression.regress();
        double b = regressionResults.getParameterEstimate(0);
        double k = regressionResults.getParameterEstimate(1);
        // σ = k * bin + b, now we want to get bin
        double estimateBinNum = (TARGET_SIGMA - b) / k;
        LOGGER.info("log(num) = {}, σ = {}, estimate bin = {}, estimate ε = {}",
            logNum, TARGET_SIGMA, estimateBinNum, estimateBinNum / num
        );
    }
}
