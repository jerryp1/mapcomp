package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mrkyber19;


import edu.alibaba.mpc4j.common.kyber.provider.KyberVecPKI;
import edu.alibaba.mpc4j.common.kyber.provider.kyber.KyberKeyOps;
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
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BnotReceiverOutput;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MRKYBER19-基础N选1-OT协议接收方。论文来源：
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
 * @author Sheng Hu
 * @date 2022/08/26
 */
public class Mrkyber19BnotReceiver extends AbstractBnotReceiver {
    /**
     * 配置项
     */
    private final MrKyber19BnotConfig config;
    /**
     * OT协议接收方参数
     */
    private KyberVecPKI[] aArray;
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

    public Mrkyber19BnotReceiver(Rpc receiverRpc, Party senderParty,MrKyber19BnotConfig config){
        super(MrKyber19BnotPtoDesc.getInstance(),receiverRpc,senderParty,config);
        this.config = config;
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
        paramsK = config.getParamsK();
        paramsInit(paramsK);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        List<byte[]> pkPayload = generatePkPayload();
        DataPacketHeader pkHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), MrKyber19BnotPtoDesc.PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(pkHeader, pkPayload));
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);

        stopWatch.reset();
        info("{}{} Recv. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pkTime);

        stopWatch.start();
        DataPacketHeader betaHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), MrKyber19BnotPtoDesc.PtoStep.SENDER_SEND_B.ordinal(), extraInfo,
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

    private List<byte[]> generatePkPayload() {
        aArray = new KyberVecPKI[choices.length];
        Hash hashFunction = HashFactory.createInstance(envType,32);
        // 公钥生成流
        IntStream pkIntStream = IntStream.range(0, choices.length);
        pkIntStream = parallel ? pkIntStream.parallel() : pkIntStream;
        return pkIntStream
                .mapToObj(index -> {
                    // 公钥（As+e）的向量
                    short[][] publickKeyVec;
                    // 随机的向量，R_1-sigma
                    short[][] randomKeyVec;
                    // 随机向量的生成元，g（R_1-sigma）
                    try {
                        // 随机生成一组钥匙对
                        aArray[index] = KyberKeyOps.generateKyberKeys(paramsK);
                        // 读取多项式格式下的公钥
                        publickKeyVec = aArray[index].getPublicKeyVec();
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                    byte[][] pkPair = new byte[n][indcpaPublicKeyBytes];
                    for (int i = 0;i < n; i++){
                        if(i != choices[index]){
                            //生成（randomKey，p_1 - sigma）并打包传输
                            randomKeyVec = KyberPublicKeyOps.getRandomKyberPK(paramsK);
                            short[][] hashKeyVec = KyberPublicKeyOps.kyberPKHash(randomKeyVec, hashFunction);
                            publickKeyVec = KyberPublicKeyOps.kyberPKAdd(publickKeyVec, hashKeyVec);
                            System.arraycopy(Poly.polyVectorToBytes(randomKeyVec),0,
                                    pkPair[i],0,paramsPolyvecBytes);
                            System.arraycopy(KyberPublicKeyOps.getRandomKeyGenerator(paramsK),0,
                                    pkPair[i],paramsPolyvecBytes,KyberParams.paramsSymBytes);
                        }
                    }
                    //将经过N-1个公钥遮掩后的（As+e，p_sigma）打包传输
                    System.arraycopy(Poly.polyVectorToBytes(publickKeyVec),0,
                            pkPair[choices[index]],0,paramsPolyvecBytes);
                    System.arraycopy(aArray[index].getPublicKeyGenerator(),0,
                            pkPair[choices[index]],paramsPolyvecBytes,KyberParams.paramsSymBytes);
                    return pkPair;
                })
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
    }

    private BnotReceiverOutput handleBetaPayload(List<byte[]> betaPayload) throws MpcAbortException{
        MpcAbortPreconditions.checkArgument(betaPayload.size() == choices.length * n);
        //解密消息获得相应的选择B_sigma
        byte[][] rbArray = new byte[choices.length][];
        IntStream decryptArrayIntStream = IntStream.range(0, choices.length);
        decryptArrayIntStream = parallel ? decryptArrayIntStream.parallel() : decryptArrayIntStream;
        decryptArrayIntStream.forEach(index ->{
            rbArray[index] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            short[][] receiverPrivateKey = aArray[index].getPrivateKeyVec();
            byte[] rbDecrypt = KyberKeyOps.decrypt(betaPayload.get(n * index + choices[index]),receiverPrivateKey,paramsK);
            System.arraycopy(rbDecrypt,0,rbArray[index],0, CommonConstants.BLOCK_BYTE_LENGTH);
        });

        return new BnotReceiverOutput(n,choices,rbArray);
    }

}
