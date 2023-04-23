package com.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Arrays;

/**
 * Mpc Z2 integer circuit party thread.
 *
 * @author Li Peng
 * @date 2023/4/21
 */
class Z2IntegerCircuitPartyThread extends Thread {
    /**
     * the party
     */
    private final PlainBcParty party;
    /**
     * x0
     */
    private final PlainZ2Vector[] x;
    /**
     * y0
     */
    private final PlainZ2Vector[] y;
    /**
     * z0
     */
    private PlainZ2Vector[] z;
    /**
     * operator
     */
    private final IntegerOperator operator;

    Z2IntegerCircuitPartyThread(PlainBcParty party, IntegerOperator operator, PlainZ2Vector[] x, PlainZ2Vector[] y) {
        this.party = party;
        this.operator = operator;
        this.x = x;
        this.y = y;
    }

    PlainZ2Vector[] getZ() {
        return z;
    }

    @Override
    public void run() {
        try {
            Z2IntegerCircuit circuit = new Z2IntegerCircuit(party);
            switch (operator) {
                case ADD:
                    z = Arrays.stream(circuit.add(x, y)).map(v -> (PlainZ2Vector) v).toArray(PlainZ2Vector[]::new);
                    break;
                case INCREASE_ONE:
                    z = Arrays.stream(circuit.increaseOne(x)).map(v -> (PlainZ2Vector) v).toArray(PlainZ2Vector[]::new);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + operator.name() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
