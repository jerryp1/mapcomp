package edu.alibaba.mpc4j.s2pc.aby.circuit.arithmetic.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

import java.util.Arrays;

/**
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/13
 */
class Z2ArithmeticReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final BcParty receiver;
    /**
     * x1
     */
    private final SquareShareZ2Vector[] shareX1;
    /**
     * y1
     */
    private final SquareShareZ2Vector[] shareY1;
    /**
     * total bit num
     */
    private final int totalBitNum;
    /**
     * z1
     */
    private SquareShareZ2Vector[] shareZ1;
    private ArithmeticOperator operator;

    Z2ArithmeticReceiverThread(BcParty receiver, ArithmeticOperator arithmeticOperator, SquareShareZ2Vector[] shareX1, SquareShareZ2Vector[] shareY1) {
        this.receiver = receiver;
        this.operator = arithmeticOperator;
        this.shareX1 = shareX1;
        this.shareY1 = shareY1;
        totalBitNum = Arrays.stream(shareX1).map(SquareShareZ2Vector::getBitVector).mapToInt(BitVector::bitNum).sum();

    }

    SquareShareZ2Vector[] getShareZ1() {
        return shareZ1;
    }

    @Override
    public void run() {
        try {
            receiver.init(totalBitNum, totalBitNum);
            Z2IntegerCircuit circuit = new Z2IntegerCircuit(receiver);
            switch (operator) {
                case LEQ:
                    shareZ1 = new SquareShareZ2Vector[] {circuit.leq(shareX1, shareY1)};
                    break;
                case BIT_ADD:
                    SquareShareZ2Vector cin = SquareShareZ2Vector.createZeros(shareX1[0].getNum());
                    shareZ1 = circuit.bitAdd(shareX1[0], shareY1[0] ,cin);
                    break;
                default:
                    throw new IllegalStateException("Invalid arithmetic operator: " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
