package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kGadget;

import java.util.Arrays;

/**
 * GF2K-核VOLE协议发送方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public abstract class AbstractGf2kCoreVoleSender extends AbstractTwoPartyPto implements Gf2kCoreVoleSender {
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
     * 最大数量
     */
    private int maxNum;
    /**
     * x
     */
    protected byte[][] x;
    /**
     * 数量
     */
    protected int num;

    protected AbstractGf2kCoreVoleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Gf2kCoreVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        gf2k = Gf2kFactory.createInstance(envType);
        l = gf2k.getL();
        byteL = gf2k.getByteL();
        gf2kGadget = new Gf2kGadget(gf2k);
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
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
