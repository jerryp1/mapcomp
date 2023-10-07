package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr16;

import com.google.common.base.Stopwatch;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.filter.Filter;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.filter.SparseRandomBloomFilter;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.RandomGbfGf2eDokvs;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import org.apache.commons.lang3.time.StopWatch;

import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * RR16 malicious PSI client.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/06
 */
public class Rr16PsiClient <T> extends AbstractPsiClient<T> {
    /**
     * 核COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * COT接收方输出
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * CT协议接收方
     */
    private final CoinTossParty ctReceiver;
    /**
     * OKVS key
     */
    private byte[][] hashKeys;
    /**
     * Bloom Filter
     */
    private SparseRandomBloomFilter<byte[]> filter;
    /**
     * GBF
     */
    private byte[][] gbfStorage;
    private RandomGbfGf2eDokvs<byte[]> gbf;
    /**
     * OT choices
     */
    private boolean[] choiceBits;
    private boolean[] invChoiceBits;
    /**
     * OT number
     */
    private int nOt;
    /**
     * Array of index of valid OT instances (0 & 1)
     */
    private Integer[] zeroIndex, oneIndex;
    /**
     * PEQT byte length
     */
    private int peqtByteLength;
    /**
     * PEQT hash
     */
    private Hash peqtHash;

    public Rr16PsiClient(Rpc clientRpc, Party serverParty, Rr16PsiConfig config) {
        super(Rr16PsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(clientRpc, serverParty, config.getCoreCotConfig());
        ctReceiver = CoinTossFactory.createReceiver(clientRpc, serverParty, config.getCoinTossConfig());
        addSubPtos(coreCotReceiver);
        addSubPtos(ctReceiver);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        ctReceiver.init();
        this.hashKeys = ctReceiver.coinToss(1, CommonConstants.BLOCK_BIT_LENGTH);
        peqtByteLength = PsiUtils.getMaliciousPeqtByteLength(maxServerElementSize, maxClientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        this.nOt = Rr16PsiUtils.getOtBatchSize(maxClientElementSize);
        this.filter = SparseRandomBloomFilter.create(envType, maxClientElementSize, hashKeys[0]);
        this.gbf = new RandomGbfGf2eDokvs<>(envType, maxClientElementSize, filter.getM(),
            CommonConstants.BLOCK_BIT_LENGTH, SparseRandomBloomFilter.getHashNum(maxClientElementSize), hashKeys[0]);
        coreCotReceiver.init(nOt);
        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 4, initCotTime, "Client generates key");

        stopWatch.start();
        this.choiceBits = new boolean[nOt];
        Arrays.fill(choiceBits, 0, Rr16PsiUtils.getOtOneCount(maxClientElementSize), true);
        List<Boolean> choiceList = Arrays.asList(BinaryUtils.binaryToObjectBinary(choiceBits));
        Collections.shuffle(choiceList, secureRandom);
        choiceBits = BinaryUtils.objectBinaryToBinary(choiceList.toArray(new Boolean[nOt]));

        invChoiceBits = new boolean[nOt];
        IntStream.range(0, nOt).forEach(i -> invChoiceBits[i] = !choiceBits[i]);

        // 执行COT协议
        cotReceiverOutput = coreCotReceiver.receive(choiceBits);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 4, cotTime, "Client OT");

        stopWatch.start();
        DataPacketHeader cncChallengeHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr16PsiPtoDesc.PtoStep.SERVER_SEND_CHANLLEGE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cncChallengeList = rpc.receive(cncChallengeHeader).getPayload();
        stopWatch.stop();
        long challengeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 4, challengeTime, "Client receives challenge");

