package edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * GF(2^e)-DOKVS efficient tests.
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@Ignore
public class Gf2eDokvsEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2eDokvsEfficiencyTest.class);
    /**
     * time format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * types
     */
    private static final Gf2eDokvsType[] TYPES = Gf2eDokvsFactory.Gf2eDokvsType.values();
    /**
     * default l
     */
    private static final int DEFAULT_L = 128;

    @Test
    public void testEfficiency() {
        LOGGER.info(
            "{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
            "                          name", "      logN", "         m", "        lm", "        rm", "  parallel",
            " encode(s)", " decode(s)", "dEncode(s)", "dDecode(s)"
        );
        testEfficiency(8);
        testEfficiency(10);
        testEfficiency(12);
        testEfficiency(14);
        testEfficiency(16);
        testEfficiency(18);
    }

    private void testEfficiency(int logN) {
        testEfficiency(logN, false);
        testEfficiency(logN, true);
    }

    private void testEfficiency(int logN, boolean parallelEncode) {
        int n = 1 << logN;
        int l = DEFAULT_L;
        for (Gf2eDokvsType type : TYPES) {
            int hashNum = Gf2eDokvsFactory.getHashKeyNum(type);
            byte[][] keys = CommonUtils.generateRandomKeys(hashNum, SECURE_RANDOM);
            Gf2eDokvs<ByteBuffer> dokvs = Gf2eDokvsFactory.createInstance(EnvType.STANDARD, type, n, l, keys);
            dokvs.setParallelEncode(parallelEncode);
            Map<ByteBuffer, byte[]> keyValueMap = Gf2eDokvsTest.randomKeyValueMap(n, l);
            STOP_WATCH.start();
            byte[][] nonDoublyStorage = dokvs.encode(keyValueMap, false);
            STOP_WATCH.stop();
            double nonDoublyEncodeTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            Stream<ByteBuffer> nonDoublyKeyStream = keyValueMap.keySet().stream();
            nonDoublyKeyStream = parallelEncode ? nonDoublyKeyStream.parallel() : nonDoublyKeyStream;
            STOP_WATCH.start();
            Map<ByteBuffer, byte[]> nonDoublyDecodeKeyValueMap = nonDoublyKeyStream
                .collect(Collectors.toMap(key -> key, key -> dokvs.decode(nonDoublyStorage, key)));
            STOP_WATCH.stop();
            double nonDoublyDecodeTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            keyValueMap.keySet()
                .forEach(key -> Assert.assertArrayEquals(keyValueMap.get(key), nonDoublyDecodeKeyValueMap.get(key)));
            STOP_WATCH.reset();
            STOP_WATCH.start();
            byte[][] doublyStorage = dokvs.encode(keyValueMap, true);
            STOP_WATCH.stop();
            double doublyEncodeTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            Stream<ByteBuffer> doublyKeyStream = keyValueMap.keySet().stream();
            doublyKeyStream = parallelEncode ? doublyKeyStream.parallel() : doublyKeyStream;
            STOP_WATCH.start();
            Map<ByteBuffer, byte[]> doublyDecodeKeyValueMap = doublyKeyStream
                .collect(Collectors.toMap(key -> key, key -> dokvs.decode(doublyStorage, key)));
            STOP_WATCH.stop();
            double doublyDecodeTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            keyValueMap.keySet()
                .forEach(key -> Assert.assertArrayEquals(keyValueMap.get(key), doublyDecodeKeyValueMap.get(key)));
            STOP_WATCH.reset();
            String lm;
            String rm;
            if (dokvs instanceof SparseGf2eDokvs) {
                SparseGf2eDokvs<ByteBuffer> sparseDokvs = (SparseGf2eDokvs<ByteBuffer>) dokvs;
                int m = sparseDokvs.getM();
                lm = String.valueOf(sparseDokvs.sparsePositionRange());
                rm = String.valueOf(m - sparseDokvs.sparsePositionRange());
            } else {
                lm = "-";
                rm = "-";
            }
            LOGGER.info(
                "{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 30),
                StringUtils.leftPad(String.valueOf(logN), 10),
                StringUtils.leftPad(String.valueOf(dokvs.getM()), 10),
                StringUtils.leftPad(lm, 10),
                StringUtils.leftPad(rm, 10),
                StringUtils.leftPad(String.valueOf(parallelEncode), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(nonDoublyEncodeTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(nonDoublyDecodeTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(doublyEncodeTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(doublyDecodeTime), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
