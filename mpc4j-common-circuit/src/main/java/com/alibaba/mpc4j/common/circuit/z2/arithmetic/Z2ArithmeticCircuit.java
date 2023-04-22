package com.alibaba.mpc4j.common.circuit.z2.arithmetic;

import com.alibaba.mpc4j.common.circuit.Circuit;
import com.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Z2 Arithmetic Circuit Interface.
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/20
 */
public interface Z2ArithmeticCircuit extends Circuit {
    /**
     * Add operation.
     *
     * @param x x.
     * @param y y.
     * @return output.
     */
    MpcZ2Vector[] add(MpcZ2Vector[] x, MpcZ2Vector[] y) throws MpcAbortException;

    /**
     * Subtract operation.
     *
     * @param x x.
     * @param y y.
     * @return output.
     */
    MpcZ2Vector[] sub(MpcZ2Vector[] x, MpcZ2Vector[] y) throws MpcAbortException;

    /**
     * Less equal than operation.
     *
     * @param x x.
     * @param y y.
     * @return output.
     */
    MpcZ2Vector leq(MpcZ2Vector[] x, MpcZ2Vector[] y) throws MpcAbortException;

    /**
     * Bit and operation
     *
     * @param x   x.
     * @param y   y.
     * @param cin carry in.
     * @return output.
     */
    MpcZ2Vector[] bitAdd(MpcZ2Vector x, MpcZ2Vector y, MpcZ2Vector cin) throws MpcAbortException;

}
