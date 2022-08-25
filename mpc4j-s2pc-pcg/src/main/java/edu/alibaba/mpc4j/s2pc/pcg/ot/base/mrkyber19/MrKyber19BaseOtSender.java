package edu.alibaba.mpc4j.s2pc.pcg.ot.base.mrkyber19;

import edu.alibaba.mpc4j.common.kyber.provider.kyber.*;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
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
     * 公钥（As+e）长度
     */
    private int paramsPolyvecBytes;
    /**
     * 公钥（（As+e），p）的长度
     */
    private int indcpaPublicKeyBytes;
    /**
     * 安全参数 K
     */
    private int paramsK;

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

    public BaseOtSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        paramsK = config.getParamsK();
        paramsInit(paramsK);
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
    private void paramsInit(int paramsK){
        switch (paramsK) {
            case 2:
                paramsPolyvecBytes = KyberParams.paramsPolyvecBytesK512;
                indcpaPublicKeyBytes = KyberParams.paramsIndcpaPublicKeyBytesK512;
                break;
            case 3:
                paramsPolyvecBytes = KyberParams.paramsPolyvecBytesK768;
                indcpaPublicKeyBytes = KyberParams.paramsIndcpaPublicKeyBytesK768;
                break;
            default:
                paramsPolyvecBytes = KyberParams.paramsPolyvecBytesK1024;
                indcpaPublicKeyBytes = KyberParams.paramsIndcpaPublicKeyBytesK1024;
        }
    }

    private BaseOtSenderOutput handlePkPayload(List<byte[]> pkPayload) throws MpcAbortException{
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num * 2);
        Hash hashFunction = HashFactory.createInstance(envType, 32);
        IntStream keyPairArrayIntStream = IntStream.range(0, num);
        keyPairArrayIntStream = parallel ? keyPairArrayIntStream.parallel() : keyPairArrayIntStream;
        //OT协议的输出
        byte[][] r0Array = new byte[num][CommonConstants.BLOCK_BYTE_LENGTH];
        byte[][] r1Array = new byte[num][CommonConstants.BLOCK_BYTE_LENGTH];
        bByte = keyPairArrayIntStream.mapToObj(index -> {
            //计算密文时的随机数
            byte[] seed0 = new byte[KyberParams.paramsSymBytes];
            byte[] seed1 = new byte[KyberParams.paramsSymBytes];
            //进行加密的密文
            byte[] message0 = new byte[KyberParams.paramsSymBytes];
            byte[] message1 = new byte[KyberParams.paramsSymBytes];
            SecureRandom Sr = new SecureRandom();
            Sr.nextBytes(seed0);
            Sr.nextBytes(seed1);
            Sr.nextBytes(message0);
            Sr.nextBytes(message1);
            //因为消息m必须要256bit，因此传递的密文中选取前128bit作为OT的输出
            r0Array[index] = Arrays.copyOfRange(message0,0,CommonConstants.BLOCK_BYTE_LENGTH);
            r1Array[index] = Arrays.copyOfRange(message1,0,CommonConstants.BLOCK_BYTE_LENGTH);
            // 读取接收端参数对R0、R1
            byte[] upperR0 = pkPayload.get(index * 2);
            byte[] upperR1 = pkPayload.get(index * 2 + 1);
            // 读取公钥（As+e）部分
            byte[] upperPKR0 = Arrays.copyOfRange(upperR0,0, paramsPolyvecBytes);
            byte[] upperPKR1 = Arrays.copyOfRange(upperR1,0, paramsPolyvecBytes);
            short[][] upperVectorR0 = Poly.polyVectorFromBytes(upperPKR0);
            short[][] upperVectorR1 = Poly.polyVectorFromBytes(upperPKR1);
            // 计算A0 = R0 - Hash(R1)、A1 = R1 - Hash(R0)
            short[][] upperA0 =
                    KyberPublicKeyOps.kyberPKSub(upperVectorR0,KyberPublicKeyOps.kyberPKHash(upperVectorR1, hashFunction));
            short[][] upperA1 =
                    KyberPublicKeyOps.kyberPKSub(upperVectorR1,KyberPublicKeyOps.kyberPKHash(upperVectorR0, hashFunction));

            //计算密文
            byte [][] cipherText = new byte[2][];
            cipherText[0] = KyberKeyOps.
                    encrypt(message0,upperA0,
                            Arrays.copyOfRange(upperR0,paramsPolyvecBytes,indcpaPublicKeyBytes),
                            seed0,paramsK);
            cipherText[1] = KyberKeyOps.
                    encrypt(message1,upperA1,
                            Arrays.copyOfRange(upperR1,paramsPolyvecBytes,indcpaPublicKeyBytes),
                            seed1,paramsK);
            return cipherText;
        })
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        return new BaseOtSenderOutput(r0Array, r1Array);
    }
}
