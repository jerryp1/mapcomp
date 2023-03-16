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
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * Abstract GF2K-core VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/15
 */
public abstract class AbstractGf2kCoreVoleReceiver extends AbstractTwoPartyPto implements Gf2kCoreVoleReceiver {
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
     * Δ
     */
    protected byte[] delta;
    /**
     * max num
     */
    private int maxNum;
    /**
     * num
     */
    protected int num;

    protected AbstractGf2kCoreVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kCoreVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(byte[] delta, int maxNum) {
        gf2e = Gf2eFactory.createInstance(envType, CommonConstants.BLOCK_BIT_LENGTH);
        l = gf2e.getL();
        byteL = gf2e.getByteL();
        Preconditions.checkArgument(gf2e.validateRangeElement(delta), "Δ must be in range [0, 2^%s)", l);
        this.delta = BytesUtils.clone(delta);
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        extraInfo++;
    }
}
