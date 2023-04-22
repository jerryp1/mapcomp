package com.alibaba.mpc4j.common.circuit.z2.arithmetic;

import com.alibaba.mpc4j.common.circuit.z2.MpcBcParty;

/**
 * Abstract Z2 Arithmetic Circuit.
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/20
 */
public class AbstractZ2ArithmeticCircuit {
    /**
     * mpc boolean circuit party.
     */
    public MpcBcParty party;

    public AbstractZ2ArithmeticCircuit(MpcBcParty party) {
        this.party = party;
    }
}
