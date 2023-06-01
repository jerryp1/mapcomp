package edu.alibaba.mpc4j.common.circuit.z2.adder;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Ripple carry adder.
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public class RippleCarryAdder extends AbstractAdder {

    public RippleCarryAdder(MpcZ2cParty party) {
        super(party);
    }

    @Override
    public MpcZ2Vector[] add(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray, MpcZ2Vector cin) throws MpcAbortException {
        checkInputs(xiArray, yiArray);
        MpcZ2Vector[] zs = new MpcZ2Vector[xiArray.length + 1];
        MpcZ2Vector[] t = addOneBit(xiArray[xiArray.length - 1], yiArray[yiArray.length - 1], cin);
        zs[zs.length - 1] = t[0];
        for (int i = zs.length - 1; i > 1; i--) {
            t = addOneBit(xiArray[i - 2], yiArray[i - 2], t[1]);
            zs[i - 1] = t[0];
        }
        zs[0] = t[1];
        return zs;
    }

    /**
     * Full 1-bit adders.
     *
     * @param x x.
     * @param y y.
     * @param c carry-in bit.
     * @return (carry_out bit, result).
     */
    private MpcZ2Vector[] addOneBit(MpcZ2Vector x, MpcZ2Vector y, MpcZ2Vector c) throws MpcAbortException {
        MpcZ2Vector[] z = new MpcZ2Vector[2];
        MpcZ2Vector t1 = party.xor(x, c);
        MpcZ2Vector t2 = party.xor(y, c);
        z[0] = party.xor(x, t2);
        t1 = party.and(t1, t2);
        z[1] = party.xor(c, t1);
        return z;
    }
}
