package edu.alibaba.mpc4j.dp.stream.heavyhitter.fo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.HeavyHitterState;

import java.util.ArrayList;
import java.util.Set;

/**
 * abstract Heavy Hitter with Local Differential Privacy based on Frequency Oracle.
 *
 * @author Weiran Liu
 * @date 2023/1/4
 */
public abstract class AbstractFoLdpHeavyHitter implements FoLdpHeavyHitter {
    /**
     * the domain set
     */
    protected Set<String> domainSet;
    /**
     * the domain array list
     */
    protected ArrayList<String> domainArrayList;
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
    protected HeavyHitterState heavyHitterState;

    public AbstractFoLdpHeavyHitter(Set<String> domainSet, int k, double windowEpsilon) {
        d = domainSet.size();
        MathPreconditions.checkGreater("|Ω|", d, 1);
        this.domainSet = domainSet;
        domainArrayList = new ArrayList<>(domainSet);
        MathPreconditions.checkPositiveInRangeClosed("k", k, d);
        this.k = k;
        MathPreconditions.checkPositive("ε / w", windowEpsilon);
        this.windowEpsilon = windowEpsilon;
        num = 0;
        heavyHitterState = HeavyHitterState.WARMUP;
    }

    @Override
    public void cleanDomainSet() {
        Preconditions.checkArgument(
            heavyHitterState.equals(HeavyHitterState.WARMUP) || heavyHitterState.equals(HeavyHitterState.STATISTICS),
            "The heavy hitter must be %s or %s: %s", HeavyHitterState.WARMUP, HeavyHitterState.STATISTICS, heavyHitterState
        );
        domainSet = null;
        domainArrayList = null;
        heavyHitterState = HeavyHitterState.CLEAN;
    }

    @Override
    public double getWindowEpsilon() {
        return windowEpsilon;
    }

    @Override
    public int getK() {
        return k;
    }

    @Override
    public int getNum() {
        return num;
    }

    @Override
    public Set<String> getDomainSet() {
        Preconditions.checkArgument(
            heavyHitterState.equals(HeavyHitterState.WARMUP) || heavyHitterState.equals(HeavyHitterState.STATISTICS),
            "The heavy hitter must be %s or %s: %s", HeavyHitterState.WARMUP, HeavyHitterState.STATISTICS, heavyHitterState
        );
        return domainSet;
    }
}
