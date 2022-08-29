package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mrkyber19;


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
        paramsInit(paramsK);
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

    private BnotSenderOutput handlePkPayload(List<byte[]> pkPayload) throws MpcAbortException{
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num * n);
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
            byte[][] upper = new byte[n][];
            for(int i = 0; i < n;i++) {
                upper[i] = pkPayload.get(index * n + i);
                byte[] upperPk = Arrays.copyOfRange(upper[i],0, paramsPolyvecBytes);
                //As+e
                upperVector[i] = Poly.polyVectorFromBytes(upperPk);
                //Hash（As+e）
                upperHashPkVector[i] = KyberPublicKeyOps.kyberPKHash(upperVector[i],hashFunction);
            }
            //恢复出原油的公钥
            for(int i = 0;i < n;i++){
                upperPkVector[i] = upperVector[i];
                for(int j = 0; j < n;j++){
                    if(i != j){
                        // 计算A = Ri - Hash(Rj)
                       upperPkVector[i] = KyberPublicKeyOps.kyberPKSub(upperPkVector[i],upperHashPkVector[j]);
                    }
                }
            }
            //密文
            byte[][] cipherText = new byte[n][];
            for(int i = 0;i < n;i++){
                //生成随机数种子
                byte[] seed = new byte[KyberParams.paramsSymBytes];
                //生成需要加密的明文
                byte[] message = new byte[KyberParams.paramsSymBytes];
                SecureRandom sr = new SecureRandom();
                sr.nextBytes(seed);
                sr.nextBytes(message);
                //因为消息m必须要256bit，因此传递的密文中选取前128bit作为OT的输出
                rbArray[index][i] = Arrays.copyOfRange(message,0,CommonConstants.BLOCK_BYTE_LENGTH);
                //计算加密函数
                cipherText[i] = KyberKeyOps.
                        encrypt(message,upperPkVector[i],
                                Arrays.copyOfRange(upper[i],paramsPolyvecBytes,indcpaPublicKeyBytes),seed,paramsK);
            }
            return cipherText;
                })
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        return new MrKyber19BnotSenderOutput(n,num, rbArray);
    }
}
