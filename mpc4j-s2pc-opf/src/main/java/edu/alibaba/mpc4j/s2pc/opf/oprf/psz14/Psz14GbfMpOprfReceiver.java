package edu.alibaba.mpc4j.s2pc.opf.oprf.psz14;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.filter.BloomFilter;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractMpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class Psz14GbfMpOprfReceiver extends AbstractMpOprfReceiver {
    /**
     * 核COT协议发送方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * COT发送方输出
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * OKVS type
     */
    private final Gf2eDokvsFactory.Gf2eDokvsType okvsType;
    /**
     * Filter type
     */
    private final FilterType filterType = FilterType.NAIVE_RANDOM_BLOOM_FILTER;
    /**
     * OKVS keys
     */
    private byte[][] okvsKeys;
    /**
     * Bloom Filter
     */
    private BloomFilter<byte[]> filter;
    /**
     * GBF
     */
    private Gf2eDokvs<byte[]> gbf;

    public Psz14GbfMpOprfReceiver(Rpc receiverRpc, Party senderParty, Psz14GbfMpOprfConfig config) {
        super(Psz14GbfMpOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPtos(coreCotReceiver);
        okvsType = config.getBinaryOkvsType();
    }

    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        //SemiHonest Setting
        this.okvsKeys = CommonUtils.generateRandomKeys(Gf2eDokvsFactory.getHashKeyNum(okvsType), secureRandom);
        this.gbf = Gf2eDokvsFactory.createBinaryInstance(envType,okvsType,maxBatchSize, CommonConstants.BLOCK_BIT_LENGTH, okvsKeys);
        int m = gbf.getM();
        this.filter = FilterFactory.createBloomFilter(envType, filterType, maxBatchSize, okvsKeys[0]);;
        List<byte[]> okvsKeyPayload = Arrays.stream(okvsKeys).collect(Collectors.toList());
        DataPacketHeader okvsKeyHeader = new DataPacketHeader(
            this.encodeTaskId, getPtoDesc().getPtoId(), Psz14GbfMpOprfPtoDesc.PtoStep.RECEIVER_SEND_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(okvsKeyHeader, okvsKeyPayload));
        coreCotReceiver.init(m);
        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initCotTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public MpOprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException {
        setPtoInput(inputs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        IntStream.range(0, inputs.length).forEach(index -> filter.put(inputs[index]));

        // 执行COT协议
        cotReceiverOutput = coreCotReceiver.receive(BinaryUtils.byteArrayToBinary(filter.getStorage()));

        MpOprfReceiverOutput receiverOutput = generateOprfOutput();
        gbf = null;
        okvsKeys = null;
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, cotTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private MpOprfReceiverOutput generateOprfOutput() {
        IntStream inputIndexStream = IntStream.range(0, batchSize);
        inputIndexStream = parallel ? inputIndexStream.parallel() : inputIndexStream;
        byte[][] prfs = inputIndexStream
                .mapToObj(index -> gbf.decode(cotReceiverOutput.getRbArray(),inputs[index]))
                .toArray(byte[][]::new);
        int oprfLength = CommonConstants.BLOCK_BIT_LENGTH / Byte.SIZE;
        cotReceiverOutput = null;
        return new MpOprfReceiverOutput(oprfLength, inputs, prfs);
    }
}
