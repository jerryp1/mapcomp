package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kGadget;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * GF2K-核VOLE接收方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public abstract class AbstractGf2kCoreVoleReceiver extends AbstractTwoPartyPto implements Gf2kCoreVoleReceiver {
    /**
     * GF2K算法
     */
    protected final Gf2k gf2k;
    /**
     * 元素比特长度
     */
    protected final int l;
    /**
     * 元素字节长度
     */
    protected final int byteL;
    /**
     * GF2K小工具
     */
    protected final Gf2kGadget gf2kGadget;
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

    protected AbstractGf2kCoreVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kCoreVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        gf2k = Gf2kFactory.createInstance(envType);
        l = gf2k.getL();
        byteL = gf2k.getByteL();
        gf2kGadget = new Gf2kGadget(gf2k);
    }

    protected void setInitInput(byte[] delta, int maxNum) {
        MathPreconditions.checkEqual("Δ.length", "l(B)", delta.length, byteL);
        // 拷贝一份
        this.delta = BytesUtils.clone(delta);
        deltaBinary = gf2kGadget.bitDecomposition(delta);
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkReadyState();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        extraInfo++;
    }
}
