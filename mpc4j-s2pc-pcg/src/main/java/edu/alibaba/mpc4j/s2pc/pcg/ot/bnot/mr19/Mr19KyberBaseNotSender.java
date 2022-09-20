package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberEngine;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberEngineFactory;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBaseNotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BaseNotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19KyberBaseNotPtoDesc.PtoStep;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MR19-Kyber-基础n选1-OT协议发送方。
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/08/26
 */
public class Mr19KyberBaseNotSender extends AbstractBaseNotSender {
    /**
     * 使用的kyber实例
     */
    private final KyberEngine kyberEngine;
    /**
     * 公钥哈希函数
     */
    private final Hash pkHash;
    /**
     * 发送方输出
     */
    private BaseNotSenderOutput senderOutput;

    public Mr19KyberBaseNotSender(Rpc senderRpc, Party receiverParty, Mr19KyberBaseNotConfig config) {
        super(Mr19KyberBaseNotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        kyberEngine = KyberEngineFactory.createInstance(config.getKyberType(), config.getParamsK());
        pkHash = HashFactory.createInstance(HashFactory.HashType.BC_SHAKE_256, kyberEngine.publicKeyByteLength());
    }

    @Override
    public void init(int maxChoice) throws MpcAbortException {
        setInitInput(maxChoice);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BaseNotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        DataPacketHeader pkHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> pkPayload = rpc.receive(pkHeader).getPayload();
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pkTime);

        stopWatch.start();
        List<byte[]> betaPayload = handlePkPayload(pkPayload);
        DataPacketHeader betaHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_BETA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(betaHeader, betaPayload));
        stopWatch.stop();
        long betaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), betaTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private List<byte[]> handlePkPayload(List<byte[]> pkPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num * (maxChoice + 1));
        byte[][] publicKeyMatrix = pkPayload.toArray(new byte[0][]);
        byte[][][] rMatrix = new byte[num][maxChoice][kyberEngine.keyByteLength()];
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        List<byte[]> betaPayload = indexIntStream
            .mapToObj(index -> {
                byte[][] publicKeys = new byte[maxChoice][];
                byte[][] hashKeys = new byte[maxChoice][];
                for (int i = 0; i < maxChoice; i++) {
                    publicKeys[i] = publicKeyMatrix[index * (maxChoice + 1) + i];
                    hashKeys[i] = pkHash.digestToBytes(publicKeys[i]);
                }
                for (int i = 0; i < maxChoice; i++) {
                    for (int j = 0; j < maxChoice; j++) {
                        if (i != j) {
                            // A = R_i ⊕ Hash(R_j)
                            BytesUtils.xori(publicKeys[i], hashKeys[j]);
                        }
                    }
                }
                return IntStream.range(0, maxChoice)
                    .mapToObj(i -> {
                        byte[] ciphertext = kyberEngine.encapsulate(
                            rMatrix[index][i], publicKeys[i], publicKeyMatrix[index * (maxChoice + 1) + maxChoice]
                        );
                        rMatrix[index][i] = kdf.deriveKey(rMatrix[index][i]);
                        return ciphertext;
                    })
                    .toArray(byte[][]::new);
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        senderOutput = new BaseNotSenderOutput(maxChoice, rMatrix);
        return betaPayload;
    }
}
