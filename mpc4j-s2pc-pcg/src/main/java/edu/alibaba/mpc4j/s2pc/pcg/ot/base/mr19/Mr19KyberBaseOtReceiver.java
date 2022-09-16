package edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19;

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
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.AbstractBaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19KyberBaseOtPtoDesc.PtoStep;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MRKYBER19-基础OT协议接收方。论文来源：
 * <p>
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
 * </p>
 *
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/08/05
 */
public class Mr19KyberBaseOtReceiver extends AbstractBaseOtReceiver {
    /**
     * Kyber引擎
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
     * OT协议接收方密钥对
     */
    private KyberKeyPair[] keyArray;

    public Mr19KyberBaseOtReceiver(Rpc receiverRpc, Party senderParty, Mr19KyberBaseOtConfig config) {
        super(Mr19KyberBaseOtPtoDesc.getInstance(), receiverRpc, senderParty, config);
        kyberEngine = KyberEngineFactory.createInstance(config.getKyberType(), config.getParamsK());
        pkHash = HashFactory.createInstance(HashFactory.HashType.BC_SHAKE_256, kyberEngine.publicKeyByteLength());
        kdf = KdfFactory.createInstance(envType);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BaseOtReceiverOutput receive(boolean[] choices) throws MpcAbortException {
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
            taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_B.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> betaPayload = rpc.receive(betaHeader).getPayload();
        stopWatch.stop();
        long betaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), betaTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return handleBetaPayload(betaPayload);
    }

    private List<byte[]> generatePkPayload() {
        keyArray = new KyberKeyPair[choices.length];
        // 公钥生成流
        IntStream pkIntStream = IntStream.range(0, choices.length);
        pkIntStream = parallel ? pkIntStream.parallel() : pkIntStream;
        return pkIntStream
            .mapToObj(index -> {
                keyArray[index] = kyberEngine.generateKeyPair();
                // 公钥
                byte[] pk = keyArray[index].getPublicKey();
                // 随机公钥
                byte[] randomPk = kyberEngine.randomPublicKey();
                // 计算 R_σ = R_σ xor Hash(R_{1 - σ})
                byte[] hashKey = pkHash.digestToBytes(randomPk);
                byte[] maskPk = BytesUtils.xor(pk, hashKey);
                if (choices[index]) {
                    return new byte[][] {
                        randomPk, maskPk, keyArray[index].getMatrixSeed()
                    };
                } else {
                    return new byte[][] {
                        maskPk, randomPk, keyArray[index].getMatrixSeed()
                    };
                }
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
    }

    private BaseOtReceiverOutput handleBetaPayload(List<byte[]> betaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(betaPayload.size() == choices.length * 2);
        byte[][] ciphertexts = betaPayload.toArray(new byte[choices.length * 2][]);
        // 解密消息获得相应的选择B_σ
        byte[][] rbArray = new byte[choices.length][];
        IntStream indexIntStream = IntStream.range(0, choices.length);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(index -> {
            int sigma = choices[index] ? 1 : 0;
            // 解密函数——在cpa方案中无需公钥，在cca方案中需要公钥。
            byte[] decapsulateKey = kyberEngine.decapsulate(ciphertexts[2 * index + sigma],
                keyArray[index].getSecretKey(), keyArray[index].getPublicKey(), keyArray[index].getMatrixSeed());
            rbArray[index] = kdf.deriveKey(decapsulateKey);
        });

        return new BaseOtReceiverOutput(choices, rbArray);
    }
}
