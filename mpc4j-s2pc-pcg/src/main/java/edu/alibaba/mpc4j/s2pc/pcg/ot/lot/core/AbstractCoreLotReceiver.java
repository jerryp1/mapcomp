package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoder;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoderFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.Arrays;

/**
 * 核2^l选1-OT协议接收方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/5/25
 */
public abstract class AbstractCoreLotReceiver extends AbstractTwoPartyPto implements CoreLotReceiver {
    /**
     * 输入比特长度
     */
    protected int inputBitLength;
    /**
     * 输入字节长度
     */
    protected int inputByteLength;
    /**
     * 输出随机量比特长度
     */
    protected int outputBitLength;
    /**
     * 输出随机量字节长度
     */
    protected int outputByteLength;
    /**
     * 线性编码器
     */
    protected LinearCoder linearCoder;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 数量
     */
    protected int num;
    /**
     * 字节数量
     */
    protected int byteNum;
    /**
     * 选择值数组
     */
    protected byte[][] choices;

    protected AbstractCoreLotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, CoreLotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(int inputBitLength, int maxNum) {
        MathPreconditions.checkPositive("inputBitLength", inputBitLength);
        this.inputBitLength = inputBitLength;
        inputByteLength = CommonUtils.getByteLength(inputBitLength);
        linearCoder = LinearCoderFactory.getInstance(inputBitLength);
        outputBitLength = linearCoder.getCodewordBitLength();
        outputByteLength = linearCoder.getCodewordByteLength();
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(byte[][] choices) {
        checkReadyState();
        MathPreconditions.checkPositiveInRangeClosed("num", choices.length, maxNum);
        num = choices.length;
        byteNum = CommonUtils.getByteLength(num);
        // 拷贝一份
        this.choices = Arrays.stream(choices)
            .peek(choice -> {
                MathPreconditions.checkEqual("choice.length", "inputByteLength", choice.length, inputByteLength);
                Preconditions.checkArgument(BytesUtils.isReduceByteArray(choice, inputBitLength));
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        extraInfo++;
    }
}