        stopWatch.start();
        List<byte[]> cncResponsePayload = genCncResponse(cncChallengeList);
        DataPacketHeader cncResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr16PsiPtoDesc.PtoStep.CLIENT_SEND_RESPONSE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cncResponseHeader, cncResponsePayload));

        List<Integer> tmp = new LinkedList<>(), tmp2 = new LinkedList<>();
        IntStream.range(0, nOt).forEach(i -> {
            if(choiceBits[i]) tmp.add(i);
            if(invChoiceBits[i]) tmp2.add(i);
        });
        Collections.shuffle(tmp, secureRandom);
        Collections.shuffle(tmp2, secureRandom);
        oneIndex = tmp.toArray(new Integer[0]);
        zeroIndex = tmp2.toArray(new Integer[0]);


        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 4, responseTime, "Client responses challenge");
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverSetSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverSetSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        Stream<T> elementStream = parallel ? clientElementArrayList.stream().parallel() : clientElementArrayList.stream();
        byte[][] clientElementByteArrays = elementStream.map(ObjectUtils::objectToByteArray).toArray(byte[][]::new);
        IntStream.range(0, clientElementByteArrays.length).forEach(index -> filter.put(clientElementByteArrays[index]));
        // permutation
        List<byte[]> piPayload = generatePermutation();
        DataPacketHeader piHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr16PsiPtoDesc.PtoStep.CLIENT_SEND_PERMUTATION.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(piHeader, piPayload));
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cotTime, "Client prepares inputs and generates Permutation");

        stopWatch.start();
        IntStream clientElementIndexIntStream = IntStream.range(0, clientElementSize);
        clientElementIndexIntStream = parallel ? clientElementIndexIntStream.parallel() : clientElementIndexIntStream;
        ArrayList<byte[]> clientOprfArrayList = clientElementIndexIntStream
            .mapToObj(index -> peqtHash.digestToBytes(gbf.decode(gbfStorage, clientElementByteArrays[index])))
            .collect(Collectors.toCollection(ArrayList::new));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, oprfTime, "Client computes oprfs and hash outputs");

        stopWatch.start();
        DataPacketHeader serverPrfFilterHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr16PsiPtoDesc.PtoStep.SERVER_SEND_PRFS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPrfFilterPayload = rpc.receive(serverPrfFilterHeader).getPayload();
        Filter<byte[]> serverPrfFilter = FilterFactory.createFilter(envType, serverPrfFilterPayload);
        Set<T> intersection = IntStream.range(0, clientElementSize)
            .mapToObj(elementIndex -> {
                T element = clientElementArrayList.get(elementIndex);
                byte[] elementPrf = clientOprfArrayList.get(elementIndex);
                return serverPrfFilter.mightContain(elementPrf) ? element : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long intersectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, intersectionTime, "Client computes the intersection");
        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }

    List<byte[]> genCncResponse(List<byte[]> cncChallengeList) {
        assert cncChallengeList.size() <= nOt - filter.getM();
        List<byte[]> challenge = new LinkedList<>();
        byte[] response = new byte[cotReceiverOutput.getRb(0).length];
        cncChallengeList.forEach(x -> {
            int index = IntUtils.byteArrayToInt(x);
            if (cotReceiverOutput.getChoice(index)){
                choiceBits[index] = false;
            } else {
                invChoiceBits[index] = false;
                BytesUtils.xori(response, cotReceiverOutput.getRb(index));
                challenge.add(x);
            }
        });
        challenge.add(response);
        return challenge;
    }

    private List<byte[]> generatePermutation(){
        byte[] filterBytes = filter.getStorage();
        this.gbfStorage = new byte[filter.getM()][];
        int[] indexes = new int[this.gbfStorage.length];
        for(int i = 0, start0 = 0, start1 = 0; i < this.gbfStorage.length; i++){
            indexes[i] = BinaryUtils.getBoolean(filterBytes, i) || start0 >= zeroIndex.length ? oneIndex[start1++] : zeroIndex[start0++];
        }
        IntStream intStream = parallel ? IntStream.range(0, this.gbfStorage.length).parallel() : IntStream.range(0, this.gbfStorage.length);
        return intStream.mapToObj(index -> {
            gbfStorage[index] = this.cotReceiverOutput.getRb(indexes[index]);
            return IntUtils.intToByteArray(indexes[index]);
        }).collect(Collectors.toList());
    }
}