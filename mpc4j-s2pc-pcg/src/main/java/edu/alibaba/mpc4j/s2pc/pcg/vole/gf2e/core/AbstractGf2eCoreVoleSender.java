package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eGadget;

import java.util.Arrays;

/**
 * Abstract GF2E-core VOLE sender.
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public abstract class AbstractGf2eCoreVoleSender extends AbstractTwoPartyPto implements Gf2eCoreVoleSender {
    /**
     * the GF2E instance
     */
    protected Gf2e gf2e;
    /**
     * l
     */
    protected int l;
    /**
     * byteL
     */
    protected int byteL;
    /**
     * the GF2E gadget
     */
    protected Gf2eGadget gf2eGadget;
    /**
     * max num
     */
    private int maxNum;
    /**
     * x
     */
    protected byte[][] x;
    /**
     * num
     */
    protected int num;

    protected AbstractGf2eCoreVoleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Gf2eCoreVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput(Gf2e gf2e, int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        this.gf2e = gf2e;
        gf2eGadget = new Gf2eGadget(gf2e);
        l = gf2e.getL();
        byteL = gf2e.getByteL();
        initState();
    }

    protected void setPtoInput(byte[][] x) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", x.length, maxNum);
        num = x.length;
        this.x = Arrays.stream(x)
            .peek(xi -> MathPreconditions.checkEqual("x.length", "l(B)", xi.length, byteL))
            .toArray(byte[][]::new);
        extraInfo++;
    }
}
