package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.php24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggUtils;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkUtils;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewReceiver;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewReceiverOutput;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.baseline.BaselinePkFkViewPtoDesc;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapClient;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput.MapType;
import gnu.trove.list.array.TIntArrayList;
import scala.collection.mutable.HashMap;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Php24PkFkViewReceiver extends AbstractTwoPartyPto implements PkFkViewReceiver {
    private final PlainPayloadMuxParty plainPayloadMuxParty;
    private final PmapClient<byte[]> pmapClient;
    private final OsnReceiver osnReceiver;
    private final PrefixAggParty prefixAggParty;

    private byte[][] appendKey;
    private PmapPartyOutput<byte[]> pmapRes;
    private int[] pi;
    private int[] sigma;
    private SquareZ2Vector mapEqualFlag;

    public Php24PkFkViewReceiver(Rpc rpc, Party senderParty, Php24PkFkViewConfig config) {
        super(BaselinePkFkViewPtoDesc.getInstance(), rpc, senderParty, config);
        plainPayloadMuxParty = PlainPlayloadMuxFactory.createReceiver(rpc, senderParty, config.getPlainPayloadMuxConfig());
        pmapClient = PmapFactory.createClient(rpc, senderParty, config.getPmapConfig());
        osnReceiver = OsnFactory.createReceiver(rpc, senderParty, config.getOsnConfig());
        prefixAggParty = PrefixAggFactory.createPrefixAggReceiver(rpc, senderParty, config.getPrefixAggConfig());
        addMultipleSubPtos(plainPayloadMuxParty, pmapClient, osnReceiver, prefixAggParty);
    }

    @Override
    public void init(int senderPayloadBitLen, int senderSize, int receiverSize) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);
        MathPreconditions.checkGreaterOrEqual("senderSize >= receiverSize", senderSize, receiverSize);
        assert senderPayloadBitLen * senderSize > 0;

        pmapClient.init(receiverSize, senderSize);
        osnReceiver.init(senderSize + receiverSize * 20);
        prefixAggParty.init(256, senderSize + receiverSize);
        plainPayloadMuxParty.init(senderSize + receiverSize);
        initState();

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PkFkViewReceiverOutput generate(byte[][] key, BitVector[] payload, int senderSize, int senderPayloadBitLen) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        assert key.length == payload.length;
        int keyByteLen = key[0].length;

        // 1. 预计算map
        stopWatch.start();
        preComp(key, senderSize);
        stopWatch.stop();
        long psiProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, psiProcess);

        // 2. 用e置0非交集
        stopWatch.start();
        SquareZ2Vector[] sharePayload = plainPayloadMuxParty.muxB(mapEqualFlag, null, senderPayloadBitLen);
        stopWatch.stop();
        long muxProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, muxProcess);


