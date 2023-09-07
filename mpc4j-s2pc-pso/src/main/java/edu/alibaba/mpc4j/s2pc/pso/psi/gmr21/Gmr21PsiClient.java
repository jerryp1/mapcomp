package edu.alibaba.mpc4j.s2pc.pso.psi.gmr21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtClient;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Gmr21PsiClient<T> extends AbstractPsiClient<T> {
    /**
     * MqRpmt接收方
     */
    private final MqRpmtClient mqRpmtClient;
    /**
     * MqRpmt output
     */
    private boolean[] clientVector;
    /**
     * COT接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * COT output
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * CuckooHashBinType
     */
    CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType = CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH;
    /**
     * JDK无填充AES-ECB模式名称
     */
    private static final String JDK_AES_MODE_NAME = "AES/ECB/NoPadding";
    /**
     * JDK的AES算法名称
     */
    private static final String JDK_AES_ALGORITHM_NAME = "AES";


    public Gmr21PsiClient(Rpc clientRpc, Party serverParty, Gmr21PsiConfig config) {
        super(Gmr21PsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        mqRpmtClient = MqRpmtFactory.createClient(clientRpc, serverParty, config.getMqRpmtConfig());
        coreCotReceiver = CoreCotFactory.createReceiver(clientRpc,serverParty,config.getCoreCotConfig());
        addSubPtos(mqRpmtClient);
        addSubPtos(coreCotReceiver);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        maxClientElementSize = Math.max(maxClientElementSize,2);
        maxServerElementSize = Math.max(maxServerElementSize,2);
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        mqRpmtClient.init(maxClientElementSize, maxServerElementSize);
        int maxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxServerElementSize);
        coreCotReceiver.init(maxBinNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverSetSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverSetSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        Set<ByteBuffer> paddedClientElementSet = clientElementArrayList.stream()
                .map(e -> ByteBuffer.wrap(ObjectUtils.objectToByteArray(e)))
                .collect(Collectors.toSet());
        if(clientElementSet.size() == 1) {
            byte[] paddingInput = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            secureRandom.nextBytes(paddingInput);
            paddedClientElementSet.add(ByteBuffer.wrap(paddingInput));
        }
        this.clientVector = mqRpmtClient.mqRpmt(paddedClientElementSet,Math.max(serverElementSize,2));
        stopWatch.stop();
        long mqRpmtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, mqRpmtTime);

        stopWatch.start();
        this.cotReceiverOutput = coreCotReceiver.receive(clientVector);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, cotTime);

        stopWatch.start();
        DataPacketHeader serverCipherHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Gmr21PsiPtoDesc.PtoStep.SERVER_SEND_CIPHER.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverCipherPayload = rpc.receive(serverCipherHeader).getPayload();
        extraInfo++;

        Set<T> intersection = handleServerCipherPayload(serverCipherPayload);
        stopWatch.stop();
        long serverCipherTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverCipherTime);

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }

    private Set<T> handleServerCipherPayload(List<byte[]> serverPayload){
        Hash keyHash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);

        IntStream clientVectorStream = IntStream.range(0, clientVector.length);
        clientVectorStream = parallel ? clientVectorStream.parallel() : clientVectorStream;
        Set<ByteBuffer> serverIntersectionSet = clientVectorStream.mapToObj(index -> {
                    if(clientVector[index]) {
                        SecretKeySpec secretKeySpec = new SecretKeySpec(keyHash.digestToBytes(cotReceiverOutput.getRb(index)), JDK_AES_ALGORITHM_NAME);
                        Cipher decryptCipher;
                        try{
                            decryptCipher = Cipher.getInstance(JDK_AES_MODE_NAME);
                            decryptCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
                            return ByteBuffer.wrap(decryptCipher.doFinal(serverPayload.get(index)));
                        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
                            throw new IllegalStateException("System does not support " + JDK_AES_MODE_NAME);
                        } catch (InvalidKeyException e) {
                            throw new IllegalStateException(String.format("Invalid AES key length"));
                        } catch (IllegalBlockSizeException | BadPaddingException e) {
                            throw new IllegalStateException(String.format("Invalid plaintext length"));
                        }
                    } else return null;
                }).collect(Collectors.toSet());
        serverIntersectionSet.removeIf(Objects::isNull);
        IntStream clientElementStream = IntStream.range(0, clientElementSize);
        clientElementStream = parallel ? clientElementStream.parallel() : clientElementStream;
        Set<T> intersection = clientElementStream.mapToObj(index ->
                serverIntersectionSet.contains(ByteBuffer.wrap(ObjectUtils.objectToByteArray(clientElementArrayList.get(index)))) ? clientElementArrayList.get(index): null)
                .collect(Collectors.toSet());
        intersection.removeIf(Objects::isNull);
        return intersection;
    }
}
