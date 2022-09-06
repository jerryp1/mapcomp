package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mrkyber19;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.Kyber;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberFactory;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
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
     * 使用的kyber实例
     */
    private Kyber kyber;
    /**
     * 随机函数
     */
    private SecureRandom secureRandom;

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
        paramsInit(config.getParamsK());
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

    private void paramsInit(int paramsK) {
        this.secureRandom = new SecureRandom();
        Hash hashFunction = HashFactory.createInstance(HashFactory.HashType.BC_BLAKE_2B_160, 16);
        this.kyber = KyberFactory.createInstance(KyberFactory.KyberType.KYBER_CPA, paramsK, secureRandom, hashFunction);
    }

    private BnotSenderOutput handlePkPayload(List<byte[]> pkPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num * (n + 1));
        IntStream keyPairArrayIntStream = IntStream.range(0, num);
        keyPairArrayIntStream = parallel ? keyPairArrayIntStream.parallel() : keyPairArrayIntStream;
        //OT协议的输出，即num * n 个选项，每个长度为16*8 bit。
        byte[][][] rbArray = new byte[num][n][CommonConstants.BLOCK_BYTE_LENGTH];
        bByte = keyPairArrayIntStream.mapToObj(index -> {
                    //收到的公钥（As+e）
                    byte[][] upperBytes = new byte[n][];
                    //用于计算hash值
                    byte[][] upperHashPkBytes = new byte[n][];
                    for (int i = 0; i < n; i++) {
                        upperBytes[i] = pkPayload.get(index * (n + 1) + i);
                        //Hash（As+e）
                        upperHashPkBytes[i] = this.kyber.hashToByte(upperBytes[i]);
                    }
                    //恢复出原油的公钥
                    for (int i = 0; i < n; i++) {
                        for (int j = 0; j < n; j++) {
                            if (i != j) {
                                // 计算A = Ri - Hash(Rj)
                                upperBytes[i] = BytesUtils.xor(upperBytes[i], upperHashPkBytes[j]);
                            }
                        }
                    }
                    //密文
                    byte[][] cipherText = new byte[n][];
                    for (int i = 0; i < n; i++) {
                        //生成需要加密的明文
                        this.secureRandom.nextBytes(rbArray[index][i]);
                        //计算加密函数，加密函数的输入是明文、公钥（As+e）部分、生成元部分、随机数种子，安全参数k
                        cipherText[i] = this.kyber.encrypt
                                (rbArray[index][i], this.kyber.polyVectorFromBytes(upperBytes[i]),
                                        pkPayload.get(index * (n + 1) + n));
                    }
                    return cipherText;
                })
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        return new MrKyber19BnotSenderOutput(n, num, rbArray);
    }
}
