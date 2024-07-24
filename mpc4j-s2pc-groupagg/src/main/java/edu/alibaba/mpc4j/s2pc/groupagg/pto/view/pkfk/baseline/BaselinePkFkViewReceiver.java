package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.baseline;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggUtils;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkUtils;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewReceiverOutput;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewReceiver;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiClientOutput;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Feng Han
 * @date 2024/7/19
 */
public class BaselinePkFkViewReceiver extends AbstractTwoPartyPto implements PkFkViewReceiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaselinePkFkViewReceiver.class);
    private final Z2MuxParty z2MuxParty;
    private final PlpsiClient<byte[]> plpsiClient;
    private final OsnReceiver osnReceiver;
    private final PrefixAggParty prefixAggParty;

    public BaselinePkFkViewReceiver(Rpc rpc, Party senderParty, BaselinePkFkViewConfig config) {
        super(BaselinePkFkViewPtoDesc.getInstance(), rpc, senderParty, config);
        z2MuxParty = Z2MuxFactory.createReceiver(rpc, senderParty, config.getZ2MuxConfig());
        plpsiClient = PlpsiFactory.createClient(rpc, senderParty, config.getPlpsiConfig());
        osnReceiver = OsnFactory.createReceiver(rpc, senderParty, config.getOsnConfig());
        prefixAggParty = PrefixAggFactory.createPrefixAggReceiver(rpc, senderParty, config.getPrefixAggConfig());
        addMultipleSubPtos(z2MuxParty, plpsiClient, prefixAggParty, osnReceiver);
    }

    @Override
    public void init(int senderPayloadBitLen, int senderSize, int receiverSize) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        assert senderPayloadBitLen * senderSize > 0;
        z2MuxParty.init(receiverSize * 20);
        plpsiClient.init(receiverSize, senderSize);
        osnReceiver.init(receiverSize * 20);
        prefixAggParty.init(256, receiverSize * 20);
        initState();

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PkFkViewReceiverOutput generate(byte[][] key, BitVector[] payload, int senderSize, int senderPayloadBitLen) throws MpcAbortException {
        return innerCommon(key, payload, senderSize, senderPayloadBitLen);
    }

    @Override
    public PkFkViewReceiverOutput refresh(PkFkViewReceiverOutput preView, BitVector[] payload) throws MpcAbortException {
        return innerCommon(preView.inputKey, payload, preView.senderInputSize, preView.shareData.length);
    }

    private PkFkViewReceiverOutput innerCommon(byte[][] key, BitVector[] payload, int senderSize, int senderPayloadBitLen) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        int keyByteLen = key[0].length;
        assert key.length == payload.length;

        // 1. key加后缀 Payload psi
        stopWatch.start();
        byte[][] appendKey = PkFkUtils.addIndex(key);
        PlpsiClientOutput<byte[]> psiRes = plpsiClient.psiWithPayload(Arrays.stream(appendKey).collect(Collectors.toList()),
            senderSize, new int[]{senderPayloadBitLen}, new boolean[]{true});
        byte[][] allPsiKey = psiRes.getTable().toArray(new byte[0][]);
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

