package edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.AbstractBit2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21.Kvh21Bit2aPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * KVH+21 Bit2a Sender.
 *
 * @author Li Peng
 * @date 2023/10/12
 */
public class Kvh21Bit2aSender extends AbstractBit2aParty {
    /**
     * COT sender.
     */
    private final CotSender cotSender;
    /**
     * Z2 circuit party.
     */
    private final Z2cParty z2cSender;

    public Kvh21Bit2aSender(Rpc rpc, Party otherParty, Kvh21Bit2aConfig config) {
        super(Kvh21Bit2aPtoDesc.getInstance(), rpc, otherParty, config);
        cotSender = CotFactory.createSender(rpc, otherParty, config.getCotConfig());
        z2cSender = Z2cFactory.createSender(rpc, otherParty, config.getZ2cConfig());
        addMultipleSubPtos(cotSender, z2cSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, maxNum);
        z2cSender.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector bit2a(SquareZ2Vector xi) throws MpcAbortException {
        setPtoInput(xi);

        logPhaseInfo(PtoState.PTO_BEGIN);

        // cot
        stopWatch.start();
        CotSenderOutput cotSenderOutput = cotSender.send(num);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, ptoTime);

        // ot payload
        stopWatch.start();
        SquareZlVector r = t0t1(cotSenderOutput);
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, ptoTime);

        logPhaseInfo(PtoState.PTO_END);
        return r;
    }

    @Override
    public SquareZlVector[] bit2a(SquareZ2Vector[] xiArray) throws MpcAbortException {
        // merge
        SquareZ2Vector mergedXiArray = (SquareZ2Vector) z2cSender.merge(xiArray);
        // bit2a
        SquareZlVector mergedZiArray = bit2a(mergedXiArray);
        // split
        int[] nums = Arrays.stream(xiArray)
            .mapToInt(SquareZ2Vector::getNum).toArray();

        return Arrays.stream(ZlVector.split(mergedZiArray.getZlVector(), nums))
            .map(z -> SquareZlVector.create(z, false)).toArray(SquareZlVector[]::new);
    }

    private SquareZlVector t0t1(CotSenderOutput cotSenderOutput) {
        Prg prg = PrgFactory.createInstance(envType, byteL);
        BitVector inputBits = input.getBitVector();
        // generate random rs
        BigInteger[] rs = ZlVector.createRandom(zl, num, secureRandom).getElements();
        byte[][] r0s = IntStream.range(0, num)
            .mapToObj(i -> zl.add(rs[i], inputBits.get(i) ? BigInteger.ONE : BigInteger.ZERO))
            .map(r -> BigIntegerUtils.nonNegBigIntegerToByteArray(r, byteL))
            .toArray(byte[][]::new);
        byte[][] r1s = IntStream.range(0, num)
            .mapToObj(i -> zl.add(rs[i], inputBits.get(i) ? BigInteger.ZERO : BigInteger.ONE))
            .map(r -> BigIntegerUtils.nonNegBigIntegerToByteArray(r, byteL))
            .toArray(byte[][]::new);
        // P1 creates t0
        IntStream t0IntStream = IntStream.range(0, num);
        t0IntStream = parallel ? t0IntStream.parallel() : t0IntStream;
        List<byte[]> t0t1Payload = t0IntStream
            .mapToObj(index -> {
                // key0
                byte[] t0 = prg.extendToBytes(cotSenderOutput.getR0(index));
                BytesUtils.xori(t0, r0s[index]);
                return t0;
            })
            .collect(Collectors.toList());
        // P1 creates t1
        IntStream t1IntStream = IntStream.range(0, num);
        t1IntStream = parallel ? t1IntStream.parallel() : t1IntStream;
        List<byte[]> t1Payload = t1IntStream
            .mapToObj(index -> {
                // key1
                byte[] t1 = prg.extendToBytes(cotSenderOutput.getR1(index));
                BytesUtils.xori(t1, r1s[index]);
                return t1;
            })
            .collect(Collectors.toList());
        // merge t0 and t1
        t0t1Payload.addAll(t1Payload);
        // sends s0 and s1
        DataPacketHeader t0t1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PAYLOADS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(t0t1Header, t0t1Payload));
        // -r as output
        return SquareZlVector.create(zl, Arrays.stream(rs).map(r -> zl.neg(r)).toArray(BigInteger[]::new), false);
    }
}
