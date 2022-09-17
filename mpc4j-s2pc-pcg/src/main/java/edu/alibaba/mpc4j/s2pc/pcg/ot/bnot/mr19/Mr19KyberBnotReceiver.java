package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.*;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.params.KyberKeyPair;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BnotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19KyberBnotPtoDesc.PtoStep;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MR19-Kyber-基础n选1-OT协议接收方。
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/08/26
 */
public class Mr19KyberBnotReceiver extends AbstractBnotReceiver {
    /**
     * 使用的kyber实例
     */
    private final KyberEngine kyberEngine;
    /**
     * 公钥哈希函数
     */
    private final Hash pkHash;
    /**
     * 密钥派生函数
     */
    private final Kdf kdf;
    /**
     * OT协议接收方拥有的密钥对
     */
    private KyberKeyPair[] keyPairArray;

    public Mr19KyberBnotReceiver(Rpc receiverRpc, Party senderParty, Mr19KyberBnotConfig config) {
        super(Mr19KyberBnotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        kyberEngine = KyberEngineFactory.createInstance(config.getKyberType(), config.getParamsK());
        pkHash = HashFactory.createInstance(HashFactory.HashType.BC_SHAKE_256, kyberEngine.publicKeyByteLength());
        kdf = KdfFactory.createInstance(envType);
    }

    @Override
    public void init(int n) throws MpcAbortException {
        setInitInput(n);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BnotReceiverOutput receive(int[] choices) throws MpcAbortException {
        setPtoInput(choices);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        List<byte[]> pkPayload = generatePkPayload();
        DataPacketHeader pkHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(pkHeader, pkPayload));
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pkTime);

        stopWatch.start();
        DataPacketHeader betaHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_BETA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> betaPayload = rpc.receive(betaHeader).getPayload();
        BnotReceiverOutput receiverOutput = handleBetaPayload(betaPayload);
        stopWatch.stop();
        long betaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), betaTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }

    private List<byte[]> generatePkPayload() {
        keyPairArray = new KyberKeyPair[choices.length];
        IntStream indexIntStream = IntStream.range(0, choices.length);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        return indexIntStream
            .mapToObj(index -> {
                // 生成公私钥对
                keyPairArray[index] = kyberEngine.generateKeyPair();
                byte[] publicKey = keyPairArray[index].getPublicKey();
                // 随机公钥
                byte[][] randomKeys = new byte[n][];
                for (int i = 0; i < n; i++) {
                    if (i != choices[index]) {
                        // 生成 Hash（RandomKey）
                        randomKeys[i] = kyberEngine.randomPublicKey();
                        byte[] hashKey = pkHash.digestToBytes(randomKeys[i]);
                        // PK = PK ⊕ Hash（RandomKey）
                        BytesUtils.xori(publicKey, hashKey);
                    }
                }
                byte[][] pkPayload = new byte[n + 1][];
                for (int i = 0; i < n; i++) {
                    if (i != choices[index]) {
                        pkPayload[i] = randomKeys[i];
                    } else {
                        pkPayload[i] = publicKey;
                    }
                }
                pkPayload[n] = keyPairArray[index].getMatrixSeed();
                return pkPayload;
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
    }

    private BnotReceiverOutput handleBetaPayload(List<byte[]> betaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(betaPayload.size() == choices.length * n);
        byte[][] ciphertext = betaPayload.toArray(new byte[choices.length * n][]);
        IntStream indexIntStream = IntStream.range(0, choices.length);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][] rbArray = indexIntStream
            .mapToObj(index -> {
                byte[] rb = kyberEngine.decapsulate(ciphertext[n * index + choices[index]],
                    keyPairArray[index].getSecretKey(), keyPairArray[index].getPublicKey(), keyPairArray[index].getMatrixSeed());
                return kdf.deriveKey(rb);
            })
            .toArray(byte[][]::new);
        return new BnotReceiverOutput(n, choices, rbArray);
    }

}
