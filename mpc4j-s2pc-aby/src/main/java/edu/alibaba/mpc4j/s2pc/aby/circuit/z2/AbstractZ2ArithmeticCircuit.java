package edu.alibaba.mpc4j.s2pc.aby.circuit.z2;

import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;

/**
 * Abstract Z2 arithmetic circuit.
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/12
 */
public abstract class AbstractZ2ArithmeticCircuit {
    /**
     * boolean circuit party
     */
    public BcParty env;

    public AbstractZ2ArithmeticCircuit(BcParty e) {
        this.env = e;
    }
}
