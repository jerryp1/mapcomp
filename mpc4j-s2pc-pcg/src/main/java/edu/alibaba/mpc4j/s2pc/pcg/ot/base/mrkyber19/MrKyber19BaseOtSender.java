package edu.alibaba.mpc4j.s2pc.pcg.ot.base.mrkyber19;



import edu.alibaba.mpc4j.common.kyber.provider.kyber.Indcpa;
import edu.alibaba.mpc4j.common.kyber.provider.kyber.KyberParams;
import edu.alibaba.mpc4j.common.kyber.provider.kyber.KyberPublicKeyOps;
import edu.alibaba.mpc4j.common.kyber.provider.kyber.Poly;
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
     * 随机数种子的安全参数
     */
    private final int secureParameter = 128;
    /**
     * hash函数单位输出长度
     */
    static final int HASH_UNIT_BYTE_LENGTH = 32;

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

    private BaseOtSenderOutput handlePkPayload(List<byte[]> pkPayload) throws MpcAbortException{
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num * 2);
        Hash hashFunction = HashFactory.createInstance(envType, 32);
        IntStream keyPairArrayIntStream = IntStream.range(0, num);
        keyPairArrayIntStream = parallel ? keyPairArrayIntStream.parallel() : keyPairArrayIntStream;
        byte[][] r0Array = new byte[num][CommonConstants.BLOCK_BYTE_LENGTH];
        byte[][] r1Array = new byte[num][CommonConstants.BLOCK_BYTE_LENGTH];
        bByte = keyPairArrayIntStream.mapToObj(index -> {
            //随机数
            byte[] seed0 = new byte[KyberParams.paramsSymBytes];
            byte[] seed1 = new byte[KyberParams.paramsSymBytes];
            byte[] message0 = new byte[KyberParams.paramsSymBytes];
            byte[] message1 = new byte[KyberParams.paramsSymBytes];
            SecureRandom Sr = new SecureRandom();
            Sr.nextBytes(seed0);
            Sr.nextBytes(seed1);
            Sr.nextBytes(message0);
            Sr.nextBytes(message1);
            r0Array[index] = Arrays.copyOfRange(message0,0,CommonConstants.BLOCK_BYTE_LENGTH);
            r1Array[index] = Arrays.copyOfRange(message1,0,CommonConstants.BLOCK_BYTE_LENGTH);
            // 读取接收端参数对R0、R1
            byte[] upperR0 = pkPayload.get(index * 2);
            byte[] upperR1 = pkPayload.get(index * 2 + 1);

            byte[] upperPKR0 = Arrays.copyOfRange(upperR0,0, KyberParams.paramsPolyvecBytesK1024);
            byte[] upperPKR1 = Arrays.copyOfRange(upperR1,0, KyberParams.paramsPolyvecBytesK1024);
            // 计算A0 = R0 - Hash(R1)、A1 = R1 - Hash(R0)

            short[][] upperVectorR0 = Poly.polyVectorFromBytes(upperPKR0);
            short[][] upperVectorR1 = Poly.polyVectorFromBytes(upperPKR1);
            short[][] upperA0 = KyberPublicKeyOps.kyberPKSub(upperVectorR0,Poly.polyVectorFromBytes(KyberPublicKeyOps.kyberPKHash(upperPKR1, hashFunction)));
            short[][] upperA1 = KyberPublicKeyOps.kyberPKSub(upperVectorR1,Poly.polyVectorFromBytes(KyberPublicKeyOps.kyberPKHash(upperPKR0, hashFunction)));
            //生成B0、B1
            info("Sender A0 {} {})", upperA0,upperA0.length);
            info("Sender A1 {} {})", upperA1,upperA1.length);
            byte[] publicKeyA0 = new byte[KyberParams.paramsIndcpaPublicKeyBytesK1024];
            System.arraycopy(Poly.polyVectorToBytes(upperA0),0,
                            publicKeyA0,0,KyberParams.paramsPolyvecBytesK1024);
            System.arraycopy(upperR0,KyberParams.paramsPolyvecBytesK1024,
                            publicKeyA0,KyberParams.paramsPolyvecBytesK1024,KyberParams.paramsSymBytes);
            byte[] publicKeyA1 = new byte[KyberParams.paramsIndcpaPublicKeyBytesK1024];
            System.arraycopy(Poly.polyVectorToBytes(upperA1),0,
                            publicKeyA1,0,KyberParams.paramsPolyvecBytesK1024);
            System.arraycopy(upperR1,KyberParams.paramsPolyvecBytesK1024,
                            publicKeyA1,KyberParams.paramsPolyvecBytesK1024,KyberParams.paramsSymBytes);
            byte [][] cipherText = new byte[2][];
            cipherText[0] = Indcpa.encrypt(message0,publicKeyA0,seed0,4);
            cipherText[1] = Indcpa.encrypt(message1,publicKeyA1,seed1,4);
            //cipherText[0] = cipherGenerator(senderKeyGen1024,upperA0,r0Array[index]);
            //cipherText[1] = cipherGenerator(senderKeyGen1024,upperA1,r1Array[index]);
            return cipherText;
        })
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        return new BaseOtSenderOutput(r0Array, r1Array);
    }

    private byte[] largeByteHashFunction(Hash hashFunction,byte[] hashInput) {
        int byteLenth = hashInput.length;
        int startPoint = 0;
        byte[] hashOutput = new byte[byteLenth];
        while(byteLenth > HASH_UNIT_BYTE_LENGTH){
            System.arraycopy(hashFunction.digestToBytes(Arrays.copyOfRange(hashInput,startPoint,startPoint + HASH_UNIT_BYTE_LENGTH)),
                    0,hashOutput,startPoint,HASH_UNIT_BYTE_LENGTH);
            startPoint = startPoint + HASH_UNIT_BYTE_LENGTH;
            byteLenth = byteLenth - HASH_UNIT_BYTE_LENGTH;
        }
        if(byteLenth > 0){
            System.arraycopy(hashFunction.digestToBytes(Arrays.copyOfRange(hashInput,startPoint,startPoint + byteLenth)),
                    0,hashOutput,startPoint,byteLenth);
        }
        return hashOutput;
    }
}
