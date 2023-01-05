package edu.alibaba.mpc4j.dp.stream.heavyhitter.fo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhServerState;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.config.LdpHhServerConfig;

/**
 * abstract Heavy Hitter server with Local Differential Privacy based on Frequency Oracle.
 *
 * @author Weiran Liu
 * @date 2023/1/4
 */
public abstract class AbstractFoLdpHhServer implements FoLdpHhServer {
    /**
     * d = |Ω|
     */
    protected final int d;
    /**
     * the number of heavy hitters k, which is equal to the cell num in the heavy part λ_h
     */
    protected final int k;
    /**
     * the private parameter ε / w
     */
    protected final double windowEpsilon;
    /**
     * the number of inserted items
     */
    protected int num;
    /**
     * the state
     */
    protected LdpHhServerState ldpHhServerState;

    public AbstractFoLdpHhServer(LdpHhServerConfig serverConfig) {
        d = serverConfig.getD();
        k = serverConfig.getK();
        windowEpsilon = serverConfig.getWindowEpsilon();
        num = 0;
        ldpHhServerState = LdpHhServerState.WARMUP;
    }

    protected void checkState(LdpHhServerState expect) {
        Preconditions.checkArgument(ldpHhServerState.equals(expect), "The state must be %s: %s", expect, ldpHhServerState);
    }

    @Override
    public double getWindowEpsilon() {
        return windowEpsilon;
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
    public int getNum() {
        return num;
    }
}
