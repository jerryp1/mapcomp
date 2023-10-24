package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * circuit PSI client output, where server encodes payload into circuit
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public class PlpsiClientOutput<T> extends PlpsiShareOutput{
    /**
     * the client table
     */
    private final ArrayList<T> table;

    public PlpsiClientOutput(EnvType envType, boolean parallel, ArrayList<T> table, SquareZ2Vector z1, Payload... payloads) {
        super(z1, payloads);
        MathPreconditions.checkPositive("β", table.size());
        this.table = table;
        MathPreconditions.checkEqual("z1.bitNum", "β", z1.getNum(), table.size());
    }

    public ArrayList<T> getTable() {
        return table;
    }
}
