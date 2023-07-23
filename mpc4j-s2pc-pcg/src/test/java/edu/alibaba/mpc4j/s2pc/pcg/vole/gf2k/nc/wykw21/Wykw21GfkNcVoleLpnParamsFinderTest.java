package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.wykw21;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.lpn.LpnParams;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.Gf2kMspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.bcg19.Bcg19RegGf2kMspVoleConfig;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * WYKW21 LPN parameter finder tests.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
@Ignore
@RunWith(Parameterized.class)
public class Wykw21GfkNcVoleLpnParamsFinderTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Wykw21GfkNcVoleLpnParamsFinderTest.class);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // WYKW21(Malicious) + BCG19(Regular-Index)
        Gf2kMspVoleConfig bcg19MaRegGf2kMspVoleConfig = new Bcg19RegGf2kMspVoleConfig
            .Builder(SecurityModel.MALICIOUS)
            .build();
        configurations.add(new Object[] {"WYKW21(Malicious) + BCG19(Regular-Index)", bcg19MaRegGf2kMspVoleConfig,});
        // WYKW21(Semi-honest) + BCG19(Regular-Index)
        Gf2kMspVoleConfig bcg19ShRegGf2kMspVoleConfig = new Bcg19RegGf2kMspVoleConfig
            .Builder(SecurityModel.SEMI_HONEST)
            .build();
        configurations.add(new Object[] {"WYKW21(Semi-honest) + BCG19(Regular-Index)", bcg19ShRegGf2kMspVoleConfig,});

        return configurations;
    }

    /**
     * GF2K-MSP-VOLE config
     */
    private final Gf2kMspVoleConfig config;

    public Wykw21GfkNcVoleLpnParamsFinderTest(String name, Gf2kMspVoleConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.config = config;
    }

    @Test
    public void test2To12() {
        testLpnParamsFinder(1 << 12);
    }

    @Test
    public void test2To13() {
        testLpnParamsFinder(1 << 13);
    }

    @Test
    public void test2To14() {
        testLpnParamsFinder(1 << 14);
    }

    @Test
    public void test2To15() {
        testLpnParamsFinder(1 << 15);
    }

    @Test
    public void test2To16() {
        testLpnParamsFinder(1 << 16);
    }

    @Test
    public void test2To17() {
        testLpnParamsFinder(1 << 17);
    }

    @Test
    public void test2To18() {
        testLpnParamsFinder(1 << 18);
    }

    @Test
    public void test2To19() {
        testLpnParamsFinder(1 << 19);
    }

    @Test
    public void test2To20() {
        testLpnParamsFinder(1 << 20);
    }

    @Test
    public void test2To21() {
        testLpnParamsFinder(1 << 21);
    }

    @Test
    public void test2To22() {
        testLpnParamsFinder(1 << 22);
    }

    @Test
    public void test2To23() {
        testLpnParamsFinder(1 << 23);
    }

    @Test
    public void test2To24() {
        testLpnParamsFinder(1 << 24);
    }

    @Test
    public void test10Million() {
        // YWL20的参数使密钥数量接近于1000万
        testLpnParamsFinder(10000000);
    }

    private void testLpnParamsFinder(int minN) {
        LOGGER.info("-----find LPN Params for n = {}-----", minN);
        LpnParams iterationLpnParams = Wykw21GfkNcVoleLpnParamsFinder.findIterationLpnParams(config, minN);
        LpnParams setupLpnParams = Wykw21GfkNcVoleLpnParamsFinder.findSetupLpnParams(config, iterationLpnParams);
        LOGGER.info("Setup    : {}", setupLpnParams);
        LOGGER.info("Iteration: {}", iterationLpnParams);
    }

    @Test
    public void testIterationOutputSize() {
        LOGGER.info("-----get {} output size-----", config.getPtoType());
        LpnParams wolverineRegLpnParams = LpnParams.uncheckCreate(10805248, 589760, 1319);
        int wolverineRegOutputSize = Wykw21GfkNcVoleLpnParamsFinder.getIterationOutputSize(config, wolverineRegLpnParams);
        LOGGER.info("Wolverine Reg {}: output size = {}", wolverineRegLpnParams, wolverineRegOutputSize);
    }
}