//        // debug
//        int payloadByteLen = CommonUtils.getByteLength(sharePayload.length);
//        List<byte[]> senderKey = receiveOtherPartyPayload(PtoStep.DEBUG.ordinal());
//        List<byte[]> senderPayload = receiveOtherPartyPayload(PtoStep.DEBUG.ordinal());
//        List<byte[]> senderShares = receiveOtherPartyPayload(PtoStep.DEBUG.ordinal());
//        assert senderShares.size() == sharePayload.length;
//        // 恢复出share的payload
//        BitVector[] joinPayload = new BitVector[sharePayload.length];
//        for (int i = 0; i < joinPayload.length; i++) {
//            joinPayload[i] = sharePayload[i].getBitVector().xor(BitVectorFactory.create(sharePayload[i].bitNum(), senderShares.get(i)));
//        }
//        byte[][] actualPayload = ZlDatabase.create(envType, parallel, joinPayload).getBytesData();
//        HashMap<BigInteger, byte[]> senderMap = new HashMap<>();
//        for (int i = 0; i < senderKey.size(); i++) {
//            byte[] originalKey = senderKey.get(i);
//            byte[] tmpKey = new byte[originalKey.length + 4];
//            System.arraycopy(originalKey, 0, tmpKey, 0, originalKey.length);
//            senderMap.put(new BigInteger(tmpKey), senderPayload.get(i));
//        }
//        for (int i = 0; i < allPsiKey.length; i++) {
//            if (allPsiKey[i] == null) {
//                assert Arrays.equals(actualPayload[i], new byte[payloadByteLen]);
//            } else if (senderMap.containsKey(new BigInteger(allPsiKey[i]))) {
//                assert Arrays.equals(actualPayload[i], senderMap.get(new BigInteger(allPsiKey[i])));
//            } else {
//                assert Arrays.equals(actualPayload[i], new byte[payloadByteLen]);
//            }
//        }

        // 3. osn
        stopWatch.start();
        // 3.1 sort based on psiRes result
        HashMap<BigInteger, Integer> map2PsiRes = new HashMap<>();
        for (int i = 0; i < appendKey.length; i++) {
            map2PsiRes.put(new BigInteger(appendKey[i]), i);
        }
        BigInteger maxNum = BigInteger.ONE.shiftLeft(key[0].length * 8 + 32);
        BigInteger[] sortData = new BigInteger[psiRes.getBeta()];

        int[] pi = new int[psiRes.getBeta()];
        int startPos = appendKey.length;
        for (int i = 0; i < sortData.length; i++) {
            if (allPsiKey[i] == null) {
                sortData[i] = maxNum.add(BigInteger.valueOf(i));
                pi[i] = startPos++;
            } else {
                sortData[i] = BigIntegerUtils.byteArrayToNonNegBigInteger(allPsiKey[i]).shiftLeft(32).add(BigInteger.valueOf(i));
                pi[i] = map2PsiRes.get(new BigInteger(allPsiKey[i]));
            }
        }
        assert startPos == sortData.length;

        Arrays.sort(sortData, (x, y) -> -x.compareTo(y));
        int[] sigma = Arrays.stream(sortData).mapToInt(BigInteger::intValue).toArray();
        // 3.2 sort payload
        BitVector[] selfPayload = IntStream.range(0, pi.length).mapToObj(i -> pi[sigma[i]] >= key.length ? null : payload[pi[sigma[i]]]).toArray(BitVector[]::new);
        // 3.3 osn the other payload
        byte[][] sharePayloadInRow = ZlDatabase.create(envType, parallel,
                Arrays.stream(sharePayload).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new))
            .getBytesData();
        byte[][] osnInput = new byte[sharePayloadInRow.length][];
        for (int i = 0; i < osnInput.length; i++) {
            osnInput[i] = new byte[sharePayloadInRow[i].length + 1];
            osnInput[i][0] = (byte) (psiRes.getZ1().getBitVector().get(i) ? 1 : 0);
            System.arraycopy(sharePayloadInRow[i], 0, osnInput[i], 1, sharePayloadInRow[i].length);
        }
        OsnPartyOutput osnPartyOutput = osnReceiver.osn(sigma, new Vector<>(Arrays.stream(osnInput).collect(Collectors.toList())), osnInput[0].length);

        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, osnTime);


//        // debug osn
//        List<byte[]> osnSenderRes = receiveOtherPartyPayload(PtoStep.DEBUG.ordinal());
//        byte[][] osnRes = IntStream.range(0, osnSenderRes.size())
//            .mapToObj(i -> BytesUtils.xor(osnSenderRes.get(i), osnPartyOutput.getShare(i)))
//            .toArray(byte[][]::new);
//        HashMap<BigInteger, byte[]> receiverKey2SharePayload = new HashMap<>();
//        for (int i = 0; i < allPsiKey.length; i++) {
//            if (allPsiKey[i] != null) {
//                receiverKey2SharePayload.put(new BigInteger(allPsiKey[i]), actualPayload[i]);
//            }
//        }
//        for (int i = 0; i < sortData.length; i++) {
//            BigInteger appSortReceiverKey = sortData[i].shiftRight(32);
//            byte[] osnResPayload = Arrays.copyOfRange(osnRes[i], 1, osnRes[i].length);
//            if (receiverKey2SharePayload.containsKey(appSortReceiverKey)) {
//                assert (osnRes[i][0] & 1) == (senderMap.containsKey(appSortReceiverKey) ? 1 : 0);
//                assert Arrays.equals(osnResPayload, receiverKey2SharePayload.get(appSortReceiverKey));
//            } else {
//                assert (osnRes[i][0] & 1) == 0;
//                assert Arrays.equals(osnResPayload, new byte[osnResPayload.length]);
//            }
//        }


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

        Vector<byte[]> keyBytes = Arrays.stream(sortData)
            .map(ea -> BigIntegerUtils.nonNegBigIntegerToByteArray(ea.shiftRight(64), keyByteLen))
            .collect(Collectors.toCollection(Vector::new));
        String[] keyStr = GroupAggUtils.bytesToBinaryString(keyBytes, keyByteLen * 8);
        PrefixAggOutput prefixAggOutput = prefixAggParty.agg(keyStr, duplicateInput);
        SquareZ2Vector[] duplicateRes = prefixAggOutput.getAggsBinary();
        SquareZ2Vector[] forTrans = Arrays.copyOf(duplicateRes, sharePayload.length);
        SquareZ2Vector finalEqualFlag = duplicateRes[sharePayload.length];

        PkFkViewReceiverOutput output = new PkFkViewReceiverOutput(key, payload,
            pi, sigma,
            forTrans, selfPayload, finalEqualFlag, psiRes.getZ1(), senderSize
        );
        stopWatch.stop();
        long transProcess = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, transProcess);

        logPhaseInfo(PtoState.PTO_END);
        return output;
    }
}
