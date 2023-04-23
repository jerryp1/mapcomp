package edu.alibaba.mpc4j.s2pc.aby.circuit.z2.arithmetic;

/**
 * Z2 Arithmetic Sender Thread.
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/13
 */

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.circuit.z2.Z2IntegerCircuit;

import java.util.Arrays;

/**
 * Z2 arithmetic sender thread.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
class Z2ArithmeticSenderThread extends Thread {
    /**
     * the sender
     */
    private final BcParty sender;
    /**
     * x0
     */
    private final SquareShareZ2Vector[] shareX0;
    /**
     * y0
     */
    private final SquareShareZ2Vector[] shareY0;
    /**
     * total bit num
     */
    private final int totalBitNum;
    /**
     * z0
     */
    private SquareShareZ2Vector[] shareZ0;
    /**
     * operator
     */
    private final ArithmeticOperator operator;


    Z2ArithmeticSenderThread(BcParty sender, ArithmeticOperator arithmeticOperator, SquareShareZ2Vector[] shareX0, SquareShareZ2Vector[] shareY0) {
        this.sender = sender;
        this.operator = arithmeticOperator;
        this.shareX0 = shareX0;
        this.shareY0 = shareY0;
        totalBitNum = Arrays.stream(shareX0).map(SquareShareZ2Vector::getBitVector).mapToInt(BitVector::bitNum).sum();
    }

    SquareShareZ2Vector[] getShareZ0() {
        return shareZ0;
    }

    @Override
    public void run() {
        try {
            sender.init(totalBitNum, totalBitNum);
            Z2IntegerCircuit circuit = new Z2IntegerCircuit(sender);
            switch (operator) {
                case LEQ:
                    shareZ0 = new SquareShareZ2Vector[]{circuit.leq(shareX0, shareY0)};
                    break;
                case BIT_ADD:
                    SquareShareZ2Vector cin = SquareShareZ2Vector.createZeros(shareX0[0].getNum());
                    shareZ0 = circuit.bitAdd(shareX0[0], shareY0[0], cin);
                    break;
                case ADD:
                    shareZ0 = circuit.add(shareX0, shareY0);
                    break;
                case SUB:
                    shareZ0 = circuit.sub(shareX0, shareY0);
                    break;
                case NOT:
                    shareZ0 = circuit.not(shareX0);
                    break;
                default:
                    throw new IllegalStateException("Invalid arithmetic operator: " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

