package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.baseline;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkUtils;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewReceiverOutput;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewReceiver;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiClientOutput;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Feng Han
 * @date 2024/7/19
 */
public class BaselinePkFkViewReceiver extends AbstractTwoPartyPto implements PkFkViewReceiver {
    private final Z2MuxParty z2MuxParty;
    private final PlpsiClient<byte[]> plpsiClient;

    protected BaselinePkFkViewReceiver(Rpc rpc, Party senderParty, BaselinePkFkViewConfig config) {
        super(BaselinePkFkViewPtoDesc.getInstance(), rpc, senderParty, config);
        z2MuxParty = Z2MuxFactory.createReceiver(rpc, senderParty, config.getZ2MuxConfig());
        plpsiClient = PlpsiFactory.createClient(rpc, senderParty, config.getPlpsiConfig());
        addMultipleSubPtos(z2MuxParty, plpsiClient);
    }

    @Override
    public void init(int senderPayloadBitLen, int senderSize, int receiverSize) throws MpcAbortException {
        assert senderPayloadBitLen * senderSize > 0;
        z2MuxParty.init(receiverSize);
        plpsiClient.init(senderSize, receiverSize);
        initState();
    }

    @Override
    public PkFkViewReceiverOutput generate(byte[][] key, BitVector[] payload, int senderSize, int senderPayloadBitLen) throws MpcAbortException {
        return innerCommon(key, payload, senderSize, senderPayloadBitLen);
    }

    @Override
    public PkFkViewReceiverOutput refresh(PkFkViewReceiverOutput preView, int senderSize) throws MpcAbortException {
        return innerCommon(preView.inputKey, preView.inputPayload, senderSize, preView.shareData.length);
    }

    private PkFkViewReceiverOutput innerCommon(byte[][] key, BitVector[] payload, int senderSize, int senderPayloadBitLen) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        int receiverSize = key.length;
        assert key.length == payload.length;

        // 1. key加后缀
        stopWatch.start();
        byte[][] appendKey = PkFkUtils.addIndex(key);
        int[] sigma = PkFkUtils.permutation4Sort(appendKey);
        appendKey = BenesNetworkUtils.permutation(sigma, appendKey);
        BitVector[] selfPayload = BenesNetworkUtils.permutation(sigma, payload);
        stopWatch.stop();
        long perProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, perProcess);

        // 2. Payload psi
        stopWatch.start();
        PlpsiClientOutput<byte[]> psiRes = plpsiClient.psiWithPayload(Arrays.stream(appendKey).collect(Collectors.toList()),
            senderSize, new int[]{senderPayloadBitLen}, new boolean[]{true});
        stopWatch.stop();
        long psiProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, psiProcess);

        // 3. 用e置0非交集
        stopWatch.start();
        SquareZ2Vector[] sharePayload = psiRes.getZ2ColumnPayload(0);
        sharePayload = z2MuxParty.mux(psiRes.getZ1(), sharePayload);
        stopWatch.stop();
        long muxProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, muxProcess);

        // 4. 复制值
        stopWatch.start();
        SquareZ2Vector[] duplicateInput = new SquareZ2Vector[sharePayload.length + 1];
        System.arraycopy(sharePayload, 0, duplicateInput, 0, sharePayload.length);
        duplicateInput[sharePayload.length] = psiRes.getZ1();
        boolean[] flag = new boolean[receiverSize];
        for (int i = 1; i < receiverSize; i++) {
            flag[i] = Arrays.equals(
                Arrays.copyOf(appendKey[i - 1], appendKey[i - 1].length - 4),
                Arrays.copyOf(appendKey[i], appendKey[i].length - 4));
        }
        SquareZ2Vector vecFlag = SquareZ2Vector.create(BitVectorFactory.create(receiverSize, BinaryUtils.binaryToRoundByteArray(flag)), true);
        // todo
        SquareZ2Vector[] duplicateRes = null;
        SquareZ2Vector[] forTrans = Arrays.copyOf(duplicateRes, sharePayload.length);
        SquareZ2Vector finalEqualFlag = duplicateRes[sharePayload.length];

        PkFkViewReceiverOutput output = new PkFkViewReceiverOutput(key, payload,
            IntStream.range(0, receiverSize).toArray(), sigma,
            forTrans, selfPayload, finalEqualFlag
        );
        stopWatch.stop();
        long transProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, transProcess);

        logPhaseInfo(PtoState.PTO_END);
        return output;
    }
}
