package edu.alibaba.mpc4j.dp.service.fo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.tool.Domain;

/**
 * Abstract Frequency Oracle LDP server.
 *
 * @author Weiran Liu
 * @date 2023/1/14
 */
public abstract class AbstractFoLdpServer implements FoLdpServer {
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
    /**
     * the number of inserted items
     */
    protected int num;

    public AbstractFoLdpServer(FoLdpConfig foLdpConfig) {
        type = foLdpConfig.getType();
        domain = new Domain(foLdpConfig.getDomainSet());
        d = domain.getD();
        epsilon = foLdpConfig.getEpsilon();
        num = 0;
    }

    protected void checkItemInDomain(String item) {
        Preconditions.checkArgument(domain.contains(item), "%s is not in the domain", item);
    }

    @Override
    public FoLdpFactory.FoLdpType getType() {
        return type;
    }

    @Override
    public double getEpsilon() {
        return epsilon;
    }

    @Override
    public int getD() {
        return d;
    }

    @Override
    public int getNum() {
        return num;
    }
}
