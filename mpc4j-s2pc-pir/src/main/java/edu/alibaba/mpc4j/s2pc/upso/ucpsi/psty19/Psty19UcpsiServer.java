package edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.PeqtParty;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.AbstractUcpsiServer;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.UbopprfFactory;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.UbopprfSender;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.getBinNum;
import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.getHashNum;
import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiPtoDesc.*;

/**
 * PSTY19 unbalanced circuit PSI server.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public class Psty19UcpsiServer extends AbstractUcpsiServer {
    /**
     * unbalanced batch OPPRF sender
     */
    private final UbopprfSender ubopprfSender;
    /**
     * peqt sender
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
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * input arrays
     */
    private byte[][][] inputArrays;
    /**
     * target array
     */
    private byte[][] targetArray;
    /**
     * target arrays
     */
    private byte[][][] targetArrays;

    public Psty19UcpsiServer(Rpc serverRpc, Party clientParty, UcpsiConfig config) {
        super(Psty19UcpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        Psty19UcpsiConfig psty19UcpsiConfig = (Psty19UcpsiConfig) config;
        ubopprfSender = UbopprfFactory.createSender(serverRpc, clientParty, psty19UcpsiConfig.getUbopprfConfig());
        addSubPtos(ubopprfSender);
        peqtParty = PeqtFactory.createSender(serverRpc, clientParty, psty19UcpsiConfig.getPeqtConfig());
        addSubPtos(peqtParty);
        hashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        hashNum = getHashNum(hashBinType);
    }

    @Override
    public void init(Set<ByteBuffer> serverElementSet, int maxClientElementSize) throws MpcAbortException {
        setInitInput(serverElementSet, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // β = (1 + ε) * n_c
        beta = getBinNum(hashBinType, maxClientElementSize);
        // point_num = hash_num * n_s
        int pointNum = hashNum * serverElementSize;
        // l = σ + log_2(β) + log_2(point_num)
        l = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(beta) + LongUtils.ceilLog2(pointNum);
        // simple hash
        hashKeys = CommonUtils.generateRandomKeys(hashNum, secureRandom);
        generateBopprfInputs(l);
        stopWatch.stop();
        long senderHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, senderHashTime, "Sender hash elements");

        stopWatch.start();
        // initialize unbalanced batch opprf
        ubopprfSender.init(l, inputArrays, targetArrays);
        inputArrays = null;
        targetArrays = null;
        stopWatch.stop();
        long senderBopprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, senderBopprfTime, "Sender init unbalanced batch opprf");

        stopWatch.start();
        // initialize peqt
        peqtParty.init(l, beta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareShareZ2Vector psi() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        // send hash keys
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));

        stopWatch.start();
        // unbalanced batch opprf
        ubopprfSender.opprf();
        stopWatch.stop();
        long senderBopprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, senderBopprfTime, "Sender execute unbalanced batch opprf");

        // membership test
        stopWatch.start();
        SquareShareZ2Vector z2Vector = peqtParty.peqt(l, targetArray);
        stopWatch.stop();
        long membershipTestTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, membershipTestTime, "Sender membership test");

        logPhaseInfo(PtoState.PTO_END);
        return z2Vector;
    }

    private void generateBopprfInputs(int l) {
        int byteL = CommonUtils.getByteLength(l);
        RandomPadHashBin<ByteBuffer> simpleHashBin = new RandomPadHashBin<>(envType, beta, serverElementSize, hashKeys);
        simpleHashBin.insertItems(serverElementArrayList);
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
        // P1 samples uniformly random and independent target values t_1, ..., t_β ∈ {0,1}^κ
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