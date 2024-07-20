package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.baseline;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewSender;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewSenderOutput;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiServer;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiShareOutput;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Feng Han
 * @date 2024/7/19
 */
public class BaselinePkFkViewSender extends AbstractTwoPartyPto implements PkFkViewSender {
    private final Z2MuxParty z2MuxParty;
    private final PlpsiServer<byte[], byte[]> plpsiServer;

    protected BaselinePkFkViewSender(Rpc rpc, Party receiverParty, BaselinePkFkViewConfig config) {
        super(BaselinePkFkViewPtoDesc.getInstance(), rpc, receiverParty, config);
        z2MuxParty = Z2MuxFactory.createSender(rpc, receiverParty, config.getZ2MuxConfig());
        plpsiServer = PlpsiFactory.createServer(rpc, receiverParty, config.getPlpsiConfig());
        addMultipleSubPtos(z2MuxParty, plpsiServer);
    }

    @Override
    public void init(int senderPayloadBitLen, int senderSize, int receiverSize) throws MpcAbortException {
        assert senderPayloadBitLen * senderSize > 0;
        z2MuxParty.init(receiverSize);
        plpsiServer.init(senderSize, receiverSize);
        initState();
    }

    @Override
    public PkFkViewSenderOutput generate(byte[][] key, BitVector[] payload, int receiverSize) throws MpcAbortException {
        return innerCommon(key, payload, receiverSize);
    }

    @Override
    public PkFkViewSenderOutput refresh(PkFkViewSenderOutput preView, int receiverSize) throws MpcAbortException {
        return innerCommon(preView.inputKey, preView.inputPayload, receiverSize);
    }

    private PkFkViewSenderOutput innerCommon(byte[][] key, BitVector[] payload, int receiverSize) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        int senderSize = key.length;
        assert key.length == payload.length;

        // 1. key加后缀
        stopWatch.start();
        byte[][] appendKey = Arrays.stream(key).map(ea -> {
            byte[] tmp = new byte[ea.length + 4];
            System.arraycopy(ea, 0, tmp, 0, ea.length);
            return tmp;
        }).toArray(byte[][]::new);
        stopWatch.stop();
        long perProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, perProcess);

        // 2. Payload psi
        stopWatch.start();
        PlpsiShareOutput psiRes = plpsiServer.psi(Arrays.stream(appendKey).collect(Collectors.toList()), receiverSize);
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
        // todo
        SquareZ2Vector[] duplicateRes = null;
        SquareZ2Vector[] forTrans = Arrays.copyOf(duplicateRes, sharePayload.length);
        SquareZ2Vector finalEqualFlag = duplicateRes[sharePayload.length];

        PkFkViewSenderOutput output = new PkFkViewSenderOutput(key, payload, null, forTrans, finalEqualFlag);
        stopWatch.stop();
        long transProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, transProcess);

        logPhaseInfo(PtoState.PTO_END);
        return output;
    }
}
