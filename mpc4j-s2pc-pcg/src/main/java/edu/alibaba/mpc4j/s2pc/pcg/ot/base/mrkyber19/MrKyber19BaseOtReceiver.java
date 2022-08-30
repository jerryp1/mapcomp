package edu.alibaba.mpc4j.s2pc.pcg.ot.base.mrkyber19;

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
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.AbstractBaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;


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
    private KyberVecPki[] aArray;
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
        paramsK = config.getParamsK();
        paramsInit(paramsK);
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

    private void paramsInit(int paramsK){
        switch (paramsK) {
            case 2:
                paramsPolyvecBytes = KyberParams.POLY_VECTOR_BYTES_512;
                indcpaPublicKeyBytes = KyberParams.INDCPA_PK_BYTES_512;
                break;
            case 3:
                paramsPolyvecBytes = KyberParams.POLY_VECTOR_BYTES_768;
                indcpaPublicKeyBytes = KyberParams.INDCPA_PK_BYTES_768;
                break;
            default:
                paramsPolyvecBytes = KyberParams.POLY_VECTOR_BYTES_1024;
                indcpaPublicKeyBytes = KyberParams.INDCPA_PK_BYTES_1024;
        }
    }

    private List<byte[]> generatePkPayload() {
        aArray = new KyberVecPki[choices.length];
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
                        // 生成一个符合格式的随机公钥 R_1-sigma
                        randomKeyVec = KyberPublicKeyOps.getRandomKyberPk(paramsK);
                        // 计算 R_sigma = R_sigma + Hash(R_1-sigma)
                        short[][] hashKeyVec = KyberPublicKeyOps.kyberPkHash(randomKeyVec, hashFunction);
                        publickKeyVec = KyberPublicKeyOps.kyberPkAdd(publickKeyVec, hashKeyVec);
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                    // 根据选择值将两个参数R分别放入对应位置
                    int sigma = choices[index] ? 1 : 0;
                    byte[][] pkPair = new byte[2][indcpaPublicKeyBytes];
                    //将（As+e，p_sigma）打包传输
                    System.arraycopy(Poly.polyVectorToBytes(publickKeyVec),0,
                            pkPair[sigma],0,paramsPolyvecBytes);
                    System.arraycopy(aArray[index].getPublicKeyGenerator(),0,
                            pkPair[sigma],paramsPolyvecBytes,KyberParams.SYM_BYTES);
                    //将（randomKey，p_1 - sigma）打包传输
                    System.arraycopy(Poly.polyVectorToBytes(randomKeyVec),0,
                            pkPair[1 - sigma],0,paramsPolyvecBytes);
                    System.arraycopy(KyberPublicKeyOps.getRandomKeyGenerator(),0,
                            pkPair[1 - sigma],paramsPolyvecBytes,KyberParams.SYM_BYTES);
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
            short[][] receiverPrivateKey = aArray[index].getPrivateKeyVec();
            byte[] rbDecrypt = KyberKeyOps.decrypt(betaPayload.get(2 * index + sigma),receiverPrivateKey,paramsK);
            System.arraycopy(rbDecrypt,0,rbArray[index],0, CommonConstants.BLOCK_BYTE_LENGTH);
        });

        return new BaseOtReceiverOutput(choices,rbArray);
    }
}
