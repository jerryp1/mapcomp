package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.cgs22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.RbopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.RbopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.RbopprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmSender;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.AbstractCcpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiClientOutput;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.cgs22.Cgs22CcpsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CGS22 client-payload circuit PSI client.
 *
 * @author Weiran Liu
 * @date 2023/4/19
 */
public class Cgs22CcpsiClient extends AbstractCcpsiClient {
    /**
     * related batched OPPRF receiver
     */
    private final RbopprfReceiver rbopprfReceiver;
    /**
     * private set membership sender
     */
    private final PsmSender psmSender;
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
    private final int hashNum;
    /**
     * cuckoo hash bin
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;

    public Cgs22CcpsiClient(Rpc serverRpc, Party clientParty, Cgs22CcpsiConfig config) {
        super(Cgs22CcpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        RbopprfConfig rbopprfConfig = config.getRbopprfConfig();
        rbopprfReceiver = RbopprfFactory.createReceiver(serverRpc, clientParty, rbopprfConfig);
        addSubPtos(rbopprfReceiver);
        d = rbopprfConfig.getD();
        psmSender = PsmFactory.createSender(serverRpc, clientParty, config.getPsmConfig());
        addSubPtos(psmSender);
        cuckooHashBinType = config.getCuckooHashBinType();
        hashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init related batched OPPRF, where β_max = (1 + ε) * n_c, max_point_num = hash_num * n_s
        int maxBeta = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize);
        int maxPointNum = hashNum * maxServerElementSize;
        rbopprfReceiver.init(maxBeta, maxPointNum);
        // init private set membership, where maxL = σ + log_2(d * β_max) + log_2(max_point_num)
        int maxL = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2((long) d * maxBeta) + LongUtils.ceilLog2(maxPointNum);
        psmSender.init(maxL, d, maxBeta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public CcpsiClientOutput psi(Set<ByteBuffer> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // β = (1 + ε) * n_c
        int beta = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
        // point_num = hash_num * n_s
        int pointNum = hashNum * serverElementSize;
        // l = σ + log_2(d * β) + log_2(point_num)
        int l = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2((long) d * beta) + LongUtils.ceilLog2(pointNum);
        // P2 inserts items into no-stash cuckoo hash bin Table_1 with β bins.
        List<byte[]> cuckooHashKeyPayload = generateCuckooHashKeyPayload();
        // P2 sends the cuckoo hash bin keys
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long binTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, binTime, "Server inserts cuckoo hash");

        stopWatch.start();
        // The parties invoke a related batched OPPRF.
        // P2 inputs Table_1[1], . . . , Table_1[β] and receives y_1^*, ..., y_β^*
        byte[][] inputArray = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                HashBinEntry<ByteBuffer> item = cuckooHashBin.getHashBinEntry(batchIndex);
                byte[] itemBytes = cuckooHashBin.getHashBinEntry(batchIndex).getItemByteArray();
                return ByteBuffer.allocate(itemBytes.length + Integer.BYTES)
                    .put(itemBytes)
                    .putInt(item.getHashIndex())
                    .array();
            })
            .toArray(byte[][]::new);
        byte[][][] targetArrays = rbopprfReceiver.opprf(l, inputArray, pointNum);
        stopWatch.stop();
        long opprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, opprfTime);

        stopWatch.start();
        // The parties invoke a private set membership with l = σ + log_2(d * β) + log_2(point_num).
        // P2 inputs y_1^*, ..., y_β^* and outputs z1.
        SquareShareZ2Vector z1 = psmSender.psm(l, targetArrays);
        // create the table
        ByteBuffer[] table = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                HashBinEntry<ByteBuffer> item = cuckooHashBin.getHashBinEntry(batchIndex);
                if (item.getHashIndex() == HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                    return ByteBuffer.wrap(new byte[0]);
                } else {
                    return item.getItem();
                }
            })
            .toArray(ByteBuffer[]::new);
        CcpsiClientOutput clientOutput = new CcpsiClientOutput(table, z1);
        cuckooHashBin = null;
        stopWatch.stop();
        long psmTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, psmTime);

        logPhaseInfo(PtoState.PTO_END);
        return clientOutput;
    }

    private List<byte[]> generateCuckooHashKeyPayload() {
        cuckooHashBin = CuckooHashBinFactory.createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, clientElementSize, clientElementArrayList, secureRandom
        );
        // pad random elements into the cuckoo hash
        cuckooHashBin.insertPaddingItems(secureRandom);
        return Arrays.stream(cuckooHashBin.getHashKeys()).collect(Collectors.toList());
    }
}
