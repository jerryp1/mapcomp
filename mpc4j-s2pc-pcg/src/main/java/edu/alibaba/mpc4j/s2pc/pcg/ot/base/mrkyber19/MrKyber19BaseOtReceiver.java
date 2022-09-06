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
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.AbstractBaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;


import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * MRKYBER19-基础OT协议接收方。论文来源：
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
 *
 * @author Sheng Hu
 * @date 2022/08/05
 */
public class MrKyber19BaseOtReceiver extends AbstractBaseOtReceiver {
    /**
     * 配置项
     */
    private final MrKyber19BaseOtConfig config;
    /**
     * OT协议接收方参数
     */
    private KyberVecKeyPair[] aArray;

    /**
     * 使用的kyber实例
     */
    private Kyber kyber;


    public MrKyber19BaseOtReceiver(Rpc receiverRpc, Party senderParty, MrKyber19BaseOtConfig config) {
        super(MrKyber19BaseOtPtoDesc.getInstance(), receiverRpc, senderParty, config);
        this.config = config;
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
        paramsInit(config.getParamsK());
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        List<byte[]> pkPayload = generatePkPayload();
        DataPacketHeader pkHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), MrKyber19BaseOtPtoDesc.PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(pkHeader, pkPayload));
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);

        stopWatch.reset();
        info("{}{} Recv. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pkTime);

        stopWatch.start();
        DataPacketHeader betaHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), MrKyber19BaseOtPtoDesc.PtoStep.SENDER_SEND_B.ordinal(), extraInfo,
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

    private void paramsInit(int paramsK) {
        SecureRandom secureRandom = new SecureRandom();
        /**
         * hash函数实例
         */
        Hash hashFunction = HashFactory.createInstance(HashFactory.HashType.BC_BLAKE_2B_160, 16);
        this.kyber = KyberFactory.createInstance(KyberFactory.KyberType.KYBER_CPA, paramsK, secureRandom, hashFunction);
    }

    private List<byte[]> generatePkPayload() {
        aArray = new KyberVecKeyPair[choices.length];
        // 公钥生成流
        IntStream pkIntStream = IntStream.range(0, choices.length);
        pkIntStream = parallel ? pkIntStream.parallel() : pkIntStream;
        return pkIntStream
                .mapToObj(index -> {
                    // 公钥（As+e）的向量
                    byte[] publickKeyBytes;
                    // 随机的向量，R_1-sigma
                    byte[] randomKeyByte;
                    // 随机向量的生成元，g（R_1-sigma）
                    // 随机生成一组钥匙对
                    aArray[index] = this.kyber.generateKyberVecKeys();
                    // 读取多项式格式下的公钥
                    publickKeyBytes = this.kyber.polyVectorToBytes(aArray[index].getPublicKeyVec());
                    // 生成一个符合格式的随机公钥 R_1-sigma
                    randomKeyByte = this.kyber.getRandomKyberPk();
                    // 计算 R_sigma = R_sigma + Hash(R_1-sigma)
                    byte[] hashKeyByte = this.kyber.hashToByte(randomKeyByte);
                    publickKeyBytes = BytesUtils.xor(publickKeyBytes, hashKeyByte);
                    // 根据选择值将两个参数R分别放入对应位置
                    int sigma = choices[index] ? 1 : 0;
                    return this.kyber.packageTwoKeys
                            (publickKeyBytes,randomKeyByte,aArray[index].getPublicKeyGenerator(),sigma);
                })
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
    }

    private BaseOtReceiverOutput handleBetaPayload(List<byte[]> betaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(betaPayload.size() == choices.length * 2);
        //解密消息获得相应的选择B_sigma
        byte[][] rbArray = new byte[choices.length][];
        IntStream decryptArrayIntStream = IntStream.range(0, choices.length);
        decryptArrayIntStream = parallel ? decryptArrayIntStream.parallel() : decryptArrayIntStream;
        decryptArrayIntStream.forEach(index -> {
            int sigma = choices[index] ? 1 : 0;
            rbArray[index] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            short[][] receiverPrivateKey = aArray[index].getPrivateKeyVec();
            byte[] rbDecrypt = this.kyber.decrypt(betaPayload.get(2 * index + sigma), receiverPrivateKey);
            System.arraycopy(rbDecrypt, 0, rbArray[index], 0, CommonConstants.BLOCK_BYTE_LENGTH);
        });

        return new BaseOtReceiverOutput(choices, rbArray);
    }
}
