package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mrkyber19;

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
     * OT协议发送方生成的密文
     */
    private List<byte[]> cipherList;
    /**
     * 使用的kyber实例
     */
    private final Kyber kyber;

    public MrKyber19BnotSender(Rpc senderRpc, Party receiverParty, MrKyber19BnotConfig config) {
        super(MrKyber19BnotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        this.kyber = KyberFactory.createInstance(config.getKyberType(), config.getParamsK(), envType);
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
        cipherList = new ArrayList<>();
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
                taskId, getPtoDesc().getPtoId(), MrKyber19BnotPtoDesc.PtoStep.SENDER_SEND_Cipher.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(betaHeader, cipherList));
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pkTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private BnotSenderOutput handlePkPayload(List<byte[]> pkPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num * (n + 1));
        byte[][] publicKey = pkPayload.toArray(new byte[num * (n + 1)][]);
        IntStream keyPairArrayIntStream = IntStream.range(0, num);
        keyPairArrayIntStream = parallel ? keyPairArrayIntStream.parallel() : keyPairArrayIntStream;
        // OT协议的输出，即num * n 个选项，每个长度为16*8 bit。
        byte[][][] rbArray = new byte[num][n][CommonConstants.BLOCK_BYTE_LENGTH];
        cipherList = keyPairArrayIntStream.mapToObj(index -> {
                    // 收到的公钥（As+e）
                    byte[][] upperBytes = new byte[n][];
                    // 用于计算hash值
                    byte[][] upperHashPkBytes = new byte[n][];
                    for (int i = 0; i < n; i++) {
                        upperBytes[i] = publicKey[index * (n + 1) + i];
                        // Hash（As+e）
                        upperHashPkBytes[i] = this.kyber.hashToByte(upperBytes[i]);
                    }
                    // 恢复出原有的公钥
                    for (int i = 0; i < n; i++) {
                        for (int j = 0; j < n; j++) {
                            if (i != j) {
                                // 计算A = Ri xor Hash(Rj)
                                BytesUtils.xori(upperBytes[i], upperHashPkBytes[j]);
                            }
                        }
                    }
                    // 密文
                    byte[][] cipherText = new byte[n][];
                    for (int i = 0; i < n; i++) {
                        //计算KEM，KEM的输入是秘密值、公钥（As+e）部分、生成元部分、随机数种子，安全参数k
                        cipherText[i] = this.kyber.encaps
                                (rbArray[index][i], upperBytes[i], publicKey[index * (n + 1) + n]);
                    }
                    return cipherText;
                })
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        return new MrKyber19BnotSenderOutput(n, num, rbArray);
    }
}
