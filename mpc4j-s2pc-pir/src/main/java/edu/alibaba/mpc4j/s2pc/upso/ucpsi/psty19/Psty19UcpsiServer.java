package edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19;

import com.google.common.primitives.Bytes;
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
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
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
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * target arrays
     */
    private byte[][] targetArrays;

    public Psty19UcpsiServer(Rpc serverRpc, Party clientParty, UcpsiConfig config) {
        super(Psty19UcpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        Psty19UcpsiConfig psty19UcpsiConfig = (Psty19UcpsiConfig) config;
        ubopprfSender = UbopprfFactory.createSender(serverRpc, clientParty, psty19UcpsiConfig.getUbopprfConfig());
        peqtParty = PeqtFactory.createSender(serverRpc, clientParty, psty19UcpsiConfig.getPeqtConfig());
        addSubPtos(ubopprfSender);
        addSubPtos(peqtParty);
    }

    @Override
    public void init(Set<ByteBuffer> serverElementSet, int maxClientElementSize) throws MpcAbortException {
        setInitInput(serverElementSet, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        // simple hash
        stopWatch.start();
        CuckooHashBinType hashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        int hashNum = getHashNum(hashBinType);
        int binNum = getBinNum(hashBinType, this.maxClientElementSize);
        hashKeys = CommonUtils.generateRandomKeys(hashNum, secureRandom);
        byte[][][] ubopprfInputArrays = senderComputingHashing(binNum);
        stopWatch.stop();
        long senderHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, senderHashTime, "Sender hash elements");

        // initialize unbalanced batch opprf
        stopWatch.start();
        byte[][][] targetInputArrays = computeTargetInputArrays(ubopprfInputArrays, binNum);
        ubopprfSender.init(CommonConstants.BLOCK_BIT_LENGTH, ubopprfInputArrays, targetInputArrays);
        stopWatch.stop();
        long senderBopprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, senderBopprfTime, "Sender init unbalanced batch opprf");

        // initialize peqt
        stopWatch.start();
        peqtParty.init(CommonConstants.BLOCK_BIT_LENGTH, binNum);
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
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(
            cuckooHashKeyHeader, Arrays.stream(hashKeys).collect(Collectors.toCollection(ArrayList::new)))
        );

        // unbalanced batch opprf
        stopWatch.start();
        ubopprfSender.opprf();
        stopWatch.stop();
        long senderBopprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, senderBopprfTime, "Sender execute unbalanced batch opprf");

        // membership test
        stopWatch.start();
        SquareShareZ2Vector z2Vector = peqtParty.peqt(CommonConstants.BLOCK_BIT_LENGTH, targetArrays);
        stopWatch.stop();
        long membershipTestTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, membershipTestTime, "Sender membership test");

        logPhaseInfo(PtoState.PTO_END);
        return z2Vector;
    }

    /**
     * sender init target input arrays.
     *
     * @param ubopprfInputArrays input arrays.
     * @param binNum             bin num.
     * @return target input arrays.
     */
    private byte[][][] computeTargetInputArrays(byte[][][] ubopprfInputArrays, int binNum) {
        int byteL = CommonUtils.getByteLength(CommonConstants.BLOCK_BIT_LENGTH);
        // random target values
        byte[][][] targetInputArrays = new byte[binNum][][];
        targetArrays = new byte[binNum][];
        for (int binIndex = 0; binIndex < binNum; binIndex++) {
            int batchPointNum = ubopprfInputArrays[binIndex].length;
            targetInputArrays[binIndex] = new byte[batchPointNum][];
            byte[] target = BytesUtils.randomByteArray(byteL, CommonConstants.BLOCK_BIT_LENGTH, secureRandom);
            for (int pointIndex = 0; pointIndex < batchPointNum; pointIndex++) {
                targetInputArrays[binIndex][pointIndex] = BytesUtils.clone(target);
            }
            targetArrays[binIndex] = BytesUtils.clone(target);
        }
        return targetInputArrays;
    }

    /**
     * sender compute simple hash.
     *
     * @param binNum bin num.
     * @return hash bin items.
     */
    private byte[][][] senderComputingHashing(int binNum) {
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(envType, binNum, serverElementSize, hashKeys);
        completeHash.insertItems(serverElementArrayList);
        byte[][][] completeHashBins = new byte[binNum][][];
        for (int i = 0; i < binNum; i++) {
            List<HashBinEntry<ByteBuffer>> binItems = new ArrayList<>(completeHash.getBin(i));
            int batchPointNum = binItems.size();
            completeHashBins[i] = new byte[batchPointNum][];
            for (int j = 0; j < batchPointNum; j++) {
                completeHashBins[i][j] = Bytes.concat(
                    binItems.get(j).getItemByteArray(), IntUtils.intToByteArray(binItems.get(j).getHashIndex())
                );
            }
        }
        return completeHashBins;
    }
}
