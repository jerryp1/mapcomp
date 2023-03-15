package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core;

import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19.Rss19Zp64CoreMtgConfig;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * RSS19-Zp64 multiplication triple test.
 *
 * @author Weiran Liu
 * @date 2023/2/15
 */
@Ignore
public class Rss19Zp64MtgTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Rss19Zp64MtgTest.class);
    /**
     * polynomial modulus degrees
     */
    private static final int[] POLY_MODULUS_DEGREES = new int[] {
        2048, 4096, 8192
    };

    @Test
    public void testValidBitLength() {
        for (int polyModulusDegree : POLY_MODULUS_DEGREES) {
            List<Long> validBitLengthList = new LinkedList<>();
            for (int l = 1; l < LongUtils.MAX_L; l++) {
                try {
                    Rss19Zp64CoreMtgConfig config = new Rss19Zp64CoreMtgConfig.Builder(l).build();
                    Zp64 zp64 = config.getZp64();
                    long primeBitLength = zp64.getElementBitLength();
                    validBitLengthList.add(primeBitLength);
                } catch (Exception ignored) {

                }
            }
            LOGGER.info("modulus degree = {}, l = {}", polyModulusDegree, Arrays.toString(validBitLengthList.toArray()));
        }
    }
}
