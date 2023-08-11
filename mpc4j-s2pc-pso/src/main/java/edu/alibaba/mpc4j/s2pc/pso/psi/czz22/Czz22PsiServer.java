package edu.alibaba.mpc4j.s2pc.pso.psi.czz22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtServer;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Czz22PsiServer <T> extends AbstractPsiServer<T> {
    /**
     * MqRpmt发送方
     */
    private final MqRpmtServer mqRpmtServer;
    /**
     * MqRpmt output
     */
    private ByteBuffer[] serverVector;
    /**
     * COT发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * COT output
     */
    private CotSenderOutput cotSenderOutput;

    public Czz22PsiServer(Rpc serverRpc, Party clientParty, Czz22PsiConfig config) {
        super(Czz22PsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        mqRpmtServer = MqRpmtFactory.createServer(serverRpc, clientParty, config.getMqRpmtConfig());
        coreCotSender = CoreCotFactory.createSender(serverRpc,clientParty,config.getCoreCotConfig());
        addSubPtos(mqRpmtServer);
        addSubPtos(coreCotSender);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        maxClientElementSize = Math.max(maxClientElementSize, 2);
        maxServerElementSize = Math.max(maxServerElementSize, 2);
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        mqRpmtServer.init(maxServerElementSize, maxClientElementSize);
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, maxServerElementSize);

        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        Set<ByteBuffer> paddedServerElementSet = serverElementArrayList.stream()
            .map(e -> ByteBuffer.wrap(ObjectUtils.objectToByteArray(e)))
            .collect(Collectors.toSet());
        if(serverElementSet.size() == 1) {
            byte[] paddingInput = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            secureRandom.nextBytes(paddingInput);
            paddedServerElementSet.add(ByteBuffer.wrap(paddingInput));
        }
        this.serverVector =  mqRpmtServer.mqRpmt(paddedServerElementSet, Math.max(clientElementSize, 2));
        long mqRpmtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, mqRpmtTime);

        stopWatch.start();
        this.cotSenderOutput =  coreCotSender.send(serverVector.length);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, cotTime);

        stopWatch.start();

        List<byte[]> serverCipherPayload = generateCipherPayload();
        DataPacketHeader serverCipherHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Czz22PsiPtoDesc.PtoStep.SERVER_SEND_CIPHER.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverCipherHeader, serverCipherPayload));
        extraInfo++;
        serverVector = null;
        cotSenderOutput = null;
        stopWatch.stop();
        long serverCipherTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverCipherTime);

        logPhaseInfo(PtoState.PTO_END);
    }

    private List<byte[]> generateCipherPayload() {

        Hash keyHash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        IntStream serverVectorStream = IntStream.range(0, serverVector.length);
        serverVectorStream = parallel ? serverVectorStream.parallel() : serverVectorStream;
        List<byte[]> cipherList = serverVectorStream
            .mapToObj(index -> {
                SecretKeySpec secretKeySpec = new SecretKeySpec(keyHash.digestToBytes(cotSenderOutput.getR1(index)), Czz22PsiConfig.Cipher_ALGORITHM_NAME);
                Cipher encryptCipher;
                try{
                    encryptCipher = Cipher.getInstance(Czz22PsiConfig.Cipher_MODE_NAME);
                    encryptCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
                    return encryptCipher.doFinal(serverVector[index].array());
                } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
                    throw new IllegalStateException("System does not support " + Czz22PsiConfig.Cipher_MODE_NAME);
                } catch (InvalidKeyException e) {
                    throw new IllegalStateException("Invalid AES key length");
                } catch (IllegalBlockSizeException | BadPaddingException e) {
                    throw new IllegalStateException("Invalid plaintext length");
                }
            }).collect(Collectors.toList());
        return cipherList;
    }
}
