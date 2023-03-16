package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory;

import java.util.Arrays;

/**
 * Abstract GF2K-core VOLE sender.
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public abstract class AbstractGf2kCoreVoleSender extends AbstractTwoPartyPto implements Gf2kCoreVoleSender {
    /**
     * the GF2K instance
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

    protected AbstractGf2kCoreVoleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Gf2kCoreVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput(int maxNum) {
        gf2e = Gf2eFactory.createInstance(envType, CommonConstants.BLOCK_BIT_LENGTH);
        l = gf2e.getL();
        byteL = gf2e.getByteL();
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(byte[][] x) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", x.length, maxNum);
        num = x.length;
        this.x = Arrays.stream(x)
            .peek(xi -> Preconditions.checkArgument(
                gf2e.validateElement(xi), "xi must be in range [0, 2^%s): %s", l, xi
            ))
            .toArray(byte[][]::new);
        extraInfo++;
    }
}