//        // debug
//        List<byte[]> senderKey = receiveOtherPartyPayload(PtoStep.DEBUG.ordinal());
//        List<byte[]> senderEqShare = receiveOtherPartyPayload(PtoStep.DEBUG.ordinal());
//        List<byte[]> senderPayload = receiveOtherPartyPayload(PtoStep.DEBUG.ordinal());
//        List<byte[]> senderPayloadShare = receiveOtherPartyPayload(PtoStep.DEBUG.ordinal());
//        java.util.HashMap<BigInteger, byte[]> senderMap = new java.util.HashMap<>();
//        for (int i = 0; i < senderKey.size(); i++) {
//            byte[] originalKey = senderKey.get(i);
//            byte[] tmpKey = new byte[originalKey.length + 4];
//            System.arraycopy(originalKey, 0, tmpKey, 0, originalKey.length);
//            senderMap.put(new BigInteger(tmpKey), senderPayload.get(i));
//        }
//        // 恢复得到真正的equal flag
//        BitVector actualEqFlag = mapEqualFlag.getBitVector().xor(BitVectorFactory.create(mapEqualFlag.bitNum(), senderEqShare.get(0)));
//        BitVector[] joinPayload = new BitVector[sharePayload.length];
//        for (int i = 0; i < joinPayload.length; i++) {
//            joinPayload[i] = sharePayload[i].getBitVector().xor(BitVectorFactory.create(sharePayload[i].bitNum(), senderPayloadShare.get(i)));
//        }
//        byte[][] actualPayload = ZlDatabase.create(envType, parallel, joinPayload).getBytesData();
//        // verify
//        for(int i = 0; i < pi.length; i++){
//            if(pi[i] >= key.length){
//                assert !actualEqFlag.get(i);
//                assert Arrays.equals(actualPayload[i], new byte[actualPayload[i].length]);
//            }else{
//                BigInteger tmpKeyBig = new BigInteger(appendKey[pi[i]]);
//                if(senderMap.containsKey(tmpKeyBig)){
//                    assert actualEqFlag.get(i);
//                    assert Arrays.equals(actualPayload[i], senderMap.get(tmpKeyBig));
//                }else{
//                    assert !actualEqFlag.get(i);
//                    assert Arrays.equals(actualPayload[i], new byte[actualPayload[i].length]);
//                }
//            }
//        }

        // 3. osn: the last bit is eqFlag
        stopWatch.start();
        int osnByteLen = CommonUtils.getByteLength(senderPayloadBitLen) + 1;
        byte[][] transPayloadBytes = ZlDatabase.create(envType, parallel,
                Arrays.stream(sharePayload).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new))
            .getBytesData();
        byte[][] osnInput = new byte[sigma.length][];
        for (int i = 0; i < sigma.length; i++) {
            osnInput[i] = new byte[osnByteLen];
            osnInput[i][0] = (byte) (mapEqualFlag.getBitVector().get(i) ? 1 : 0);
            System.arraycopy(transPayloadBytes[i], 0, osnInput[i], 1, transPayloadBytes[i].length);
        }
        OsnPartyOutput osnPartyOutput = osnReceiver.osn(sigma,
            new Vector<>(Arrays.stream(osnInput).collect(Collectors.toList())), osnByteLen);
        BitVector[] osnSenderPayload = new BitVector[sigma.length];
        BitVector osnEqual = BitVectorFactory.createZeros(sigma.length);
        byte andNum = (byte) (senderPayloadBitLen % 8 == 0 ? 255 : (1 << (senderPayloadBitLen % 8)) - 1);
        for (int i = 0; i < sigma.length; i++) {
            byte[] tmp = osnPartyOutput.getShare(i);
            tmp[1] &= andNum;
            osnSenderPayload[i] = BitVectorFactory.create(senderPayloadBitLen, Arrays.copyOfRange(tmp, 1, tmp.length));
            osnEqual.set(i, (tmp[0] & 1) == 1);
        }
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, osnTime);


//        // debug osn
//        List<byte[]> osnSenderPayloadRes = receiveOtherPartyPayload(PtoStep.DEBUG.ordinal());
//        List<byte[]> osnSenderEqRes = receiveOtherPartyPayload(PtoStep.DEBUG.ordinal());
//        BitVector actualEqRes = osnEqual.xor(BitVectorFactory.create(osnEqual.bitNum(), osnSenderEqRes.get(0)));
//        BitVector[] osnRes = IntStream.range(0, osnSenderPayloadRes.size())
//            .mapToObj(i -> osnSenderPayload[i].xor(BitVectorFactory.create(osnSenderPayload[i].bitNum(), osnSenderPayloadRes.get(i))))
//            .toArray(BitVector[]::new);
//        for (int i = 0; i < sigma.length; i++) {
//            if(pi[sigma[i]] < key.length){
//                BigInteger appSortReceiverKey = new BigInteger(appendKey[pi[sigma[i]]]);
//                if (senderMap.containsKey(appSortReceiverKey)) {
//                    assert actualEqRes.get(i);
//                    assert Arrays.equals(osnRes[i].getBytes(), senderMap.get(appSortReceiverKey));
//                } else {
//                    assert !actualEqRes.get(i);
//                    assert Arrays.equals(osnRes[i].getBytes(), new byte[osnRes[i].byteNum()]);
//                }
//            }else{
//                assert !actualEqRes.get(i);
//                assert Arrays.equals(osnRes[i].getBytes(), new byte[osnRes[i].byteNum()]);
//            }
//        }


        // 4. traversal
        stopWatch.start();
        Vector<byte[]> groupBytes = getGroupBytes(key);
        String[] keyStr = GroupAggUtils.bytesToBinaryString(groupBytes, keyByteLen * 8);
        SquareZ2Vector[] groupInput = new SquareZ2Vector[senderPayloadBitLen + 1];
        byte[][] payloadTrans = ZlDatabase.create(envType, parallel, osnSenderPayload).getBytesData();
        IntStream.range(0, payloadTrans.length).forEach(i -> groupInput[i] = SquareZ2Vector.create(sigma.length, payloadTrans[i], false));
        groupInput[senderPayloadBitLen] = SquareZ2Vector.create(osnEqual, false);
        PrefixAggOutput prefixAggOutput = prefixAggParty.agg(keyStr, groupInput);
        SquareZ2Vector[] groupOut = prefixAggOutput.getAggsBinary();
        stopWatch.stop();
        long traversalTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, traversalTime);

        BitVector[] selfData = new BitVector[sigma.length];
        for (int i = 0; i < sigma.length; i++) {
            int target = pi[sigma[i]];
            if (target < key.length) {
                selfData[i] = payload[target];
            } else {
                selfData[i] = null;
            }
        }

        logPhaseInfo(PtoState.PTO_END);
        return new PkFkViewReceiverOutput(key, payload, pi, sigma,
            Arrays.copyOf(groupOut, senderPayloadBitLen), selfData, groupOut[senderPayloadBitLen], mapEqualFlag, senderSize);
    }

    @Override
    public PkFkViewReceiverOutput refresh(PkFkViewReceiverOutput preView, BitVector[] payload) throws MpcAbortException {
        appendKey = PkFkUtils.addIndex(preView.inputKey);
        int keyByteLen = preView.inputKey[0].length;
        pi = preView.pi;
        sigma = preView.sigma;
        mapEqualFlag = preView.mapEqualFlag;

        int senderPayloadBitLen = preView.shareData.length;

        // 1. 用e置0非交集
        stopWatch.start();
        SquareZ2Vector[] sharePayload = plainPayloadMuxParty.muxB(mapEqualFlag, null, senderPayloadBitLen);
        stopWatch.stop();
        long muxProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, muxProcess);

