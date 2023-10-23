package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * circuit PSI server output, where server encodes payload into circuit
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public class PlpsiServerOutput {
    /**
     * the server share indicator bits
     */
    private final SquareZ2Vector z1;

    /**
     * the server received shared payload
     */
    private final Payload payload;

    public PlpsiServerOutput(SquareZ2Vector z1, Payload payload) {
        if (payload != null) {
            MathPreconditions.checkEqual("z1.bitNum", "payload.length", z1.getNum(), payload.getBeta());
        }
        this.z1 = z1;
        this.payload = payload;
    }

    public int getBeta() {
        return z1.getNum();
    }

    public SquareZ2Vector getZ1() {
        return z1;
    }

    public Payload getPayload() {
        return payload;
    }

    public SquareZlVector getZlPayload() {
        if (payload != null) {
            return payload.getZlPayload();
        } else {
            return null;
        }
    }

    public SquareZ2Vector[] getZ2Payload() {
        if (payload != null) {
            return payload.getZ2Payload();
        } else {
            return null;
        }
    }

}
