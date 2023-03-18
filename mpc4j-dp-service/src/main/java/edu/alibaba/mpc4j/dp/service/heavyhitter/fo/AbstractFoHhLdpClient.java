package edu.alibaba.mpc4j.dp.service.heavyhitter.fo;

import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpClient;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;

/**
 * abstract Heavy Hitter LDP client.
 *
 * @author Weiran Liu
 * @date 2023/3/18
 */
abstract class AbstractFoHhLdpClient implements HhLdpClient {
    /**
     * the config
     */
    private final HhLdpConfig hhLdpConfig;

    AbstractFoHhLdpClient(HhLdpConfig hhLdpConfig) {
        this.hhLdpConfig = hhLdpConfig;
    }

    @Override
    public HhLdpFactory.HhLdpType getType() {
        return hhLdpConfig.getType();
    }

    @Override
    public int getD() {
        return hhLdpConfig.getD();
    }

    @Override
    public int getK() {
        return hhLdpConfig.getK();
    }

    @Override
    public double getWindowEpsilon() {
        return hhLdpConfig.getWindowEpsilon();
    }

    @Override
    public int getWindowSize() {
        return hhLdpConfig.getWindowSize();
    }
}
