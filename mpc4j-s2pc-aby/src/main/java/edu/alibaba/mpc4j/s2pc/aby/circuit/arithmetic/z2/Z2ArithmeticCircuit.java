package edu.alibaba.mpc4j.s2pc.aby.circuit.arithmetic.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.ShareVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

/**
 * 算术电路计算库
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/12
 */
public interface Z2ArithmeticCircuit {

    /**
     * 加法
     * @param x 输入数据
     * @param y 输入数据
     * @return 输出数据
     */
    SquareShareZ2Vector[] add(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y);

    /**
     * 减法
     * @param x 输入数据
     * @param y 输入数据
     * @return 输出数据
     */
    SquareShareZ2Vector[] sub(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y) throws MpcAbortException;

    /**
     * 小于等于
     * @param x 输入数据
     * @param y 输入数据
     * @return 输出数据
     */
    SquareShareZ2Vector leq(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y) throws MpcAbortException;


    SquareShareZ2Vector[] bitAdd(SquareShareZ2Vector x, SquareShareZ2Vector y, SquareShareZ2Vector cin) throws MpcAbortException;

}
