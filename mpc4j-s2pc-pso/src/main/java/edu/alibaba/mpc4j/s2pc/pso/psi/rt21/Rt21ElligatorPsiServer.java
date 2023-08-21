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
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Rt21ElligatorPsiServer<T> extends AbstractPsiServer<T> {
    /**
     * 字节Elligator椭圆曲线
     */
    private final ByteMulElligatorEcc byteMulElligatorEcc;
    /**
     * 客户端密钥β
     */
    private byte[] a;
    /**
     * 交换映射
     */
    private List<Prp> prpList;
    /**
     *  交换映射num
     */
    private int prpNum;
    /**
     * OKVS type
     */
    private final Gf2eDokvsFactory.Gf2eDokvsType okvsType;
    /**
     * Polynomial for encoded messages
     */
    private Gf2eDokvs<ByteBuffer> dOkvs;
    /**
     * OKVS keys
     */
    private byte[][] okvsKeys;
    /**
     * hash函数 对应论文fig4中的h1
     */
    private final Hash h1;
    /**
     * 过滤器类型
     */
    private final FilterFactory.FilterType filterType;

    public Rt21ElligatorPsiServer(Rpc serverRpc, Party clientParty, Rt21ElligatorPsiConfig config) {
        super(Rt21ElligatorPsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        byteMulElligatorEcc = ByteEccFactory.createMulElligatorInstance(ByteEccFactory.ByteEccType.X25519_ELLIGATOR_BC);
        filterType = config.getFilterType();
        okvsType = config.getOkvsType();
        h1 = HashFactory.createInstance(envType, byteMulElligatorEcc.pointByteLength());
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 初始化OKVS
        this.okvsKeys = CommonUtils.generateRandomKeys(Gf2eDokvsFactory.getHashKeyNum(okvsType), secureRandom);
        this.dOkvs = Gf2eDokvsFactory.createInstance(envType, okvsType, Math.max(2, maxClientElementSize),
            byteMulElligatorEcc.pointByteLength() * Byte.SIZE, okvsKeys);
        List<byte[]> initPayload = generateInitPayload();
        DataPacketHeader initHeader = new DataPacketHeader(
                this.encodeTaskId, getPtoDesc().getPtoId(), Rt21ElligatorPsiPtoDesc.PtoStep.SERVER_SEND_INIT.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(initHeader, initPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }


    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();

        // 服务端接收Poly
        DataPacketHeader polyHeader = new DataPacketHeader(
                this.encodeTaskId, getPtoDesc().getPtoId(), Rt21ElligatorPsiPtoDesc.PtoStep.CLIENT_SEND_POLY.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> polyPayload = rpc.receive(polyHeader).getPayload();
        assert polyPayload.size() > 1;
        stopWatch.stop();
        long polyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, polyTime);

        stopWatch.start();
        // 服务端计算并发送K
        List<byte[]> peqtPayload = generatePeqtPayload(polyPayload);
        DataPacketHeader peqtHeader = new DataPacketHeader(
                this.encodeTaskId, getPtoDesc().getPtoId(), Rt21ElligatorPsiPtoDesc.PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(peqtHeader, peqtPayload));
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, peqtTime);

        logPhaseInfo(PtoState.PTO_END);
    }

    private List<byte[]> generateInitPayload() {
        // 生成msg1a
        a = new byte[byteMulElligatorEcc.scalarByteLength()];
        byte[] m = Rt21ElligatorPsiUtils.generateKaMessage(byteMulElligatorEcc, a, secureRandom);
        // 构造交换映射
        assert m.length % CommonConstants.BLOCK_BYTE_LENGTH == 0;
        this.prpNum = m.length / CommonConstants.BLOCK_BYTE_LENGTH;
        byte[][] prpKeys = CommonUtils.generateRandomKeys(prpNum, secureRandom);
        this.prpList = IntStream.range(0, prpNum)
                .mapToObj(index -> {
                    Prp prp = PrpFactory.createInstance(envType);
                    prp.setKey(prpKeys[index]);
                    return prp;
                })
                .collect(Collectors.toList());
        // 构造InitPayload
        List<byte[]> initPayload = new ArrayList<>();
        initPayload.addAll(Arrays.asList(prpKeys));
        initPayload.add(m);
        initPayload.addAll(Arrays.asList(okvsKeys));
        return initPayload;
    }

    private List<byte[]> generatePeqtPayload(List<byte[]> polyPayload){
        byte[][] storage = polyPayload.toArray(new byte[0][]);
        Hash keyHash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        int peqtByteLength = 2 * CommonConstants.BLOCK_BYTE_LENGTH;
        Stream<T> elementStream = serverElementArrayList.stream();
        elementStream = parallel ? elementStream.parallel() : elementStream;
        List<byte[]> peqtList = elementStream.map(element -> {
            byte[] binaryElement = ObjectUtils.objectToByteArray(element);
            //  P(H1(x))
            byte[] ph1x = dOkvs.decode(storage, ByteBuffer.wrap(h1.digestToBytes(binaryElement)));
            // prp(P(H1(x)))
            ByteBuffer permutedPh1xByteBuffer = ByteBuffer.allocate(ph1x.length);
            IntStream.range(0, prpNum).forEach(i -> permutedPh1xByteBuffer.put(prpList.get(i).prp(Arrays.copyOfRange(ph1x,
                i * CommonConstants.BLOCK_BYTE_LENGTH, (i + 1) * CommonConstants.BLOCK_BYTE_LENGTH))));
            // K = H2(x, KA.key(a, prp(P(H1(x))))
            Prf peqtPrf = PrfFactory.createInstance(envType, peqtByteLength);
            peqtPrf.setKey(keyHash.digestToBytes(Rt21ElligatorPsiUtils.generateKaKey(byteMulElligatorEcc, permutedPh1xByteBuffer.array(), a)));
            return peqtPrf.getBytes(binaryElement);
        }).collect(Collectors.toList());
        Collections.shuffle(peqtList, secureRandom);
        // 构建过滤器
        Filter<byte[]> peqtFilter = FilterFactory.createFilter(envType, filterType, serverElementSize, secureRandom);
        peqtList.forEach(peqtFilter::put);
        return peqtFilter.toByteArrayList();
    }

}
