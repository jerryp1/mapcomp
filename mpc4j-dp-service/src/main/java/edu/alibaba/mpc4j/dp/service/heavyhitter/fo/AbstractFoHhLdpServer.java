package edu.alibaba.mpc4j.dp.service.heavyhitter.fo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpServerState;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;

/**
 * abstract Heavy Hitter server with Local Differential Privacy based on Frequency Oracle.
 *
 * @author Weiran Liu
 * @date 2023/1/4
 */
public abstract class AbstractFoHhLdpServer implements FoHhLdpServer {
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
    protected HhLdpServerState hhLdpServerState;

    public AbstractFoHhLdpServer(HhLdpConfig hhLdpConfig) {
        d = hhLdpConfig.getD();
        k = hhLdpConfig.getK();
        windowEpsilon = hhLdpConfig.getWindowEpsilon();
        num = 0;
        hhLdpServerState = HhLdpServerState.WARMUP;
    }

    protected void checkState(HhLdpServerState expect) {
        Preconditions.checkArgument(hhLdpServerState.equals(expect), "The state must be %s: %s", expect, hhLdpServerState);
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
