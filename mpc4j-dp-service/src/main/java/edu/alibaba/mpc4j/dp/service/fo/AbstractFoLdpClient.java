package edu.alibaba.mpc4j.dp.service.fo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.tool.Domain;

/**
 * Abstract Frequency Oracle LDP client.
 *
 * @author Weiran Liu
 * @date 2023/1/14
 */
public abstract class AbstractFoLdpClient implements FoLdpClient {
    /**
     * the type
     */
    private final FoLdpFactory.FoLdpType type;
    /**
     * the domain
     */
    protected final Domain domain;
    /**
     * d = |Ω|
     */
    protected final int d;
    /**
     * the private parameter ε
     */
    protected final double epsilon;

    public AbstractFoLdpClient(FoLdpConfig foLdpConfig) {
        type = foLdpConfig.getType();
        domain = foLdpConfig.getDomain();
        d = domain.getD();
        epsilon = foLdpConfig.getEpsilon();
    }

    protected void checkItemInDomain(String item) {
        Preconditions.checkArgument(domain.contains(item), "%s is not in the domain", item);
    }

    @Override
    public FoLdpFactory.FoLdpType getType() {
        return type;
    }

    @Override
    public int getD() {
        return d;
    }

    @Override
    public double getEpsilon() {
        return epsilon;
    }
}
