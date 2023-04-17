package edu.alibaba.mpc4j.s2pc.aby.circuit.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

/**
 * Z2 Arithmetic Circuit Interface.
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/12
 */
public interface Z2ArithmeticCircuit {

    /**
     * Add operation.
     *
     * @param x x.
     * @param y y.
     * @return output.
     */
    SquareShareZ2Vector[] add(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y) throws MpcAbortException;

    /**
     * Subtract operation.
     *
     * @param x x.
     * @param y y.
     * @return output.
     */
    SquareShareZ2Vector[] sub(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y) throws MpcAbortException;

    /**
     * Less equal than operation.
     *
     * @param x x.
     * @param y y.
     * @return output.
     */
    SquareShareZ2Vector leq(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y) throws MpcAbortException;

    /**
     * Bit and operation
     *
     * @param x   x.
     * @param y   y.
     * @param cin carry in.
     * @return output.
     */
    SquareShareZ2Vector[] bitAdd(SquareShareZ2Vector x, SquareShareZ2Vector y, SquareShareZ2Vector cin) throws MpcAbortException;

}
