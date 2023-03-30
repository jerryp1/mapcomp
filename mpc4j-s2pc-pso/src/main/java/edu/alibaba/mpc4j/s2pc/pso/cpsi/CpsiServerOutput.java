package edu.alibaba.mpc4j.s2pc.pso.cpsi;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;

import java.util.ArrayList;

/**
 * Circuit PSI server output.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class CpsiServerOutput<T> {
    /**
     * the server table
     */
    private final ArrayList<T> table;
    /**
     * the server share bits
     */
    private final SquareSbitVector z0;
    /**
     * β
     */
    private final int beta;

    public CpsiServerOutput(ArrayList<T> table, SquareSbitVector z0) {
        MathPreconditions.checkPositive("β", table.size());
        this.table = table;
        beta = table.size();
        MathPreconditions.checkEqual("share bit length", "β", z0.bitNum(), beta);
        this.z0 = z0;
    }

    public int getBeta() {
        return beta;
    }

    public ArrayList<T> getTable() {
        return table;
    }

    public SquareSbitVector getZ0() {
        return z0;
    }
}
