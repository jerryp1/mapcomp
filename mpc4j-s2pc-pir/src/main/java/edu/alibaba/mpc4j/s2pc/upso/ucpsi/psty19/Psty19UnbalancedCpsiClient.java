package edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmReceiver;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmSender;
import edu.alibaba.mpc4j.s2pc.opf.psm.cgs22.Cgs22OpprfPsmConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.AbstractUnbalancedCpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UnbalancedCpsiConfig;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.*;
import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UnbalancedCpsiPtoDesc.*;

/**
 * PSTY19 unbalanced circuit PSI client.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public class Psty19UnbalancedCpsiClient<T> extends AbstractUnbalancedCpsiClient<T> {
    /**
     * Batch OPPRF receiver
     */
    private final BopprfReceiver bopprfReceiver;
    /**
     * Boolean Circuit party
     */
    private final BcParty bcParty;
    byte[][] cuckooHashArray;
    byte[][] stashArray;

    public Psty19UnbalancedCpsiClient(Rpc clientRpc, Party serverParty, UnbalancedCpsiConfig config) {
        super(Psty19UnbalancedCpsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        Psty19UnbalancedCpsiConfig cpsiConfig = (Psty19UnbalancedCpsiConfig) config;
        bopprfReceiver = BopprfFactory.createReceiver(clientRpc, serverParty, cpsiConfig.getBopprfConfig());
        bcParty = BcFactory.createSender(clientRpc, serverParty, cpsiConfig.getBcConfig());
//        addSubPtos(bopprfReceiver);
//        addSubPtos(bcParty);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
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
    public SquareShareZ2Vector psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException {
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
        MpcAbortPreconditions.checkArgument(cuckooHash(hashKeys));
        stopWatch.stop();
        long receiverCuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, receiverCuckooHashTime, "Receiver hash elements");

        // batch opprf
        stopWatch.start();
        byte[][] oprfOutputs = receiverBopprf();
        stopWatch.stop();
        long bopprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, bopprfTime, "Receiver batch opprf");

        // membership test
        stopWatch.start();
        SquareShareZ2Vector z2Vector = membershipTest(oprfOutputs);
        stopWatch.stop();
        long membershipTestTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, membershipTestTime, "Receiver membership test");

        return z2Vector;
    }

    private SquareShareZ2Vector membershipTest(byte[][] oprfOutputs) throws MpcAbortException {
        PsmReceiver receiver = PsmFactory.createReceiver(
            rpc, otherParty(), new Cgs22OpprfPsmConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        );
        receiver.init(CommonConstants.BLOCK_BIT_LENGTH, 2 * serverElementSize, cuckooHashArray.length);
        return receiver.psm(CommonConstants.BLOCK_BIT_LENGTH, oprfOutputs);
    }

    private byte[][] receiverBopprf() throws MpcAbortException {
        int batchSize = cuckooHashArray.length;
        bopprfReceiver.init(batchSize, 3 * serverElementSize);
        byte[][] oprfOutputs = bopprfReceiver.opprf(CommonConstants.BLOCK_BIT_LENGTH, cuckooHashArray, 3 * serverElementSize);
        for (int i = 0; i < oprfOutputs.length; i++) {
            System.out.println(Arrays.toString(oprfOutputs[i]));
        }
        return oprfOutputs;
    }

    private boolean cuckooHash(byte[][] hashKeys) {
        CuckooHashBinType hashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        CuckooHashBin<ByteBuffer> cuckooHashBin = CuckooHashBinFactory.createCuckooHashBin(
            envType, hashBinType, clientElementSize, hashKeys
        );
        cuckooHashBin.insertItems(clientElementArrayList);
        if (cuckooHashBin.stashSize() != 0) {
            return false;
        }
        cuckooHashBin.insertPaddingItems(secureRandom);
        cuckooHashArray = new byte[cuckooHashBin.binNum()][];
        for (int i = 0; i < cuckooHashBin.binNum(); i++) {
            cuckooHashArray[i] = cuckooHashBin.getHashBinEntry(i).getItemByteArray();
        }
        return true;
    }
}
