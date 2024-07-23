package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.baseline;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewSender;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewSenderOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiServer;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiShareOutput;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Feng Han
 * @date 2024/7/19
 */
public class BaselinePkFkViewSender extends AbstractTwoPartyPto implements PkFkViewSender {
    private final Z2MuxParty z2MuxParty;
    private final PlpsiServer<byte[], byte[]> plpsiServer;
    private final PrefixAggParty prefixAggParty;
    private final OsnSender osnSender;

    public BaselinePkFkViewSender(Rpc rpc, Party receiverParty, BaselinePkFkViewConfig config) {
        super(BaselinePkFkViewPtoDesc.getInstance(), rpc, receiverParty, config);
        z2MuxParty = Z2MuxFactory.createSender(rpc, receiverParty, config.getZ2MuxConfig());
        plpsiServer = PlpsiFactory.createServer(rpc, receiverParty, config.getPlpsiConfig());
        prefixAggParty = PrefixAggFactory.createPrefixAggSender(rpc, receiverParty, config.getPrefixAggConfig());
        osnSender = OsnFactory.createSender(rpc, receiverParty, config.getOsnConfig());
        addMultipleSubPtos(z2MuxParty, plpsiServer, prefixAggParty, osnSender);
    }

    @Override
    public void init(int payloadBitLen, int senderSize, int receiverSize) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        assert payloadBitLen * senderSize > 0;
        z2MuxParty.init(receiverSize * 20);
        plpsiServer.init(senderSize, receiverSize);
        osnSender.init(receiverSize * 20);
        prefixAggParty.init(256, receiverSize * 10);
        initState();

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PkFkViewSenderOutput generate(byte[][] key, BitVector[] payload, int receiverSize) throws MpcAbortException {
        return innerCommon(key, payload, receiverSize);
    }

    @Override
    public PkFkViewSenderOutput refresh(PkFkViewSenderOutput preView, BitVector[] payload) throws MpcAbortException {
        assert preView.inputKey.length == payload.length;
        return innerCommon(preView.inputKey, payload, preView.receiverInputSize);
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
        List<byte[]> payloadByteList = Arrays.stream(payload).map(BitVector::getBytes).collect(Collectors.toList());
        PlpsiShareOutput psiRes = plpsiServer.psiWithPayload(Arrays.stream(appendKey).collect(Collectors.toList()), receiverSize,
            Collections.singletonList(payloadByteList), new int[]{payload[0].bitNum()}, new boolean[]{true}
        );
        stopWatch.stop();
        long psiProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, psiProcess);

        // 2. 用e置0非交集
        stopWatch.start();
        SquareZ2Vector[] sharePayload = psiRes.getZ2ColumnPayload(0);
        sharePayload = z2MuxParty.mux(psiRes.getZ1(), sharePayload);
        stopWatch.stop();
        long muxProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, muxProcess);

        // 3. osn
        stopWatch.start();
        byte[][] sharePayloadInRow = ZlDatabase.create(envType, parallel,
                Arrays.stream(sharePayload).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new))
            .getBytesData();
        byte[][] osnInput = new byte[sharePayloadInRow.length][];
        for (int i = 0; i < osnInput.length; i++) {
            osnInput[i] = new byte[sharePayloadInRow[i].length + 1];
            osnInput[i][0] = (byte) (psiRes.getZ1().getBitVector().get(i) ? 1 : 0);
            System.arraycopy(sharePayloadInRow[i], 0, osnInput[i], 1, sharePayloadInRow[i].length);
        }
        OsnPartyOutput osnPartyOutput = osnSender.osn(new Vector<>(Arrays.stream(osnInput).collect(Collectors.toList())), osnInput[0].length);
        stopWatch.stop();
        long osnProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, osnProcess);

        // 4. 复制值
        stopWatch.start();
        SquareZ2Vector[] duplicateInput = new SquareZ2Vector[sharePayload.length + 1];
        SquareZ2Vector[] payloadAndFlag = Arrays.stream(
                ZlDatabase.create(osnInput[0].length * 8 - 7, osnPartyOutput.getShareArray(osnInput[0].length * 8 - 7))
                    .bitPartition(envType, parallel))
            .map(ea -> SquareZ2Vector.create(ea, false))
            .toArray(SquareZ2Vector[]::new);
        System.arraycopy(payloadAndFlag, payloadAndFlag.length - sharePayload.length, duplicateInput, 0, sharePayload.length);
        duplicateInput[sharePayload.length] = payloadAndFlag[0];
        PrefixAggOutput prefixAggOutput = prefixAggParty.agg((String[]) null, duplicateInput);
        SquareZ2Vector[] duplicateRes = prefixAggOutput.getAggsBinary();
        SquareZ2Vector[] forTrans = Arrays.copyOf(duplicateRes, sharePayload.length);
        SquareZ2Vector finalEqualFlag = duplicateRes[sharePayload.length];

        PkFkViewSenderOutput output = new PkFkViewSenderOutput(key, payload, null, forTrans, finalEqualFlag, psiRes.getZ1(), receiverSize);
        stopWatch.stop();
        long transProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, transProcess);

        logPhaseInfo(PtoState.PTO_END);
        return output;
    }
}
