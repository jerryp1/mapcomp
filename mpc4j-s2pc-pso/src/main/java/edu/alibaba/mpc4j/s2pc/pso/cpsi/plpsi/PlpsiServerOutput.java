package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

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

    public PlpsiServerOutput(EnvType envType, boolean parallel, SquareZ2Vector z1, SquareZ2Vector[] payload, boolean isBinaryShare) {
        MathPreconditions.checkEqual("z1.bitNum", "payload.length", z1.getNum(), payload.length);
        this.z1 = z1;
        this.payload = new Payload(envType, parallel, payload, isBinaryShare);
    }

    public PlpsiServerOutput(SquareZ2Vector z1, Payload payload) {
        MathPreconditions.checkEqual("z1.bitNum", "payload.length", z1.getNum(), payload.getBeta());
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

}
