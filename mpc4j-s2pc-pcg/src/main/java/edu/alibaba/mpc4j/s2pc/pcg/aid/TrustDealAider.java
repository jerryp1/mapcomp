package edu.alibaba.mpc4j.s2pc.pcg.aid;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyPto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pcg.aid.TrustDealPtoDesc.AidPtoStep;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * trust deal aider.
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
public class TrustDealAider extends AbstractThreePartyPto {
    /**
     * encoded task ID map
     */
    private final Map<Long, Object> encodeTaskIdMap;

    public TrustDealAider(Rpc aiderRpc, Party leftParty, Party rightParty) {
        super(TrustDealPtoDesc.getInstance(), aiderRpc, leftParty, rightParty, new TrustDealConfig.Builder().build());
        encodeTaskIdMap = new HashMap<>(1);
    }

    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    public void init() throws MpcAbortException {
        initState();
        logPhaseInfo(PtoState.INIT_BEGIN);
        // empty
        logPhaseInfo(PtoState.INIT_END);
    }

    /**
     * Executes the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    public void aid() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        boolean run = true;
        while (run) {
            // receive any packet
            DataPacket dataPacket = rpc.receiveAny();
            DataPacketHeader header = dataPacket.getHeader();
            // verify protocol ID
            MpcAbortPreconditions.checkArgument(header.getPtoId() == getPtoDesc().getPtoId());
            AidPtoStep aidPtoStep = AidPtoStep.values()[header.getStepId()];
            switch (aidPtoStep) {
                case INIT_QUERY:
                    stopWatch.start();
                    initResponse(dataPacket);
                    stopWatch.stop();
                    long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                    stopWatch.reset();
                    logStepInfo(PtoState.PTO_STEP, 1, 3, initTime);
                    break;
                case REQUEST_QUERY:
                    stopWatch.start();
                    requestResponse(dataPacket);
                    stopWatch.stop();
                    long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                    stopWatch.reset();
                    logStepInfo(PtoState.PTO_STEP, 2, 3, responseTime);
                    break;
                case DESTROY_QUERY:
                    stopWatch.start();
                    destroyResponse(dataPacket);
                    stopWatch.stop();
                    long destroyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                    stopWatch.reset();
                    logStepInfo(PtoState.PTO_STEP, 3, 3, destroyTime);
                    break;
                default:
                    throw new MpcAbortException("Invalid " + AidPtoStep.class.getSimpleName() + ": " + aidPtoStep);
            }
            run = (encodeTaskIdMap.size() != 0);
        }
        logPhaseInfo(PtoState.PTO_END);
    }

    private void initResponse(DataPacket thisInitDataPacket) throws MpcAbortException {
        DataPacketHeader thisInitHeader = thisInitDataPacket.getHeader();
        long initEncodeTaskId = thisInitHeader.getEncodeTaskId();
        int thisId = thisInitHeader.getSenderId();
        int thatId = (thisId == leftParty().getPartyId() ? rightParty().getPartyId() : leftParty().getPartyId());
        long initExtraInfo = thisInitHeader.getExtraInfo();
        // check no-exist of encode task ID
        MpcAbortPreconditions.checkArgument(!encodeTaskIdMap.containsKey(initEncodeTaskId));
        // receive init query from that party
        DataPacketHeader thatInitHeader = new DataPacketHeader(
            initEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.INIT_QUERY.ordinal(), initExtraInfo,
            thatId, ownParty().getPartyId()
        );
        DataPacket thatInitDataPacket = rpc.receive(thatInitHeader);
        // read the config
        List<byte[]> thisInitPayload = thisInitDataPacket.getPayload();
        List<byte[]> thatInitPayload = thatInitDataPacket.getPayload();
        MpcAbortPreconditions.checkArgument(thisInitPayload.size() >= 1);
        MpcAbortPreconditions.checkArgument(thatInitPayload.size() >= 1);
        int thisTypeIndex = IntUtils.byteArrayToInt(thisInitPayload.get(0));
        int thatTypeIndex = IntUtils.byteArrayToInt(thatInitPayload.get(0));
        MpcAbortPreconditions.checkArgument(thisTypeIndex == thatTypeIndex);
        TrustDealType trustDealType = TrustDealType.values()[thatTypeIndex];
        switch (trustDealType) {
            case Z2_TRIPLE:
                // Z2 triple, no config
                encodeTaskIdMap.put(initEncodeTaskId, new Object());
                break;
            case ZL_TRIPLE:
                // Zl triple, read l
                MpcAbortPreconditions.checkArgument(thisInitPayload.size() == 2);
                MpcAbortPreconditions.checkArgument(thatInitPayload.size() == 2);
                int thisL = IntUtils.byteArrayToInt(thisInitPayload.get(1));
                int thatL = IntUtils.byteArrayToInt(thatInitPayload.get(1));
                MpcAbortPreconditions.checkArgument(thisL == thatL);
                encodeTaskIdMap.put(initEncodeTaskId, ZlFactory.createInstance(envType, thisL));
                break;
            default:
                throw new MpcAbortException("Invalid " + TrustDealType.class.getSimpleName() + ": " + trustDealType.name());
        }
        // response to the left party
        DataPacketHeader leftResponseHeader = new DataPacketHeader(
            initEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.INIT_RESPONSE.ordinal(), initExtraInfo,
            ownParty().getPartyId(), leftParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(leftResponseHeader, new LinkedList<>()));
        // response to the right party
        DataPacketHeader rightResponseHeader = new DataPacketHeader(
            initEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.INIT_RESPONSE.ordinal(), initExtraInfo,
            ownParty().getPartyId(), rightParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(rightResponseHeader, new LinkedList<>()));
    }

    private void requestResponse(DataPacket thisRequestDataPacket) throws MpcAbortException {
        DataPacketHeader thisRequestHeader = thisRequestDataPacket.getHeader();
        long requestEncodeTaskId = thisRequestHeader.getEncodeTaskId();
        int thisId = thisRequestHeader.getSenderId();
        int thatId = (thisId == leftParty().getPartyId() ? rightParty().getPartyId() : leftParty().getPartyId());
        long requestExtraInfo = thisRequestHeader.getExtraInfo();
        // check encode task ID
        MpcAbortPreconditions.checkArgument(encodeTaskIdMap.containsKey(requestEncodeTaskId));
        // receive request query from that party
        DataPacketHeader thatRequestHeader = new DataPacketHeader(
            requestEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.REQUEST_QUERY.ordinal(), requestExtraInfo,
            thatId, ownParty().getPartyId()
        );
        DataPacket thatRequestDataPacket = rpc.receive(thatRequestHeader);
        // parse and check type
        List<byte[]> thisRequestPayload = thisRequestDataPacket.getPayload();
        List<byte[]> thatRequestPayload = thatRequestDataPacket.getPayload();
        MpcAbortPreconditions.checkArgument(thisRequestPayload.size() >= 1);
        MpcAbortPreconditions.checkArgument(thatRequestPayload.size() >= 1);
        int thisTypeIndex = IntUtils.byteArrayToInt(thisRequestPayload.get(0));
        int thatTypeIndex = IntUtils.byteArrayToInt(thatRequestPayload.get(0));
        MpcAbortPreconditions.checkArgument(thisTypeIndex == thatTypeIndex);
        TrustDealType trustDealType = TrustDealType.values()[thatTypeIndex];
        //noinspection SwitchStatementWithTooFewBranches
        switch (trustDealType) {
            case Z2_TRIPLE:
                z2TripleResponse(requestEncodeTaskId, requestExtraInfo, thisRequestPayload, thatRequestPayload);
                break;
            default:
                throw new MpcAbortException("Invalid " + TrustDealType.class.getSimpleName() + ": " + trustDealType.name());
        }
    }

    private void z2TripleResponse(long requestEncodeTaskId, long requestExtraInfo,
                                  List<byte[]> thisRequestPayload, List<byte[]> thatRequestPayload)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(thisRequestPayload.size() == 2);
        MpcAbortPreconditions.checkArgument(thatRequestPayload.size() == 2);
        int thisNum = IntUtils.byteArrayToInt(thisRequestPayload.get(1));
        int thatNum = IntUtils.byteArrayToInt(thatRequestPayload.get(1));
        MpcAbortPreconditions.checkArgument(thisNum == thatNum);
        // generate Z2 triple
        BitVector aBitVector = BitVectorFactory.createRandom(thisNum, secureRandom);
        BitVector bBitVector = BitVectorFactory.createRandom(thisNum, secureRandom);
        BitVector cBitVector = aBitVector.and(bBitVector);
        BitVector a0BitVector = BitVectorFactory.createRandom(thisNum, secureRandom);
        BitVector a1BitVector = aBitVector.xor(a0BitVector);
        BitVector b0BitVector = BitVectorFactory.createRandom(thisNum, secureRandom);
        BitVector b1BitVector = bBitVector.xor(b0BitVector);
        BitVector c0BitVector = BitVectorFactory.createRandom(thisNum, secureRandom);
        BitVector c1BitVector = cBitVector.xor(c0BitVector);
        // response to the left party
        List<byte[]> leftResponsePayload = new LinkedList<>();
        leftResponsePayload.add(a0BitVector.getBytes());
        leftResponsePayload.add(b0BitVector.getBytes());
        leftResponsePayload.add(c0BitVector.getBytes());
        DataPacketHeader leftResponseHeader = new DataPacketHeader(
            requestEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.REQUEST_RESPONSE.ordinal(), requestExtraInfo,
            ownParty().getPartyId(), leftParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(leftResponseHeader, leftResponsePayload));
        // response to the right party
        List<byte[]> rightResponsePayload = new LinkedList<>();
        rightResponsePayload.add(a1BitVector.getBytes());
        rightResponsePayload.add(b1BitVector.getBytes());
        rightResponsePayload.add(c1BitVector.getBytes());
        DataPacketHeader rightResponseHeader = new DataPacketHeader(
            requestEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.REQUEST_RESPONSE.ordinal(), requestExtraInfo,
            ownParty().getPartyId(), rightParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(rightResponseHeader, rightResponsePayload));
    }

    private void destroyResponse(DataPacket thisDataPacket) throws MpcAbortException {
        DataPacketHeader thisHeader = thisDataPacket.getHeader();
        long destroyEncodeTaskId = thisHeader.getEncodeTaskId();
        int thisId = thisHeader.getSenderId();
        int thatId = (thisId == leftParty().getPartyId() ? rightParty().getPartyId() : leftParty().getPartyId());
        long destroyExtraInfo = thisHeader.getExtraInfo();
        // check encode task ID
        MpcAbortPreconditions.checkArgument(encodeTaskIdMap.containsKey(destroyEncodeTaskId));
        // receive destroy query from that party
        DataPacketHeader thatHeader = new DataPacketHeader(
            destroyEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.DESTROY_QUERY.ordinal(), destroyExtraInfo,
            thatId, ownParty().getPartyId()
        );
        DataPacket thatDataPacket = rpc.receive(thatHeader);
        MpcAbortPreconditions.checkArgument(thisDataPacket.getPayload().size() == 0);
        MpcAbortPreconditions.checkArgument(thatDataPacket.getPayload().size() == 0);
        // remove encode task ID from the set
        encodeTaskIdMap.remove(destroyEncodeTaskId);
        // response to the left party
        DataPacketHeader leftResponseHeader = new DataPacketHeader(
            destroyEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.DESTROY_RESPONSE.ordinal(), destroyExtraInfo,
            ownParty().getPartyId(), leftParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(leftResponseHeader, new LinkedList<>()));
        // response to the right party
        DataPacketHeader rightResponseHeader = new DataPacketHeader(
            destroyEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.DESTROY_RESPONSE.ordinal(), destroyExtraInfo,
            ownParty().getPartyId(), rightParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(rightResponseHeader, new LinkedList<>()));
    }
}
