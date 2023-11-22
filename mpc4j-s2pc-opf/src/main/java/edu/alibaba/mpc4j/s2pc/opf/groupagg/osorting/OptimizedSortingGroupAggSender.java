package edu.alibaba.mpc4j.s2pc.opf.groupagg.osorting;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.AbstractGroupAggParty;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggOut;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggUtils;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.osorting.OptimizedSortingGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationReceiver;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationSender;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggParty;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Optimized sorting-based group aggregation sender.
 *
 * @author Li Peng
 * @date 2023/11/19
 */
public class OptimizedSortingGroupAggSender extends AbstractGroupAggParty {
    /**
     * Osn receiver.
     */
    private final OsnReceiver osnReceiver;
    /**
     * Zl mux party.
     */
    private final ZlMuxParty zlMuxSender;
    /**
     * Prefix aggregation sender.
     */
    private final PrefixAggParty prefixAggSender;
    /**
     * Z2 circuit sender.
     */
    private final Z2cParty z2cSender;
    /**
     * Zl circuit sender
     */
    private final ZlcParty zlcSender;
    /**
     * B2a sender.
     */
    private final B2aParty b2aSender;
    /**
     * Permutation sender.
     */
    private final PermutationSender permutationSender;
    /**
     * Permutation receiver.
     */
    private final PermutationReceiver permutationReceiver;
    /**
     * Z2 integer circuit.
     */
    private final Z2IntegerCircuit z2IntegerCircuit;
    /**
     * Own bit split.
     */
    private Vector<byte[]> eByte;
    /**
     * permutation of pSorter
     */
    private Vector<byte[]> piGi;
    /**
     * sigma_b
     */
    private int[] sigmaB;
    /**
     * rou
     */
    private Vector<byte[]> rho;

    public OptimizedSortingGroupAggSender(Rpc senderRpc, Party receiverParty, OptimizedSortingGroupAggConfig config) {
        super(OptimizedSortingGroupAggPtoDesc.getInstance(), senderRpc, receiverParty, config);
        osnReceiver = OsnFactory.createReceiver(senderRpc, receiverParty, config.getOsnConfig());
        zlMuxSender = ZlMuxFactory.createSender(senderRpc, receiverParty, config.getZlMuxConfig());
        prefixAggSender = PrefixAggFactory.createPrefixAggSender(senderRpc, receiverParty, config.getPrefixAggConfig());
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        b2aSender = B2aFactory.createSender(senderRpc, receiverParty, config.getB2aConfig());
        permutationSender = PermutationFactory.createSender(senderRpc, receiverParty, config.getPermutationConfig());
        permutationReceiver = PermutationFactory.createReceiver(senderRpc, receiverParty, config.getPermutationConfig());
//        addSubPtos(osnReceiver);
//        addSubPtos(zlMuxSender);
//        addSubPtos(sharedPermutationSender);
//        addSubPtos(prefixAggSender);
//        addSubPtos(z2cSender);
//        addSubPtos(zlcSender);
        z2IntegerCircuit = new Z2IntegerCircuit(z2cSender);
    }

    @Override
    public void init(Properties properties) throws MpcAbortException {
        super.init(properties);
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();

        osnReceiver.init(maxNum);
        zlMuxSender.init(maxNum);
        prefixAggSender.init(maxL, maxNum);
        z2cSender.init(maxL * maxNum);
        zlcSender.init(1);
        b2aSender.init(maxL, maxNum);
        permutationSender.init(maxL, maxNum);
        permutationReceiver.init(maxL, maxNum);

        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public GroupAggOut groupAgg(String[] groupField, long[] aggField,
                                SquareZ2Vector interFlagE) throws MpcAbortException {
        assert aggField == null;
        // set input
        setPtoInput(groupField, aggField, interFlagE);
        // osn1
        osn1(groupField);
        // pSorting using e and receiver's group
        pSorter();
        // permute
        permute1();
        // permute
        permute2();
        // merge group
        Vector<byte[]> mergedTwoGroup = mergeGroup();
        // b2a
        SquareZlVector otherAggB2a = b2a();
        // ### test
        zlcSender.revealOther(otherAggB2a);
        revealOtherGroup(mergedTwoGroup);
        // agg
        aggregation(mergedTwoGroup, otherAggB2a, e);
        return null;
    }

    private void osn1(String[] groupAttr) throws MpcAbortException {
        // sigma_s permutation
        sigmaB = obtainPerms(groupAttr);
        // osn1, sender permute receiver's group, agg and e
        Vector<byte[]> osnOutput1 = osnReceiver.osn(sigmaB, receiverGroupByteLength + 1).getShare();
        // split
        List<Vector<byte[]>> splits = GroupAggUtils.split(osnOutput1, new int[]{receiverGroupByteLength, 1});
        receiverGroupShare = splits.get(0);
        Vector<byte[]> receiverE = splits.get(1);

        // e share in byte form
        Vector<byte[]> tempE = IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(1)
            .put((e.getBitVector().get(i) ? (byte) 1 : (byte) 0)).array()).collect(Collectors.toCollection(Vector::new));
        // permute own e share
        Vector<byte[]> tempE2 = BenesNetworkUtils.permutation(sigmaB, tempE);
        // xor two e shares to get aligned e shares
        eByte = IntStream.range(0, num).mapToObj(i -> BytesUtils.xor(receiverE.get(i), tempE2.get(i))).collect(Collectors.toCollection(Vector::new));
        // sender group
        senderGroupShare = GroupAggUtils.binaryStringToBytes(groupAttr);

        // ### test
        revealOtherBit(eByte);
    }

