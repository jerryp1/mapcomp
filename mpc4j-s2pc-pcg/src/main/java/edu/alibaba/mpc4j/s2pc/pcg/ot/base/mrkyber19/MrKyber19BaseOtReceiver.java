package edu.alibaba.mpc4j.s2pc.pcg.ot.base.mrkyber19;

import edu.alibaba.mpc4j.common.kyber.provider.KyberPackedPKI;
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
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.AbstractBaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;


import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;



/**
 * MRKYBER19-基础OT协议接收方。论文来源：
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
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
    private KyberPackedPKI[] aArray;
    /**
     * hash函数单位输出长度
     */
    static final int HASH_UNIT_BYTE_LENGTH = 32;

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

    private List<byte[]> generatePkPayload() {
        aArray = new KyberPackedPKI[choices.length];
        Hash hashFunction = HashFactory.createInstance(envType,32);
        // 公钥生成流
        IntStream pkIntStream = IntStream.range(0, choices.length);
        pkIntStream = parallel ? pkIntStream.parallel() : pkIntStream;
        return pkIntStream
                .mapToObj(index -> {
                    byte[] randomKey;
                    short[][] publickKeyVec;
                    byte[] publicKey;
                    try {
                        aArray[index] = Indcpa.generateKyberKeys(4);
                        // 随机生成一组钥匙对
                        publicKey = aArray[index].getPackedPublicKey();
                        //info("Receiver publickey {}  {})" ,index, publicKey);
                        // 生成一个随机的参数R_1-sigma
                        randomKey = KyberPublicKeyOps.getRandomKyberPK(4);
                        byte[] hashKey = KyberPublicKeyOps.kyberPKHash(randomKey, hashFunction);
                        publickKeyVec = Poly.polyVectorFromBytes(Arrays.copyOfRange(publicKey, 0, KyberParams.paramsPolyvecBytesK1024));
                        publickKeyVec = KyberPublicKeyOps.kyberPKAdd(publickKeyVec, Poly.polyVectorFromBytes(hashKey));
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }

                    //info("Receiver Ab {}  {} l{})", publicKey, choices[index],publicKey.length);
                    // 根据选择值将两个参数R分别放入对应位置
                    int sigma = choices[index] ? 1 : 0;
                    byte[][] pkPair = new byte[2][KyberParams.paramsIndcpaPublicKeyBytesK1024];
                    System.arraycopy(Poly.polyVectorToBytes(publickKeyVec),0,
                            pkPair[sigma],0,KyberParams.paramsPolyvecBytesK1024);
                    System.arraycopy(publicKey,KyberParams.paramsPolyvecBytesK1024,
                            pkPair[sigma],KyberParams.paramsPolyvecBytesK1024,KyberParams.paramsSymBytes);
                    System.arraycopy(randomKey,0,pkPair[1 - sigma],0,KyberParams.paramsPolyvecBytesK1024);
                    System.arraycopy(publicKey,KyberParams.paramsPolyvecBytesK1024,
                            pkPair[1 - sigma],KyberParams.paramsPolyvecBytesK1024,KyberParams.paramsSymBytes);
                    info("R A0 {} {})", randomKey,randomKey.length);
                    info("R A1 {} {})", Poly.polyVectorToBytes(publickKeyVec),Poly.polyVectorToBytes(publickKeyVec).length);
                    return pkPair;
                })
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
    }

    private  BaseOtReceiverOutput handleBetaPayload(List<byte[]> betaPayload) throws MpcAbortException{
        MpcAbortPreconditions.checkArgument(betaPayload.size() == choices.length * 2);
        //解密消息获得相应的选择B_sigma
        byte[][] rbArray = new byte[choices.length][];
        IntStream decryptArrayIntStream = IntStream.range(0, choices.length);
        decryptArrayIntStream = parallel ? decryptArrayIntStream.parallel() : decryptArrayIntStream;
        decryptArrayIntStream.forEach(index ->{
            int sigma = choices[index] ? 1 : 0;
            rbArray[index] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            byte[] receiverPrivateKey = aArray[index].getPackedPrivateKey();
            byte[] rbDecrypt = Indcpa.decrypt(betaPayload.get(2 * index + sigma),receiverPrivateKey,4);
            System.arraycopy(rbDecrypt,0,rbArray[index],0, CommonConstants.BLOCK_BYTE_LENGTH);
        });

        return new BaseOtReceiverOutput(choices,rbArray);
    }
}
