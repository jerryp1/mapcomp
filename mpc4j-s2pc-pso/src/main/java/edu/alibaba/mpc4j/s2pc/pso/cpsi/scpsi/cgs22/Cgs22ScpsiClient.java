package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.cgs22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.RbopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.RbopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.RbopprfSender;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmReceiver;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.AbstractScpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.cgs22.Cgs22ScpsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * PSTY19 server-payload circuit PSI client.
 *
 * @author Weiran Liu
 * @date 2023/4/19
 */
public class Cgs22ScpsiClient extends AbstractScpsiClient {
    /**
     * related batched OPPRF sender
     */
    private final RbopprfSender rbopprfSender;
    /**
     * private set membership receiver
     */
    private final PsmReceiver psmReceiver;
    /**
     * d
     */
    private final int d;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * cuckoo hash num
     */
    private final int cuckooHashNum;
    /**
     * β
     */
    private int beta;
    /**
     * simple hash bin
     */
    private RandomPadHashBin<ByteBuffer> simpleHashBin;
    /**
     * target array
     */
    private byte[][] targetArray;
    /**
     * input arrays
     */
    private byte[][][] inputArrays;
    /**
     * target arrays
     */
    private byte[][][] targetArrays;

    public Cgs22ScpsiClient(Rpc clientRpc, Party senderParty, Cgs22ScpsiConfig config) {
        super(Cgs22ScpsiPtoDesc.getInstance(), clientRpc, senderParty, config);
        RbopprfConfig rbopprfConfig = config.getRbopprfConfig();
        rbopprfSender = RbopprfFactory.createSender(clientRpc, senderParty, rbopprfConfig);
        d = rbopprfConfig.getD();
        addSubPtos(rbopprfSender);
        psmReceiver = PsmFactory.createReceiver(clientRpc, senderParty, config.getPsmConfig());
        addSubPtos(psmReceiver);
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init batched OPPRF, where β_max = (1 + ε) * n_s, max_point_num = hash_num * n_c
        int maxBeta = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxServerElementSize);
        int maxPointNum = cuckooHashNum * maxClientElementSize;
        rbopprfSender.init(maxBeta, maxPointNum);
        // init private equality test, where maxL = σ + log_2(d * β_max) + log_2(max_point_num)
        int maxL = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2((long) d * maxBeta) + LongUtils.ceilLog2(maxPointNum);
        psmReceiver.init(maxL, d, maxBeta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareShareZ2Vector psi(Set<ByteBuffer> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // P2 receives the cuckoo hash bin keys
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();

        stopWatch.start();
        // β = (1 + ε) * n_s
        beta = CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverElementSize);
        // point_num = hash_num * n_c
        int pointNum = cuckooHashNum * clientElementSize;
        // l = σ + log_2(d * β) + log_2(point_num)
        int l = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2((long) d * beta) + LongUtils.ceilLog2(pointNum);
        // P2 inserts items into simple hash bin Table_2 with β bins
        handleCuckooHashKeyPayload(cuckooHashKeyPayload);
        stopWatch.stop();
        long binTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, binTime, "Client inserts simple hash");

        stopWatch.start();
        // The parties invoke a related batched OPPRF.
        // P2 inputs Table_2[1], . . . , Table_2[β] and receives T[1], ..., T[β]
        generateRbopprfInputs(l);
        rbopprfSender.opprf(l, inputArrays, targetArrays);
        inputArrays = null;
        targetArrays = null;
        stopWatch.stop();
        long opprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, opprfTime);

        stopWatch.start();
        // The parties invoke a private equality test with l = σ + log_2(point_num).
        // P1 inputs y_1^*, ..., y_β^* and outputs z0.
        SquareShareZ2Vector z1 = psmReceiver.psm(l, targetArray);
        targetArray = null;
        stopWatch.stop();
        long psmTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, psmTime);

        logPhaseInfo(PtoState.PTO_END);
        return z1;
    }

    private void handleCuckooHashKeyPayload(List<byte[]> cuckooHashKeyPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(cuckooHashKeyPayload.size() == cuckooHashNum);
        byte[][] cuckooHashKeys = cuckooHashKeyPayload.toArray(new byte[0][]);
        simpleHashBin = new RandomPadHashBin<>(envType, beta, clientElementSize, cuckooHashKeys);
        simpleHashBin.insertItems(clientElementArrayList);
    }

    private void generateRbopprfInputs(int l) {
        int byteL = CommonUtils.getByteLength(l);
        // P2 generates the input arrays
        inputArrays = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                ArrayList<HashBinEntry<ByteBuffer>> bin = new ArrayList<>(simpleHashBin.getBin(batchIndex));
                return bin.stream()
                    .map(entry -> {
                        byte[] itemBytes = entry.getItemByteArray();
                        return ByteBuffer.allocate(itemBytes.length + Integer.BYTES)
                            .put(itemBytes)
                            .putInt(entry.getHashIndex())
                            .array();
                    })
                    .toArray(byte[][]::new);
            })
            .toArray(byte[][][]::new);
        simpleHashBin = null;
        // P2 samples uniformly random and independent target values t_1, ..., t_β ∈ {0,1}^κ
        targetArray = IntStream.range(0, beta)
            .mapToObj(batchIndex -> BytesUtils.randomByteArray(byteL, l, secureRandom)).toArray(byte[][]::new);
        targetArrays = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                int batchPointNum = inputArrays[batchIndex].length;
                byte[][] copyTargetArray = new byte[batchPointNum][byteL];
                for (int i = 0; i < batchPointNum; i++) {
                    copyTargetArray[i] = BytesUtils.clone(targetArray[batchIndex]);
                }
                return copyTargetArray;
            })
            .toArray(byte[][][]::new);
    }
}
