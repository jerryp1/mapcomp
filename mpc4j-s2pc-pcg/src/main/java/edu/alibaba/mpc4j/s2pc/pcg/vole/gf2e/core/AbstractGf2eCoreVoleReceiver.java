package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eGadget;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * GF2K-核VOLE接收方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public abstract class AbstractGf2eCoreVoleReceiver extends AbstractTwoPartyPto implements Gf2eCoreVoleReceiver {
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
     * 关联值Δ
     */
    protected byte[] delta;
    /**
     * 关联值Δ的比特表示
     */
    protected boolean[] deltaBinary;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 数量
     */
    protected int num;

    protected AbstractGf2eCoreVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2eCoreVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(Gf2e gf2e, byte[] delta, int maxNum) {
        this.gf2e = gf2e;
        l = gf2e.getL();
        byteL = gf2e.getByteL();
        Preconditions.checkArgument(gf2e.validateRangeElement(delta), "Δ must be in {0,1}^" + l);
        this.delta = BytesUtils.clone(delta);
        deltaBinary = gf2eGadget.bitDecomposition(delta);
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
