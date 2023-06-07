package edu.alibaba.mpc4j.common.circuit.z2.adder;

import edu.alibaba.mpc4j.common.circuit.z2.AbstractZ2Circuit;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;

/**
 * Abstract Adder.
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public abstract class AbstractAdder extends AbstractZ2Circuit implements Adder {

    public AbstractAdder(MpcZ2cParty party) {
        super(party);
    }
}
