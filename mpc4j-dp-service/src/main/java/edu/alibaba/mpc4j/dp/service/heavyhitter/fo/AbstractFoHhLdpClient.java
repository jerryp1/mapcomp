package edu.alibaba.mpc4j.dp.service.heavyhitter.fo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.tool.Domain;

/**
 * abstract Heavy Hitter client with Local Differential Privacy based on Frequency Oracle.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public abstract class AbstractFoHhLdpClient implements FoHhLdpClient {
    /**
     * the domain
     */
    protected final Domain domain;
    /**
     * d = |Ω|
     */
    protected final int d;
    /**
     * the number of heavy hitters k
     */
    protected final int k;
    /**
     * the private parameter ε / w
     */
    protected final double windowEpsilon;

    public AbstractFoHhLdpClient(HhLdpConfig hhLdpConfig) {
        domain = new Domain(hhLdpConfig.getDomainSet());
        d = domain.getD();
        k = hhLdpConfig.getK();
        windowEpsilon = hhLdpConfig.getWindowEpsilon();
    }

    protected void checkItemInDomain(String item) {
        Preconditions.checkArgument(domain.contains(item), "%s is not in the domain", item);
    }

    @Override
    public int getD() {
        return d;
    }

    @Override
    public int getK() {
        return k;
    }

    @Override
    public double getWindowEpsilon() {
        return windowEpsilon;
    }
}
