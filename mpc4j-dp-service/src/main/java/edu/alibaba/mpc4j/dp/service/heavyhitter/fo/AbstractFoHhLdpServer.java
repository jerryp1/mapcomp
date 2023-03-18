package edu.alibaba.mpc4j.dp.service.heavyhitter.fo;

import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpServer;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;

/**
 * abstract Heavy Hitter LDP server.
 *
 * @author Weiran Liu
 * @date 2023/3/18
 */
abstract class AbstractFoHhLdpServer implements HhLdpServer {
    /**
     * the config
     */
    private final HhLdpConfig hhLdpConfig;

    AbstractFoHhLdpServer(HhLdpConfig hhLdpConfig) {
        this.hhLdpConfig = hhLdpConfig;
    }

    @Override
    public HhLdpFactory.HhLdpType getType() {
        return hhLdpConfig.getType();
    }

    @Override
    public double getWindowEpsilon() {
        return hhLdpConfig.getWindowEpsilon();
    }

    @Override
    public int getWindowSize() {
        return hhLdpConfig.getWindowSize();
    }

    @Override
    public int getD() {
        return hhLdpConfig.getD();
    }

    @Override
    public int getK() {
        return hhLdpConfig.getK();
    }
}
