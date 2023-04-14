package edu.alibaba.mpc4j.s2pc.aby.circuit.arithmetic.z2;

import edu.alibaba.mpc4j.s2pc.aby.basics.bc.AbstractBcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

/**
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/12
 */
public abstract class AbstractZ2ArithmeticCircuit {
    /**
     * 执行电路协议的抽象参与方
     */
    public BcParty env;

    public AbstractZ2ArithmeticCircuit(BcParty e) {
        this.env = e;
    }
}
