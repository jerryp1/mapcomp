package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

import java.util.ArrayList;

/**
 * Unbalanced Circuit PSI server output.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public class UnbalancedCpsiServerOutput<T> {
    /**
     * the server table
     */
    private final ArrayList<T> table;
    /**
     * the server share bits
     */
    private final SquareShareZ2Vector z0;
    /**
     * β
     */
    private final int beta;

    public UnbalancedCpsiServerOutput(ArrayList<T> table, SquareShareZ2Vector z0) {
        MathPreconditions.checkPositive("β", table.size());
        this.table = table;
        beta = table.size();
        MathPreconditions.checkEqual("share bit length", "β", z0.getNum(), beta);
        this.z0 = z0;
    }

    public int getBeta() {
        return beta;
    }

    public ArrayList<T> getTable() {
        return table;
    }

    public SquareShareZ2Vector getZ0() {
        return z0;
    }
}