//        // debug
//        List<byte[]> senderKey = receiveOtherPartyPayload(PtoStep.DEBUG.ordinal());
//        List<byte[]> senderEqShare = receiveOtherPartyPayload(PtoStep.DEBUG.ordinal());
//        List<byte[]> senderPayload = receiveOtherPartyPayload(PtoStep.DEBUG.ordinal());
//        List<byte[]> senderPayloadShare = receiveOtherPartyPayload(PtoStep.DEBUG.ordinal());
//        java.util.HashMap<BigInteger, byte[]> senderMap = new java.util.HashMap<>();
//        for (int i = 0; i < senderKey.size(); i++) {
//            byte[] originalKey = senderKey.get(i);
//            byte[] tmpKey = new byte[originalKey.length + 4];
//            System.arraycopy(originalKey, 0, tmpKey, 0, originalKey.length);
//            senderMap.put(new BigInteger(tmpKey), senderPayload.get(i));
//        }
//        // 恢复得到真正的equal flag
//        BitVector actualEqFlag = mapEqualFlag.getBitVector().xor(BitVectorFactory.create(mapEqualFlag.bitNum(), senderEqShare.get(0)));
//        BitVector[] joinPayload = new BitVector[sharePayload.length];
//        for (int i = 0; i < joinPayload.length; i++) {
//            joinPayload[i] = sharePayload[i].getBitVector().xor(BitVectorFactory.create(sharePayload[i].bitNum(), senderPayloadShare.get(i)));
//        }
//        byte[][] actualPayload = ZlDatabase.create(envType, parallel, joinPayload).getBytesData();
//        // verify
//        for(int i = 0; i < pi.length; i++){
//            if(pi[i] >= preView.inputKey.length){
//                assert !actualEqFlag.get(i);
//                assert Arrays.equals(actualPayload[i], new byte[actualPayload[i].length]);
//            }else{
//                BigInteger tmpKeyBig = new BigInteger(appendKey[pi[i]]);
//                if(senderMap.containsKey(tmpKeyBig)){
//                    assert actualEqFlag.get(i);
//                    assert Arrays.equals(actualPayload[i], senderMap.get(tmpKeyBig));
//                }else{
//                    assert !actualEqFlag.get(i);
//                    assert Arrays.equals(actualPayload[i], new byte[actualPayload[i].length]);
//                }
//            }
//        }

        // 2. osn: the last bit is eqFlag
        stopWatch.start();
        int osnByteLen = CommonUtils.getByteLength(senderPayloadBitLen);
        byte[][] osnInput = ZlDatabase.create(envType, parallel,
                Arrays.stream(sharePayload).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new))
            .getBytesData();
        OsnPartyOutput osnPartyOutput = osnReceiver.osn(sigma,
            new Vector<>(Arrays.stream(osnInput).collect(Collectors.toList())), osnByteLen);
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


