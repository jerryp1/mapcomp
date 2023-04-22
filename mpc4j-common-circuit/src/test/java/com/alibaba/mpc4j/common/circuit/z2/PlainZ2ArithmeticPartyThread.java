package com.alibaba.mpc4j.common.circuit.z2;

import com.alibaba.mpc4j.common.circuit.z2.arithmetic.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Arrays;

/**
 * Mpc Z2 Arithmetic Party Thread.
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/21
 */
class PlainZ2ArithmeticPartyThread extends Thread {
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
    private final ArithmeticOperator operator;

    PlainZ2ArithmeticPartyThread(PlainBcParty party, ArithmeticOperator arithmeticOperator, PlainZ2Vector[] x, PlainZ2Vector[] y) {
        this.party = party;
        this.operator = arithmeticOperator;
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
                case LEQ:
                    z = new PlainZ2Vector[]{(PlainZ2Vector) circuit.leq(x, y)};
                    break;
                case BIT_ADD:
                    PlainZ2Vector cin = PlainZ2Vector.createZeros(x[0].getNum());
                    z = Arrays.stream(circuit.bitAdd(x[0], y[0], cin)).map(v -> (PlainZ2Vector) v).toArray(PlainZ2Vector[]::new);
                    break;
                case ADD:
                    z = Arrays.stream(circuit.add(x, y)).map(v -> (PlainZ2Vector) v).toArray(PlainZ2Vector[]::new);
                    break;
                case SUB:
                    z = Arrays.stream(circuit.sub(x, y)).map(v -> (PlainZ2Vector) v).toArray(PlainZ2Vector[]::new);
                    break;
                case NOT:
                    z = Arrays.stream(circuit.not(x)).map(v -> (PlainZ2Vector) v).toArray(PlainZ2Vector[]::new);
                    break;
                default:
                    throw new IllegalStateException("Invalid arithmetic operator: " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
