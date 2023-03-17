package edu.alibaba.mpc4j.dp.service.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

/**
 * test for the LDP Heavy Hitter main.
 *
 * @author Weiran Liu
 * @date 2023/2/11
 */
public class HhLdpMainTest {

    @Test
    public void testDefault() throws IOException {
        HhLdpMain hhLdpMain = createHhLdpMain("test_config/hh_ldp_test_config_default.conf");
        hhLdpMain.run();
    }

    @Test
    public void testNoDomain() throws IOException {
        HhLdpMain hhLdpMain = createHhLdpMain("test_config/hh_ldp_test_config_no_domain.conf");
        hhLdpMain.run();
    }

    @Test
    public void testNoFoTypes() throws IOException {
        HhLdpMain hhLdpMain = createHhLdpMain("test_config/hh_ldp_test_config_no_fo_types.conf");
        Assert.assertEquals(0, hhLdpMain.getFoLdpList().size());
        hhLdpMain.run();
    }

    @Test
    public void testEmptyFoTypes() throws IOException {
        HhLdpMain hhLdpMain = createHhLdpMain("test_config/hh_ldp_test_config_empty_fo_types.conf");
        Assert.assertEquals(0, hhLdpMain.getFoLdpList().size());
        hhLdpMain.run();
    }

    @Test
    public void testNoHgTypes() throws IOException {
        HhLdpMain hhLdpMain = createHhLdpMain("test_config/hh_ldp_test_config_no_hg_types.conf");
        Assert.assertEquals(0, hhLdpMain.getHgTypeList().size());
        hhLdpMain.run();
    }

    @Test
    public void testEmptyHgTypes() throws IOException {
        HhLdpMain hhLdpMain = createHhLdpMain("test_config/hh_ldp_test_config_empty_hg_types.conf");
        Assert.assertEquals(0, hhLdpMain.getHgTypeList().size());
        hhLdpMain.run();
    }

    @Test
    public void testNoReportPostfix() throws IOException {
        HhLdpMain hhLdpMain = createHhLdpMain("test_config/hh_ldp_test_config_no_report_postfix.conf");
        Assert.assertEquals("", hhLdpMain.getReportFilePostfix());
        hhLdpMain.run();
    }

    @Test
    public void testEmptyReportPostfix() throws IOException {
        HhLdpMain hhLdpMain = createHhLdpMain("test_config/hh_ldp_test_config_empty_report_postfix.conf");
        Assert.assertEquals("", hhLdpMain.getReportFilePostfix());
        hhLdpMain.run();
    }

    @Test
    public void testNoAlpha() throws IOException {
        HhLdpMain hhLdpMain = createHhLdpMain("test_config/hh_ldp_test_config_no_alpha.conf");
        Assert.assertEquals(0, hhLdpMain.getAlphas().length);
        hhLdpMain.run();
    }

    @Test
    public void testEmptyAlpha() throws IOException {
        HhLdpMain hhLdpMain = createHhLdpMain("test_config/hh_ldp_test_config_empty_alpha.conf");
        Assert.assertEquals(0, hhLdpMain.getAlphas().length);
        hhLdpMain.run();
    }

    @Test
    public void testNoPlain() throws IOException {
        HhLdpMain hhLdpMain = createHhLdpMain("test_config/hh_ldp_test_config_no_plain.conf");
        Assert.assertFalse(hhLdpMain.getPlain());
        hhLdpMain.run();
    }

    @Test
    public void testEmptyPlain() throws IOException {
        HhLdpMain hhLdpMain = createHhLdpMain("test_config/hh_ldp_test_config_empty_plain.conf");
        Assert.assertFalse(hhLdpMain.getPlain());
        hhLdpMain.run();
    }

    @Test
    public void testNoWarmup() throws IOException {
        HhLdpMain hhLdpMain = createHhLdpMain("test_config/hh_ldp_test_config_no_warmup.conf");
        Assert.assertEquals(0, hhLdpMain.getWarmupNum());
        hhLdpMain.run();
    }

    @Test
    public void testGammaH() throws IOException {
        HhLdpMain hhLdpMain = createHhLdpMain("test_config/hh_ldp_test_config_gammah.conf");
        Assert.assertTrue(hhLdpMain.getWarmupNum() > 0);
        Assert.assertTrue(hhLdpMain.getGammaHs().length > 0);
        hhLdpMain.run();
    }

    @Test
    public void testGammaNoWarmup() throws IOException {
        HhLdpMain hhLdpMain = createHhLdpMain("test_config/hh_ldp_test_config_gammah_no_warmup.conf");
        Assert.assertEquals(0, hhLdpMain.getWarmupNum());
        Assert.assertTrue(hhLdpMain.getGammaHs().length > 0);
        hhLdpMain.run();
    }

    private HhLdpMain createHhLdpMain(String path) throws IOException {
        String configPath = Objects.requireNonNull(
            HhLdpMainTest.class.getClassLoader().getResource(path)
        ).getPath();
        Properties properties = PropertiesUtils.loadProperties(configPath);
        return new HhLdpMain(properties);
    }
}