    private void pSorter() throws MpcAbortException {
        // obtain input of osn
        Vector<byte[]> mergedSortInput = merge(Arrays.asList(eByte, receiverGroupShare));

        // prepare psorter input, with shared e and group of receiver
        SquareZ2Vector[] psorterInput = Arrays.stream(TransposeUtils.transposeSplit(mergedSortInput, (receiverGroupByteLength + 1) * 8))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        // psorter
        SquareZ2Vector[] piGiVector = Arrays.stream(z2IntegerCircuit.psort(new SquareZ2Vector[][]{psorterInput},
            null, PlainZ2Vector.createOnes(1), true, true)).map(v -> (SquareZ2Vector) v).toArray(SquareZ2Vector[]::new);
        // ### test
        for (int i = 0; i < piGiVector.length; i++) {
            z2cSender.revealOther(piGiVector[i]);
        }

        // get vector form of piGi
        piGi = TransposeUtils.transposeMergeToVector(Arrays.stream(piGiVector).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));

        // ### test
        revealOtherLong(piGi.stream().map(v -> BytesUtils.fixedByteArrayLength(v, 8)).collect(Collectors.toCollection(Vector::new)));

        // get receiver's shared agg and e from psorter's input, which have been sorted.
        Vector<byte[]> trans = TransposeUtils.transposeMergeToVector(Arrays.stream(psorterInput).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));

        List<Vector<byte[]>> splitOther = GroupAggUtils.split(trans, new int[]{1, receiverGroupByteLength});
        receiverGroupShare = splitOther.get(1);
        // ### test
        revealOtherGroup(receiverGroupShare);

        e = SquareZ2Vector.createZeros(num, false);
        IntStream.range(0, num).forEach(i -> e.getBitVector().set(i, (splitOther.get(0).get(i)[0] & 1) == 1));
        z2cSender.revealOther(e);
    }

    private void permute1() throws MpcAbortException {
        senderGroupShare = BenesNetworkUtils.permutation(sigmaB, senderGroupShare);
        // sender's group and sigmaB
        Vector<byte[]> permInput = IntStream.range(0, num).mapToObj(i ->
            ByteBuffer.allocate(senderGroupByteLength + Integer.BYTES)
                .put(senderGroupShare.get(i)).putInt(sigmaB[i]).array())
            .collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> permuted = permutationSender.permute(piGi, permInput);
        List<Vector<byte[]>> splitPermuted = GroupAggUtils.split(permuted, new int[]{senderGroupByteLength, Integer.BYTES});
        senderGroupShare = splitPermuted.get(0);
        rho = splitPermuted.get(1);
    }

    private void permute2() throws MpcAbortException {
        aggShare = permutationReceiver.permute(rho, Long.BYTES);
    }

    private Vector<byte[]> mergeGroup() {
        // merge group
        return IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(totalGroupByteLength)
            .put(senderGroupShare.get(i)).put(receiverGroupShare.get(i)).array()).collect(Collectors.toCollection(Vector::new));
    }

    private SquareZlVector b2a() throws MpcAbortException {
        // b2a
        SquareZ2Vector[] transposed = Arrays.stream(TransposeUtils.transposeSplit(aggShare, Long.SIZE))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        return b2aSender.b2a(transposed);
    }

    private void aggregation(Vector<byte[]> groupField, SquareZlVector aggField, SquareZ2Vector flag) throws MpcAbortException {
        PrefixAggOutput agg = prefixAggSender.agg(groupField, aggField, flag);
        // reveal
        zlcSender.revealOther(agg.getAggs());
        revealOtherGroup(agg.getGroupings());
        z2cSender.revealOther(agg.getIndicator());

        Preconditions.checkArgument(agg.getNum() == num, "size of output not correct");
    }

    protected void revealOtherGroup(Vector<byte[]> input) {
        List<byte[]> otherShares = new ArrayList<>(input);

        DataPacketHeader sendSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.REVEAL_OUTPUT.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sendSharesHeader, otherShares));
        extraInfo++;
    }

    protected void revealOtherBit(Vector<byte[]> input) {

        List<byte[]> otherShares = new ArrayList<>(input);

        DataPacketHeader sendSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.REVEAL_BIT.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sendSharesHeader, otherShares));
        extraInfo++;
    }

    protected void revealOtherLong(Vector<byte[]> input) {

        List<byte[]> otherShares = new ArrayList<>(input);
        DataPacketHeader sendSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.TEST.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sendSharesHeader, otherShares));
        extraInfo++;
    }
}
