package edu.alibaba.mpc4j.s2pc.pcg.ot.base.mrkyber19;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.*;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.AbstractBaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSenderOutput;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MRKYBER19-基础OT协议发送方。论文来源：
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
 *
 * @author Sheng Hu
 * @date 2022/08/05
 */
public class MrKyber19BaseOtSender extends AbstractBaseOtSender {
    /**
     * 配置项
     */
    private final MrKyber19BaseOtConfig config;
    /**
     * OT协议发送方参数
     */
    private List<byte[]> bByte;
    /**
     * 使用的kyber实例
     */
    private Kyber kyber;
    /**
     * 随机函数
     */
    private SecureRandom secureRandom;

    public MrKyber19BaseOtSender(Rpc senderRpc, Party receiverParty, MrKyber19BaseOtConfig config) {
        super(MrKyber19BaseOtPtoDesc.getInstance(), senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BaseOtSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        paramsInit(config.getParamsK());
        bByte = new ArrayList<>();
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
        stopWatch.start();
        DataPacketHeader pkHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), MrKyber19BaseOtPtoDesc.PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> pkPayload = rpc.receive(pkHeader).getPayload();
        stopWatch.stop();
        long betaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), betaTime);

        stopWatch.start();
        BaseOtSenderOutput senderOutput = handlePkPayload(pkPayload);
        DataPacketHeader betaHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), MrKyber19BaseOtPtoDesc.PtoStep.SENDER_SEND_B.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(betaHeader, bByte));
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pkTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private void paramsInit(int paramsK) {
        this.secureRandom = new SecureRandom();
        Hash hashFunction = HashFactory.createInstance(HashFactory.HashType.BC_BLAKE_2B_160, 16);
        this.kyber = KyberFactory.createInstance(KyberFactory.KyberType.KYBER_CPA, paramsK, secureRandom, hashFunction);
    }

    private BaseOtSenderOutput handlePkPayload(List<byte[]> pkPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num * 3);
        IntStream keyPairArrayIntStream = IntStream.range(0, num);
        keyPairArrayIntStream = parallel ? keyPairArrayIntStream.parallel() : keyPairArrayIntStream;
        //OT协议的输出
        byte[][] r0Array = new byte[num][CommonConstants.BLOCK_BYTE_LENGTH];
        byte[][] r1Array = new byte[num][CommonConstants.BLOCK_BYTE_LENGTH];
        bByte = keyPairArrayIntStream.mapToObj(index -> {
                    //进行加密的明文
                    this.secureRandom.nextBytes(r0Array[index]);
                    this.secureRandom.nextBytes(r1Array[index]);
                    // 读取公钥（As+e）部分
                    byte[] upperPkR0 = pkPayload.get(index * 3);
                    byte[] upperPkR1 = pkPayload.get(index * 3 + 1);
                    // 计算A0 = R0 xor Hash(R1)、A1 = R1 xor Hash(R0)
                    byte[] hashKeyR0 = this.kyber.hashToByte(upperPkR0);
                    byte[] hashKeyR1 = this.kyber.hashToByte(upperPkR1);
                    upperPkR0 = BytesUtils.xor(upperPkR0, hashKeyR1);
                    upperPkR1 = BytesUtils.xor(upperPkR1, hashKeyR0);

                    //计算密文
                    byte[][] cipherText = new byte[2][];
                    //加密函数的输入是明文、公钥（As+e）部分、生成元部分、随机数种子，安全参数k
                    cipherText[0] = this.kyber.encrypt(r0Array[index], upperPkR0, pkPayload.get(index * 3 + 2));
                    cipherText[1] = this.kyber.encrypt(r1Array[index], upperPkR1, pkPayload.get(index * 3 + 2));
                    return cipherText;
                })
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        return new BaseOtSenderOutput(r0Array, r1Array);
    }
}
