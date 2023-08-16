package edu.alibaba.mpc4j.s2pc.pso.psi.rt21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteMulElligatorEcc;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.filter.Filter;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory.ByteEccType.X25519_ELLIGATOR_BC;

public class Rt21ElligatorPsiClient<T> extends AbstractPsiClient<T> {
    /**
     * 字节Elligator椭圆曲线
     */
    private final ByteMulElligatorEcc byteMulElligatorEcc;
    /**
     * 客户端b Vector
     */
    private Vector<byte[]> bVector;
    /**
     * 客户端Permuted encoded b vector
     */
    private Vector<byte[]> fVector;
    /**
     * message of a
     */
    private byte[] m;
    /**
     * OKVS type
     */
    private final Gf2eDokvsFactory.Gf2eDokvsType okvsType = Gf2eDokvsType.MEGA_BIN;
    /**
     * Polynomial for encoded messages
     */
    private Gf2eDokvs<ByteBuffer> poly;

    /**
     * 服务端元素的PRF结果
     */
    Filter<byte[]> serverPrfFilter;

    public Rt21ElligatorPsiClient(Rpc clientRpc, Party serverParty, Rt21ElligatorPsiConfig config) {
        super(Rt21ElligatorPsiPtoDesc.getInstance(), clientRpc, serverParty, config);
            byteMulElligatorEcc = ByteEccFactory.createMulElligatorInstance(X25519_ELLIGATOR_BC);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        DataPacketHeader initHeader = new DataPacketHeader(
        this.encodeTaskId, getPtoDesc().getPtoId(), Rt21ElligatorPsiPtoDesc.PtoStep.SERVER_SEND_INIT.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> initPayload = rpc.receive(initHeader).getPayload();
        handleInitPayload(initPayload, maxClientElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 客户端计算并发送Polynomial(OKVS Encoding)
        List<byte[]> polyPayload = generatePolynomialPayload();
        DataPacketHeader polyHeader = new DataPacketHeader(
            this.encodeTaskId, getPtoDesc().getPtoId(), Rt21ElligatorPsiPtoDesc.PtoStep.CLIENT_SEND_POLY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(polyHeader, polyPayload));
        stopWatch.stop();
        long polyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, polyTime);

        stopWatch.start();
        DataPacketHeader peqtHeader = new DataPacketHeader(
            this.encodeTaskId, getPtoDesc().getPtoId(), Rt21ElligatorPsiPtoDesc.PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> peqtPayload = rpc.receive(peqtHeader).getPayload();
        Set<T> intersection = handlePeqtPayload(peqtPayload);

        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, peqtTime);

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
        }


    private void handleInitPayload(List<byte[]> initPayload, int maxClientElementSize) {
        // 构造交换映射
        int prpNum = byteMulElligatorEcc.pointByteLength() / CommonConstants.BLOCK_BYTE_LENGTH;
        List<Prp> prpList = IntStream.range(0, prpNum).mapToObj(index -> {
            Prp prp = PrpFactory.createInstance(envType);
            prp.setKey(initPayload.get(index));
            return prp;
        }).collect(Collectors.toList());
        // 构造 {msg(b)}
        this.m = initPayload.get(prpNum);
        this.bVector = IntStream.range(0, maxClientElementSize).mapToObj(index ->
            new byte[byteMulElligatorEcc.scalarByteLength()])
            .collect(Collectors.toCollection(Vector::new));
        // m' = KA.msg(b, m)
        // f = invPrp (m')
        IntStream bVectorStream = IntStream.range(0,maxClientElementSize);
        bVectorStream = parallel ? bVectorStream.parallel() : bVectorStream;
        this.fVector = bVectorStream
            .mapToObj(bIndex -> {
                byte[] mPrime = Rt21ElligatorPsiUtils.generateKaMessage(byteMulElligatorEcc, bVector.get(bIndex), secureRandom);
                byte[] f = new byte[mPrime.length];
                byte[] src = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                for(int i = 0; i < prpNum; i++){
                    System.arraycopy(mPrime, i * CommonConstants.BLOCK_BYTE_LENGTH, src, 0, CommonConstants.BLOCK_BYTE_LENGTH);
                    byte[] dst = prpList.get(i).invPrp(src);
                    System.arraycopy(dst, 0, f, i * CommonConstants.BLOCK_BYTE_LENGTH, CommonConstants.BLOCK_BYTE_LENGTH);
                }
                return f;
            }).collect(Collectors.toCollection(Vector::new));
        // 初始化OKVS
        byte[][] okvsKeys = initPayload.subList(prpNum + 1, initPayload.size()).toArray(new byte[0][]);
        this.poly = Gf2eDokvsFactory.createInstance(envType, okvsType, Math.max(2, maxClientElementSize), byteMulElligatorEcc.pointByteLength() * Byte.SIZE, okvsKeys);
    }

    private List<byte[]> generatePolynomialPayload() {
//        // P = encode ( H1(y). f )
//        Map<ByteBuffer, byte[]> map = new HashMap<>();
//        IntStream.range(0, clientElementSize).forEach(index -> {
//            byte[] h1y = byteMulElligatorEcc.hashToCurve(ObjectUtils.objectToByteArray(clientElementArrayList.get(index)));
//            map.put(ByteBuffer.wrap(h1y), fVector.get(index));
//        });
//        return Arrays.asList(poly.encode(map));
        return null;
    }

    private Set<T> handlePeqtPayload(List<byte[]> peqtPayload) throws MpcAbortException {
        try {
            this.serverPrfFilter = FilterFactory.createFilter(envType, peqtPayload);
        } catch (IllegalArgumentException e) {
            throw new MpcAbortException();
        }

        //  初始化PEQT哈希 {0, 1}^* * F -> {0, 1}^2k
        int peqtByteLength = 2 * CommonConstants.BLOCK_BYTE_LENGTH;
        Hash keyHash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);

        IntStream elementStream = IntStream.range(0, clientElementSize);
        elementStream = parallel ? elementStream.parallel() : elementStream;
        Set<T> intersection = elementStream.mapToObj(index -> {
            T element = clientElementArrayList.get(index);
            Prf peqtPrf = PrfFactory.createInstance(envType, peqtByteLength);
            peqtPrf.setKey(keyHash.digestToBytes(Rt21ElligatorPsiUtils.generateKaKey(byteMulElligatorEcc, m, bVector.get(index))));
            return serverPrfFilter.mightContain(peqtPrf.getBytes(ObjectUtils.objectToByteArray(element))) ? element : null;
        }).filter(Objects::nonNull).collect(Collectors.toSet());
        serverPrfFilter = null;
        return intersection;
    }
}
