package edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.NoStashCuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.PeqtParty;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.AbstractUcpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiClientOutput;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.UbopprfFactory;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.UbopprfReceiver;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.*;
import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiPtoDesc.*;

/**
 * PSTY19 unbalanced circuit PSI client.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public class Psty19UcpsiClient extends AbstractUcpsiClient {
    /**
     * unbalanced batch OPPRF receiver
     */
    private final UbopprfReceiver ubopprfReceiver;
    /**
     * peqt receiver
     */
    private final PeqtParty peqtParty;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType hashBinType;
    /**
     * hash num
     */
    private final int hashNum;
    /**
     * bin num
     */
    private int beta;
    /**
     * l
     */
    private int l;
    /**
     * cuckoo hash bin
     */
    private NoStashCuckooHashBin<ByteBuffer> cuckooHashBin;

    public Psty19UcpsiClient(Rpc clientRpc, Party serverParty, UcpsiConfig config) {
        super(Psty19UcpsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        Psty19UcpsiConfig psty19UcpsiConfig = (Psty19UcpsiConfig) config;
        ubopprfReceiver = UbopprfFactory.createReceiver(clientRpc, serverParty, psty19UcpsiConfig.getUbopprfConfig());
        addSubPtos(ubopprfReceiver);
        peqtParty = PeqtFactory.createReceiver(clientRpc, serverParty, psty19UcpsiConfig.getPeqtConfig());
        addSubPtos(peqtParty);
        hashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        hashNum = getHashNum(hashBinType);
    }

    @Override
    public void init(int maxClientElementSize, int serverElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, serverElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // β = (1 + ε) * n_c
        beta = getBinNum(hashBinType, maxClientElementSize);
        // point_num = hash_num * n_s
        int pointNum = hashNum * serverElementSize;
        // l = σ + log_2(β) + log_2(point_num)
        l = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(beta) + LongUtils.ceilLog2(pointNum);
        peqtParty.init(l, beta);
        ubopprfReceiver.init(l, beta, pointNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public UcpsiClientOutput psi(Set<ByteBuffer> clientElementSet) throws MpcAbortException {
        setPtoInput(clientElementSet);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive hash keys
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();

        stopWatch.start();
        handleCuckooHashKeyPayload(cuckooHashKeyPayload);
        stopWatch.stop();
        long receiverCuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, receiverCuckooHashTime, "Receiver hash elements");

        stopWatch.start();
        // unbalanced batch opprf
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
        byte[][] targetArray = ubopprfReceiver.opprf(inputArray);
        stopWatch.stop();
        long bopprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, bopprfTime, "Receiver batch opprf");

        stopWatch.start();
        // private equality test
        SquareShareZ2Vector z1 = peqtParty.peqt(l, targetArray);
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
        cuckooHashBin = null;
        UcpsiClientOutput clientOutput = new UcpsiClientOutput(table, z1);
        stopWatch.stop();
        long membershipTestTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, membershipTestTime, "Receiver PEQT");

        return clientOutput;
    }

    private void handleCuckooHashKeyPayload(List<byte[]> cuckooHashKeyPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(cuckooHashKeyPayload.size() == hashNum);
        byte[][] hashKeys = cuckooHashKeyPayload.toArray(new byte[0][]);
        cuckooHashBin = CuckooHashBinFactory.createNoStashCuckooHashBin(envType, hashBinType, clientElementSize, hashKeys);
        cuckooHashBin.insertItems(clientElementArrayList);
        cuckooHashBin.insertPaddingItems(secureRandom);
    }
}
