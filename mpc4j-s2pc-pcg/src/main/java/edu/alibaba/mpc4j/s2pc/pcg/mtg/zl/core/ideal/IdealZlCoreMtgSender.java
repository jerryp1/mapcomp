package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ideal;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlTriple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.AbstractZlCoreMtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ideal.IdealZlCoreMtgPtoDesc.PtoStep;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 理想核l比特三元组生成协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public class IdealZlCoreMtgSender extends AbstractZlCoreMtgParty {
    /**
     * 生成a0的伪随机函数
     */
    private Prf a0Prf;
    /**
     * 生成b0的伪随机函数
     */
    private Prf b0Prf;
    /**
     * 生成c0的伪随机函数
     */
    private Prf c0Prf;

    public IdealZlCoreMtgSender(Rpc senderRpc, Party receiverParty, IdealZlCoreMtgConfig config) {
        super(IdealZlCoreMtgPtoDesc.getInstance(), senderRpc, receiverParty, config);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] rootKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(rootKey);
        List<byte[]> rootKeyPayload = new LinkedList<>();
        rootKeyPayload.add(rootKey);
        DataPacketHeader rootKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ROOT_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(rootKeyHeader, rootKeyPayload));
        stopWatch.stop();
        long rootKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, rootKeyTime);

        stopWatch.start();
        Kdf kdf = KdfFactory.createInstance(envType);
        byte[] key = kdf.deriveKey(rootKey);
        a0Prf = PrfFactory.createInstance(envType, byteL);
        a0Prf.setKey(key);
        key = kdf.deriveKey(key);
        b0Prf = PrfFactory.createInstance(envType, byteL);
        b0Prf.setKey(key);
        key = kdf.deriveKey(key);
        c0Prf = PrfFactory.createInstance(envType, byteL);
        c0Prf.setKey(key);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public ZlTriple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        ZlTriple senderOutput = generateZlTriple();
        stopWatch.stop();
        long generateTripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, generateTripleTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private ZlTriple generateZlTriple() {
        BigInteger[] a0s = new BigInteger[num];
        BigInteger[] b0s = new BigInteger[num];
        BigInteger[] c0s = new BigInteger[num];
        IntStream numIntStream = IntStream.range(0, num);
        numIntStream = parallel ? numIntStream.parallel() : numIntStream;
        numIntStream.forEach(index -> {
            byte[] seed = ByteBuffer.allocate(Long.BYTES + Integer.BYTES)
                .putLong(extraInfo)
                .putInt(index)
                .array();
            // 生成a0，b0，c0
            byte[] a0Bytes = a0Prf.getBytes(seed);
            a0s[index] = BigIntegerUtils.byteArrayToBigInteger(a0Bytes).and(mask);
            byte[] b0Bytes = b0Prf.getBytes(seed);
            b0s[index] = BigIntegerUtils.byteArrayToBigInteger(b0Bytes).and(mask);
            byte[] c0Bytes = c0Prf.getBytes(seed);
            c0s[index] = BigIntegerUtils.byteArrayToBigInteger(c0Bytes).and(mask);
        });

        return ZlTriple.create(envType, l, num, a0s, b0s, c0s);
    }
}
