package edu.alibaba.mpc4j.common.circuit.z2.adder;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;

/**
 * Abstract Adder.
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public abstract class AbstractAdder extends Z2IntegerCircuit implements Adder {

    public AbstractAdder(MpcZ2cParty party) {
        super(party);
    }
}
