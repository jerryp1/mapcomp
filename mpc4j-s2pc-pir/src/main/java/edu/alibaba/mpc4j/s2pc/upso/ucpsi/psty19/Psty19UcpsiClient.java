package edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19;

import com.google.common.primitives.Bytes;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.PeqtParty;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.AbstractUcpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiClientOutput;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.UbopprfFactory;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.UbopprfReceiver;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
     * hash num
     */
    private int hashNum;
    /**
     * bin num
     */
    private int binNum;
    /**
     * cuckoo hash bin type
     */
    private CuckooHashBinType hashBinType;
    /**
     * cuckoo hash bin
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;

    public Psty19UcpsiClient(Rpc clientRpc, Party serverParty, UcpsiConfig config) {
        super(Psty19UcpsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        Psty19UcpsiConfig psty19UcpsiConfig = (Psty19UcpsiConfig) config;
        ubopprfReceiver = UbopprfFactory.createReceiver(clientRpc, serverParty, psty19UcpsiConfig.getUbopprfConfig());
        peqtParty = PeqtFactory.createReceiver(clientRpc, serverParty, psty19UcpsiConfig.getPeqtConfig());
        addSubPtos(ubopprfReceiver);
        addSubPtos(peqtParty);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        hashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        hashNum = getHashNum(hashBinType);
        binNum = getBinNum(hashBinType, maxClientElementSize);
        peqtParty.init(CommonConstants.BLOCK_BIT_LENGTH, binNum);
        ubopprfReceiver.init(binNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public UcpsiClientOutput psi(Set<ByteBuffer> clientElementSet, int serverElementSize)
        throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive hash keys
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        byte[][] hashKeys = rpc.receive(cuckooHashKeyHeader).getPayload().toArray(new byte[0][]);

        // hash
        stopWatch.start();
        byte[][] hashBinItems = cuckooHash(hashKeys);
        stopWatch.stop();
        long receiverCuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, receiverCuckooHashTime, "Receiver hash elements");

        // batch opprf
        stopWatch.start();
        byte[][] oprfOutputs = ubopprfReceiver.opprf(
            CommonConstants.BLOCK_BIT_LENGTH, hashBinItems, hashNum * serverElementSize
        );
        stopWatch.stop();
        long bopprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, bopprfTime, "Receiver batch opprf");

        // membership test
        stopWatch.start();
        SquareShareZ2Vector z2Vector = peqtParty.peqt(CommonConstants.BLOCK_BIT_LENGTH, oprfOutputs);
        ArrayList<ByteBuffer> arrayList = IntStream.range(0, binNum)
            .mapToObj(i -> cuckooHashBin.getHashBinEntry(i).getItem())
            .collect(Collectors.toCollection(() -> new ArrayList<>(binNum)));
        cuckooHashBin = null;
        UcpsiClientOutput output = new UcpsiClientOutput(arrayList, z2Vector);
        stopWatch.stop();
        long membershipTestTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, membershipTestTime, "Receiver membership test");

        return output;
    }

    private byte[][] cuckooHash(byte[][] hashKeys) throws MpcAbortException {
        cuckooHashBin = CuckooHashBinFactory.createCuckooHashBin(envType, hashBinType, clientElementSize, hashKeys);
        cuckooHashBin.insertItems(clientElementArrayList);
        if (cuckooHashBin.stashSize() != 0) {
            MpcAbortPreconditions.checkArgument(false);
        }
        cuckooHashBin.insertPaddingItems(botElementByteBuffer);
        return IntStream.range(0, cuckooHashBin.binNum())
            .mapToObj(i -> Bytes.concat(
                cuckooHashBin.getHashBinEntry(i).getItemByteArray(),
                IntUtils.intToByteArray(cuckooHashBin.getHashBinEntry(i).getHashIndex()))
            )
            .toArray(byte[][]::new);
    }
}