//        // debug osn
//        List<byte[]> osnSenderPayloadRes = receiveOtherPartyPayload(PtoStep.DEBUG.ordinal());
//        BitVector[] osnRes = IntStream.range(0, osnSenderPayloadRes.size())
//            .mapToObj(i -> osnSenderPayload[i].xor(BitVectorFactory.create(osnSenderPayload[i].bitNum(), osnSenderPayloadRes.get(i))))
//            .toArray(BitVector[]::new);
//        for (int i = 0; i < sigma.length; i++) {
//            if(pi[sigma[i]] < preView.inputKey.length){
//                BigInteger appSortReceiverKey = new BigInteger(appendKey[pi[sigma[i]]]);
//                if (senderMap.containsKey(appSortReceiverKey)) {
//                    assert Arrays.equals(osnRes[i].getBytes(), senderMap.get(appSortReceiverKey));
//                } else {
//                    assert Arrays.equals(osnRes[i].getBytes(), new byte[osnRes[i].byteNum()]);
//                }
//            }else{
//                assert Arrays.equals(osnRes[i].getBytes(), new byte[osnRes[i].byteNum()]);
//            }
//        }


        // 3. traversal
        stopWatch.start();
        Vector<byte[]> groupBytes = getGroupBytes(preView.inputKey);
        String[] keyStr = GroupAggUtils.bytesToBinaryString(groupBytes, keyByteLen * 8);
        SquareZ2Vector[] groupInput = Arrays.stream(ZlDatabase.create(envType, parallel, osnSenderPayload).getBytesData())
            .map(ea -> SquareZ2Vector.create(osnSenderPayload.length, ea, false))
            .toArray(SquareZ2Vector[]::new);
        PrefixAggOutput prefixAggOutput = prefixAggParty.agg(keyStr, groupInput);
        SquareZ2Vector[] groupOut = prefixAggOutput.getAggsBinary();
        stopWatch.stop();
        long traversalTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, traversalTime);

        BitVector[] selfData = new BitVector[sigma.length];
        for (int i = 0; i < sigma.length; i++) {
            int target = pi[sigma[i]];
            if (target < payload.length) {
                selfData[i] = payload[target];
            } else {
                selfData[i] = null;
            }
        }
        logPhaseInfo(PtoState.PTO_END);
        return new PkFkViewReceiverOutput(preView.inputKey, payload, preView.pi, preView.sigma,
            groupOut, selfData, preView.equalFlag, preView.mapEqualFlag, preView.senderInputSize);
    }

    private void preComp(byte[][] key, int senderSize) throws MpcAbortException {
        int receiverSize = key.length;
        // 1. key加后缀
        appendKey = PkFkUtils.addIndex(key);
        // 2. pmap
        pmapRes = pmapClient.map(Arrays.stream(appendKey).collect(Collectors.toList()), senderSize);

        Map<Integer, byte[]> resultMap = pmapRes.getIndexMap();
        // extend the permutation
        if (pmapRes.getIndexMap().size() < receiverSize) {
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
        if (pmapRes.getIndexMap().size() < senderSize) {
            for (int i = pmapRes.getIndexMap().size(); i < senderSize; i++) {
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
        for (int i = 0; i < receiverSize; i++) {
            data2pos.put(new BigInteger(appendKey[i]), i);
        }
        int dummyIndex = receiverSize;
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
        genPerm4Sort();
    }

    private void genPerm4Sort() {
        int bigNum = appendKey[0].length * 8;
        BigInteger maxNum = BigInteger.ONE.shiftLeft(bigNum);
        HashMap<BigInteger, Integer> data2pos = new HashMap<>();
        TIntArrayList intList = new TIntArrayList(pi.length - appendKey.length);
        // 排序已经置换过的key
        BigInteger[] tmp = Arrays.stream(pi).mapToObj(i -> {
            if (pi[i] < appendKey.length) {
                BigInteger v = BigIntegerUtils.byteArrayToNonNegBigInteger(appendKey[pi[i]]);
                data2pos.put(v, i);
                return v;
            } else {
                intList.add(i);
                return maxNum;
            }
        }).toArray(BigInteger[]::new);
        Arrays.sort(tmp, (x, y) -> -x.compareTo(y));
        sigma = new int[pi.length];
        for (int i = 0, other = 0; i < pi.length; i++) {
            BigInteger v = tmp[i];
            if (data2pos.contains(v)) {
                sigma[i] = data2pos.get(v).get();
            } else {
                sigma[i] = intList.get(other++);
            }
        }
    }

    private Vector<byte[]> getGroupBytes(byte[][] key) {
        byte[] defaultBytes = new byte[key[0].length];
        Arrays.fill(defaultBytes, (byte) 255);
        return IntStream.range(0, pi.length).mapToObj(i -> {
            int source = pi[sigma[i]];
            return source < key.length ? key[source] : defaultBytes;
        }).collect(Collectors.toCollection(Vector::new));
    }
}
