package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core;

import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19.Rss19Zp64CoreMtgConfig;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RSS19-核布尔三元组生成协议配置项测试。
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
@Ignore
public class Rss19Zp64CoreMtgConfigTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Rss19Zp64CoreMtgConfigTest.class);

    @Test
    public void testConfigSetPrimeBitLength() {
        for (int l = 1; l < LongUtils.MAX_L; l++) {
            try {
                Rss19Zp64CoreMtgConfig config = new Rss19Zp64CoreMtgConfig.Builder(l)
                    .build();
                Zp64 zp64 = config.getZp64();
                LOGGER.info("config build success for l = {}, prime = {}", l, zp64.getPrime());
            } catch (Exception e) {
                LOGGER.info("config build  failed for l = {}", l);
            }
        }
    }
}
