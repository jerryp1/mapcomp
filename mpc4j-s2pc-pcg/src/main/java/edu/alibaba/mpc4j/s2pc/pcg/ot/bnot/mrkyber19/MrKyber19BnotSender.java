package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mrkyber19;

import edu.alibaba.mpc4j.common.tool.crypto.kyber.*;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BnotSenderOutput;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MRKYBER19-基础N选1-OT协议发送方。论文来源：
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
 *
 * @author Sheng Hu
 * @date 2022/08/26
 */
public class MrKyber19BnotSender extends AbstractBnotSender {
    /**
     * 配置项
     */
    private final MrKyber19BnotConfig config;
    /**
     * OT协议发送方参数
     */
    private List<byte[]> bByte;
    /**
     * 安全参数 K
     */
    private int paramsK;

    public MrKyber19BnotSender(Rpc senderRpc, Party receiverParty, MrKyber19BnotConfig config) {
        super(MrKyber19BnotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public void init(int n) throws MpcAbortException {
        setInitInput(n);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BnotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        paramsK = config.getParamsK();
        bByte = new ArrayList<>();
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
        stopWatch.start();
        DataPacketHeader pkHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), MrKyber19BnotPtoDesc.PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> pkPayload = rpc.receive(pkHeader).getPayload();
        stopWatch.stop();
        long betaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), betaTime);

        stopWatch.start();
        BnotSenderOutput senderOutput = handlePkPayload(pkPayload);
        DataPacketHeader betaHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), MrKyber19BnotPtoDesc.PtoStep.SENDER_SEND_B.ordinal(), extraInfo,
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

    private BnotSenderOutput handlePkPayload(List<byte[]> pkPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num * (n + 1));
        Hash hashFunction = HashFactory.createInstance(envType, 32);
        IntStream keyPairArrayIntStream = IntStream.range(0, num);
        keyPairArrayIntStream = parallel ? keyPairArrayIntStream.parallel() : keyPairArrayIntStream;
        //OT协议的输出，即num * n 个选项，每个长度为16*8 bit。
        byte[][][] rbArray = new byte[num][n][CommonConstants.BLOCK_BYTE_LENGTH];
        bByte = keyPairArrayIntStream.mapToObj(index -> {
                    //收到的公钥（As+e）
                    short[][][] upperVector = new short[n][][];
                    //用于计算减去另外N-1个公钥的hash值
                    short[][][] upperPkVector = new short[n][][];
                    //用于计算hash值
                    short[][][] upperHashPkVector = new short[n][][];
                    for (int i = 0; i < n; i++) {
                        byte[] upperPk = pkPayload.get(index * (n + 1) + i);
                        //As+e
                        upperVector[i] = Poly.polyVectorFromBytes(upperPk);
                        //Hash（As+e）
                        upperHashPkVector[i] = KyberPublicKeyOps.kyberPkHash(upperVector[i], hashFunction);
                    }
                    //恢复出原油的公钥
                    for (int i = 0; i < n; i++) {
                        upperPkVector[i] = upperVector[i];
                        for (int j = 0; j < n; j++) {
                            if (i != j) {
                                // 计算A = Ri - Hash(Rj)
                                upperPkVector[i] = KyberPublicKeyOps.kyberPkSub(upperPkVector[i], upperHashPkVector[j]);
                            }
                        }
                    }
                    //密文
                    byte[][] cipherText = new byte[n][];
                    for (int i = 0; i < n; i++) {
                        //生成随机数种子
                        byte[] seed = new byte[KyberParams.SYM_BYTES];
                        //生成需要加密的明文
                        byte[] message = new byte[KyberParams.SYM_BYTES];
                        SecureRandom sr = new SecureRandom();
                        sr.nextBytes(seed);
                        sr.nextBytes(message);
                        //因为消息m必须要256bit，因此传递的密文中选取前128bit作为OT的输出
                        rbArray[index][i] = Arrays.copyOfRange(message, 0, CommonConstants.BLOCK_BYTE_LENGTH);
                        //计算加密函数，加密函数的输入是明文、公钥（As+e）部分、生成元部分、随机数种子，安全参数k
                        cipherText[i] = KyberKeyOps.
                                encrypt(message, upperPkVector[i],
                                        pkPayload.get(index * (n + 1) + n), seed, paramsK);
                    }
                    return cipherText;
                })
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        return new MrKyber19BnotSenderOutput(n, num, rbArray);
    }
}
