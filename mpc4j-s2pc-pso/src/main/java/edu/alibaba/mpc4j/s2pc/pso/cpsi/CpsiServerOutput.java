package edu.alibaba.mpc4j.s2pc.pso.cpsi;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

import java.nio.ByteBuffer;

/**
 * Circuit PSI server output.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class CpsiServerOutput {
    /**
     * the server table
     */
    private final ByteBuffer[] table;
    /**
     * the server share bits
     */
    private final SquareShareZ2Vector z0;

    public CpsiServerOutput(ByteBuffer[] table, SquareShareZ2Vector z0) {
        MathPreconditions.checkPositive("β", table.length);
        this.table = table;
        MathPreconditions.checkEqual("z0.bitNum", "β", z0.getNum(), table.length);
        this.z0 = z0;
    }

    public int getBeta() {
        return table.length;
    }

    public ByteBuffer[] getTable() {
        return table;
    }

    public SquareShareZ2Vector getZ0() {
        return z0;
    }
}
