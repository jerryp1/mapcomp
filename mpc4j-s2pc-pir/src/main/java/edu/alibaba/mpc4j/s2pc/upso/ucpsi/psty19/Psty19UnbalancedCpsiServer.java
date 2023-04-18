package edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfSender;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmSender;
import edu.alibaba.mpc4j.s2pc.opf.psm.cgs22.Cgs22OpprfPsmConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.AbstractUnbalancedCpsiServer;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UnbalancedCpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UnbalancedCpsiServerOutput;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.getBinNum;
import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.getHashNum;
import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UnbalancedCpsiPtoDesc.*;

/**
 * PSTY19 unbalanced circuit PSI server.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public class Psty19UnbalancedCpsiServer<T> extends AbstractUnbalancedCpsiServer<T> {
    /**
     * Batch OPPRF sender
     */
    private final BopprfSender bopprfSender;
    /**
     * Boolean Circuit party
     */
    private final BcParty bcParty;
    /**
     * hash bins
     */
    byte[][][] completeHashBins;

    byte[][][] targetArrays;
    public Psty19UnbalancedCpsiServer(Rpc serverRpc, Party clientParty, UnbalancedCpsiConfig config) {
        super(Psty19UnbalancedCpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        bopprfSender = BopprfFactory.createSender(serverRpc, clientParty, ((Psty19UnbalancedCpsiConfig) config).getBopprfConfig());
        bcParty = BcFactory.createSender(serverRpc, clientParty, ((Psty19UnbalancedCpsiConfig) config).getBcConfig());
//        addSubPtos(bopprfSender);
//        addSubPtos(bcParty);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // empty
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public UnbalancedCpsiServerOutput<T> psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // hash
        stopWatch.start();
        List<byte[]> hashKeys = senderComputingHashing();
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, hashKeys));
        stopWatch.stop();
        long senderHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, senderHashTime, "Sender hash elements");

        // batch opprf
        stopWatch.start();
        senderBopprf();
        stopWatch.stop();
        long senderBopprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, senderBopprfTime, "Sender batch opprf");

        // membership test
        stopWatch.start();
        membershipTest();
        stopWatch.stop();
        long membershipTestTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, membershipTestTime, "Sender membership test");
        return null;
    }

    private void membershipTest() throws MpcAbortException {
        PsmSender sender = PsmFactory.createSender(
            rpc, otherParty(), new Cgs22OpprfPsmConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        );
        sender.init(CommonConstants.BLOCK_BIT_LENGTH, targetArrays[0].length, targetArrays.length);
        SquareShareZ2Vector result = sender.psm(CommonConstants.BLOCK_BIT_LENGTH, targetArrays);
    }

    private void senderBopprf() throws MpcAbortException {
        int batchSize = completeHashBins.length;
        int pointNum = Arrays.stream(completeHashBins)
            .mapToInt(inputArray -> inputArray.length)
            .sum();
        bopprfSender.init(batchSize, pointNum);
        int byteL = CommonUtils.getByteLength(CommonConstants.BLOCK_BIT_LENGTH);
        // random target values
        targetArrays = new byte[batchSize][][];
        for (int batchIndex = 0; batchIndex < batchSize; batchIndex++) {
            int batchPointNum = completeHashBins[batchIndex].length;
            targetArrays[batchIndex] = new byte[batchPointNum][];

            for (int pointIndex = 0; pointIndex < batchPointNum; pointIndex++) {
                targetArrays[batchIndex][pointIndex] = BytesUtils.randomByteArray(byteL, CommonConstants.BLOCK_BIT_LENGTH, secureRandom);
            }
        }
        bopprfSender.opprf(CommonConstants.BLOCK_BIT_LENGTH, completeHashBins, targetArrays);
        for (int i = 0; i < targetArrays.length; i++) {
            System.out.println(Arrays.deepToString(targetArrays[i]));
        }
    }

    private List<byte[]> senderComputingHashing() {
        CuckooHashBinType hashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        int hashNum = getHashNum(hashBinType);
        int binNum = getBinNum(hashBinType, clientElementSize);
        byte[][] hashKeys = CommonUtils.generateRandomKeys(hashNum, secureRandom);
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(envType, binNum, serverElementSize, hashKeys);
        completeHash.insertItems(serverElementArrayList);
        completeHashBins = new byte[binNum][][];
        for (int i = 0; i < binNum; i++) {
            List<HashBinEntry<ByteBuffer>> binItems = new ArrayList<>(completeHash.getBin(i));
            Set<ByteBuffer> binItemsSet = new HashSet<>();
            for (int j = 0; j < binItems.size(); j++) {
                binItemsSet.add(binItems.get(j).getItem());
            }
            while (binItemsSet.size() < binItems.size()) {
                byte[] paddingBytes = new byte[CommonConstants.STATS_BYTE_LENGTH];
                secureRandom.nextBytes(paddingBytes);
                binItemsSet.add(ByteBuffer.wrap(paddingBytes));
            }
            int batchPointNum = binItemsSet.size();
            completeHashBins[i] = new byte[batchPointNum][];
            List<ByteBuffer> binItemsArray = new ArrayList<>(binItemsSet);
            for (int j = 0; j < batchPointNum; j++) {
                completeHashBins[i][j] = binItemsArray.get(j).array();
            }
        }
        return Arrays.stream(hashKeys).collect(Collectors.toCollection(ArrayList::new));
    }
}
