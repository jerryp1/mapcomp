package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.php24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.*;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.baseline.BaselinePkFkViewPtoDesc;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.baseline.BaselinePkFkViewPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput.MapType;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapServer;
import scala.collection.mutable.HashMap;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Php24PkFkViewSender extends AbstractTwoPartyPto implements PkFkViewSender {
    private final PlainPayloadMuxParty plainPayloadMuxParty;
    private final PmapServer<byte[]> pmapServer;
    private final OsnSender osnSender;
    private final PrefixAggParty prefixAggParty;

    private byte[][] appendKey;
    private PmapPartyOutput<byte[]> pmapRes;
    private int[] pi;
    private SquareZ2Vector mapEqualFlag;

    public Php24PkFkViewSender(Rpc rpc, Party receiverParty, Php24PkFkViewConfig config) {
        super(BaselinePkFkViewPtoDesc.getInstance(), rpc, receiverParty, config);
        plainPayloadMuxParty = PlainPlayloadMuxFactory.createSender(rpc, receiverParty, config.getPlainPayloadMuxConfig());
        pmapServer = PmapFactory.createServer(rpc, receiverParty, config.getPmapConfig());
        osnSender = OsnFactory.createSender(rpc, receiverParty, config.getOsnConfig());
        prefixAggParty = PrefixAggFactory.createPrefixAggSender(rpc, receiverParty, config.getPrefixAggConfig());
        addMultipleSubPtos(plainPayloadMuxParty, pmapServer, osnSender, prefixAggParty);
    }

    @Override
    public void init(int payloadBitLen, int senderSize, int receiverSize) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        MathPreconditions.checkGreaterOrEqual("senderSize >= receiverSize", senderSize, receiverSize);
        assert payloadBitLen * senderSize > 0;

        pmapServer.init(senderSize, receiverSize);
        osnSender.init(senderSize + receiverSize * 20);
        prefixAggParty.init(256, senderSize + receiverSize);
        plainPayloadMuxParty.init(senderSize + receiverSize);
        initState();

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PkFkViewSenderOutput generate(byte[][] key, BitVector[] payload, int receiverSize) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        assert key.length == payload.length;
        int senderPayloadBitLen = payload[0].bitNum();

        // 1. 预计算map
        stopWatch.start();
        preComp(key, receiverSize);
        stopWatch.stop();
        long psiProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, psiProcess);

        // 2. 用e置0非交集
        stopWatch.start();
        // 2.1 扩展输入的payload长度
        BitVector[] extendPayload = getExtendPayload(payload);
        BitVector[] columnPayload = Arrays.stream(ZlDatabase.create(envType, parallel, extendPayload).getBytesData())
            .map(ea -> BitVectorFactory.create(extendPayload.length, ea))
            .toArray(BitVector[]::new);
        SquareZ2Vector[] sharePayload = plainPayloadMuxParty.muxB(mapEqualFlag, columnPayload, senderPayloadBitLen);
        stopWatch.stop();
        long muxProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, muxProcess);

        // debug
        sendOtherPartyEqualSizePayload(PtoStep.DEBUG.ordinal(), Arrays.stream(key).collect(Collectors.toList()));
        sendOtherPartyPayload(PtoStep.DEBUG.ordinal(), Collections.singletonList(mapEqualFlag.getBitVector().getBytes()));
        sendOtherPartyPayload(PtoStep.DEBUG.ordinal(), Arrays.stream(payload).map(BitVector::getBytes).collect(Collectors.toList()));
        sendOtherPartyPayload(PtoStep.DEBUG.ordinal(), Arrays.stream(sharePayload)
            .map(SquareZ2Vector::getBitVector)
            .map(BitVector::getBytes)
            .collect(Collectors.toList()));

        // 3. osn: the last bit is eqFlag
        stopWatch.start();
        int osnByteLen = CommonUtils.getByteLength(senderPayloadBitLen) + 1;
        byte[][] transPayloadBytes = ZlDatabase.create(envType, parallel,
                Arrays.stream(sharePayload).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new))
            .getBytesData();
        byte[][] osnInput = new byte[pi.length][];
        for (int i = 0; i < pi.length; i++) {
            osnInput[i] = new byte[osnByteLen];
            osnInput[i][0] = (byte) (mapEqualFlag.getBitVector().get(i) ? 1 : 0);
            System.arraycopy(transPayloadBytes[i], 0, osnInput[i], 1, transPayloadBytes[i].length);
        }
        OsnPartyOutput osnPartyOutput = osnSender.osn(new Vector<>(Arrays.stream(osnInput).collect(Collectors.toList())), osnByteLen);
        BitVector[] osnSenderPayload = new BitVector[pi.length];
        BitVector osnEqual = BitVectorFactory.createZeros(pi.length);
        byte andNum = (byte) (senderPayloadBitLen % 8 == 0 ? 255 : (1 << (senderPayloadBitLen % 8)) - 1);
        for (int i = 0; i < pi.length; i++) {
            byte[] tmp = osnPartyOutput.getShare(i);
            tmp[1] &= andNum;
            osnSenderPayload[i] = BitVectorFactory.create(senderPayloadBitLen, Arrays.copyOfRange(tmp, 1, tmp.length));
            osnEqual.set(i, (tmp[0] & 1) == 1);
        }
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, osnTime);


        // debug osn
        sendOtherPartyPayload(PtoStep.DEBUG.ordinal(), Arrays.stream(osnSenderPayload).map(BitVector::getBytes).collect(Collectors.toList()));
        sendOtherPartyPayload(PtoStep.DEBUG.ordinal(), Collections.singletonList(osnEqual.getBytes()));


        // 4. traversal
        stopWatch.start();
        SquareZ2Vector[] groupInput = new SquareZ2Vector[senderPayloadBitLen + 1];
        byte[][] payloadTrans = ZlDatabase.create(envType, parallel, osnSenderPayload).getBytesData();
        IntStream.range(0, payloadTrans.length).forEach(i -> groupInput[i] = SquareZ2Vector.create(pi.length, payloadTrans[i], false));
        groupInput[senderPayloadBitLen] = SquareZ2Vector.create(osnEqual, false);
        PrefixAggOutput prefixAggOutput = prefixAggParty.agg((String[]) null, groupInput);
        SquareZ2Vector[] groupOut = prefixAggOutput.getAggsBinary();
        stopWatch.stop();
        long traversalTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, traversalTime);

        logPhaseInfo(PtoState.PTO_END);
        return new PkFkViewSenderOutput(key, payload, pi,
            Arrays.copyOf(groupOut, senderPayloadBitLen), groupOut[senderPayloadBitLen], mapEqualFlag, receiverSize);
    }

    @Override
    public PkFkViewSenderOutput refresh(PkFkViewSenderOutput preView, BitVector[] payload) throws MpcAbortException {
        pi = preView.pi;
        mapEqualFlag = preView.mapEqualFlag;
        int senderPayloadBitLen = preView.shareData.length;
        logPhaseInfo(PtoState.PTO_BEGIN);

        // 1. 用e置0非交集
        stopWatch.start();
        BitVector[] extendPayload = getExtendPayload(payload);
        BitVector[] columnPayload = Arrays.stream(ZlDatabase.create(envType, parallel, extendPayload).getBytesData())
            .map(ea -> BitVectorFactory.create(extendPayload.length, ea))
            .toArray(BitVector[]::new);
        SquareZ2Vector[] sharePayload = plainPayloadMuxParty.muxB(mapEqualFlag, columnPayload, senderPayloadBitLen);
        stopWatch.stop();
        long muxProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, muxProcess);

        // debug
        sendOtherPartyEqualSizePayload(PtoStep.DEBUG.ordinal(), Arrays.stream(preView.inputKey).collect(Collectors.toList()));
        sendOtherPartyPayload(PtoStep.DEBUG.ordinal(), Collections.singletonList(mapEqualFlag.getBitVector().getBytes()));
        sendOtherPartyPayload(PtoStep.DEBUG.ordinal(), Arrays.stream(payload).map(BitVector::getBytes).collect(Collectors.toList()));
        sendOtherPartyPayload(PtoStep.DEBUG.ordinal(), Arrays.stream(sharePayload)
            .map(SquareZ2Vector::getBitVector)
            .map(BitVector::getBytes)
            .collect(Collectors.toList()));

        // 2. osn: the last bit is eqFlag
        stopWatch.start();
        int osnByteLen = CommonUtils.getByteLength(senderPayloadBitLen);
        byte[][] osnInput = ZlDatabase.create(envType, parallel,
                Arrays.stream(sharePayload).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new))
            .getBytesData();
        OsnPartyOutput osnPartyOutput = osnSender.osn(new Vector<>(Arrays.stream(osnInput).collect(Collectors.toList())), osnByteLen);
        byte andNum = (byte) (senderPayloadBitLen % 8 == 0 ? 255 : (1 << (senderPayloadBitLen % 8)) - 1);
        BitVector[] osnSenderPayload = Arrays.stream(osnPartyOutput.getVector().toArray(new byte[0][]))
            .map(ea -> {
                ea[0] &= andNum;
                return BitVectorFactory.create(senderPayloadBitLen, ea);
            })
            .toArray(BitVector[]::new);
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, osnTime);

        // debug osn
        sendOtherPartyPayload(PtoStep.DEBUG.ordinal(), Arrays.stream(osnSenderPayload).map(BitVector::getBytes).collect(Collectors.toList()));

        // 3. traversal
        stopWatch.start();
        SquareZ2Vector[] groupInput = Arrays.stream(ZlDatabase.create(envType, parallel, osnSenderPayload).getBytesData())
            .map(ea -> SquareZ2Vector.create(osnSenderPayload.length, ea, false))
            .toArray(SquareZ2Vector[]::new);
        PrefixAggOutput prefixAggOutput = prefixAggParty.agg((String[]) null, groupInput);
        SquareZ2Vector[] groupOut = prefixAggOutput.getAggsBinary();
        stopWatch.stop();
        long traversalTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, traversalTime);

        logPhaseInfo(PtoState.PTO_END);
        return new PkFkViewSenderOutput(preView.inputKey, payload, preView.pi,
            groupOut, preView.equalFlag, preView.mapEqualFlag, preView.receiverInputSize);
    }

    private void preComp(byte[][] key, int receiverSize) throws MpcAbortException {
        int senderSize = key.length;
        // 1. key加后缀
        appendKey = Arrays.stream(key).map(ea -> {
            byte[] tmp = new byte[ea.length + 4];
            System.arraycopy(ea, 0, tmp, 0, ea.length);
            return tmp;
        }).toArray(byte[][]::new);
        // 2. pmap
        pmapRes = pmapServer.map(Arrays.stream(appendKey).collect(Collectors.toList()), receiverSize);

        Map<Integer, byte[]> resultMap = pmapRes.getIndexMap();
        // extend the permutation
        if (pmapRes.getIndexMap().size() < senderSize) {
            assert pmapRes.getMapType().equals(MapType.PSI);
            HashSet<BigInteger> psiKeySet = pmapRes.getIndexMap().values().stream()
                .map(BigInteger::new)
                .collect(Collectors.toCollection(HashSet::new));
            int startIndex = pmapRes.getIndexMap().size();
            for (byte[] oneKey : appendKey) {
                if (!psiKeySet.contains(new BigInteger(oneKey))) {
                    resultMap.put(startIndex++, oneKey);
                }
            }
        }
        if (pmapRes.getIndexMap().size() < receiverSize) {
            for (int i = pmapRes.getIndexMap().size(); i < receiverSize; i++) {
                resultMap.put(i, new byte[0]);
            }
        }
        // extend flag
        mapEqualFlag = pmapRes.getEqualFlag();
        if (resultMap.size() > mapEqualFlag.bitNum()) {
            BigInteger eqBig = mapEqualFlag.getBitVector().getBigInteger().shiftLeft(resultMap.size() - mapEqualFlag.bitNum());
            mapEqualFlag = SquareZ2Vector.create(BitVectorFactory.create(resultMap.size(), eqBig), false);
        }
        // get pi
        HashMap<BigInteger, Integer> data2pos = new HashMap<>();
        for (int i = 0; i < senderSize; i++) {
            data2pos.put(new BigInteger(appendKey[i]), i);
        }
        int dummyIndex = senderSize;
        pi = new int[resultMap.size()];
        for (int i = 0; i < resultMap.size(); i++) {
            byte[] tmp = resultMap.get(i);
            if(tmp == null || tmp.length < 1){
                pi[i] = dummyIndex++;
            }else{
                BigInteger tmpBig = new BigInteger(tmp);
                if (data2pos.contains(tmpBig)) {
                    pi[i] = data2pos.get(tmpBig).get();
                } else {
                    pi[i] = dummyIndex++;
                }
            }
        }
        assert dummyIndex == resultMap.size();
    }

    private BitVector[] getExtendPayload(BitVector[] payload) {
        int senderPayloadBitLen = payload[0].bitNum();
        BitVector[] extendPayload = new BitVector[pi.length];
        for (int i = 0; i < pi.length; i++) {
            int target = pi[i];
            if (target < payload.length) {
                extendPayload[i] = payload[target];
            } else {
                extendPayload[i] = BitVectorFactory.createZeros(senderPayloadBitLen);
            }
        }
        return extendPayload;
    }
}
