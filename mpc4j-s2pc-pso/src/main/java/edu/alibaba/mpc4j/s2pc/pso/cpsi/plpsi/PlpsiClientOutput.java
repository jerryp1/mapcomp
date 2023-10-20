package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.ArrayList;

/**
 * circuit PSI client output, where server encodes payload into circuit
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public class PlpsiClientOutput<T> {
    /**
     * the client table
     */
    private final ArrayList<T> table;
    /**
     * the client share indicator bits
     */
    private final SquareZ2Vector z1;
    /**
     * the client share payload
     */
    private final Payload payload;

    public PlpsiClientOutput(EnvType envType, boolean parallel, ArrayList<T> table, SquareZ2Vector z1, byte[][] payload, int bitLen, boolean isBinaryShare) {
        MathPreconditions.checkPositive("β", table.size());
        this.table = table;
        MathPreconditions.checkEqual("z1.bitNum", "β", z1.getNum(), table.size());
        this.z1 = z1;
        MathPreconditions.checkEqual("payload.length", "β", payload.length, table.size());
        this.payload = new Payload(envType, parallel, payload, bitLen,  isBinaryShare);
    }

    public int getBeta() {
        return table.size();
    }

    public ArrayList<T> getTable() {
        return table;
    }

    public SquareZ2Vector getZ1() {
        return z1;
    }

    public Payload getPayload() {
        return payload;
    }
}
