package edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.AbstractBit2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21.Kvh21Bit2aPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * KVH+21 Bit2a Receiver.
 *
 * @author Li Peng
 * @date 2023/10/12
 */
public class Kvh21Bit2aReceiver extends AbstractBit2aParty {
    /**
     * COT receiver.
     */
    private final CotReceiver cotReceiver;
    /**
     * Z2 circuit party.
     */
    private final Z2cParty z2cReceiver;

    public Kvh21Bit2aReceiver(Rpc rpc, Party otherParty, Kvh21Bit2aConfig config) {
        super(Kvh21Bit2aPtoDesc.getInstance(), rpc, otherParty, config);
        cotReceiver = CotFactory.createReceiver(rpc, otherParty, config.getCotConfig());
        z2cReceiver = Z2cFactory.createReceiver(rpc, otherParty, config.getZ2cConfig());
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        cotReceiver.init(maxNum);
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
        byte[] x0Bytes = xi.getBitVector().getBytes();
        boolean[] x0Binary = BinaryUtils.byteArrayToBinary(x0Bytes, num);
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(x0Binary);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, ptoTime);

        // receive payload and decrypt
        stopWatch.start();
        DataPacketHeader t0t1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PAYLOADS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> t0t1Payload = rpc.receive(t0t1Header).getPayload();
        SquareZlVector result = t0t1(cotReceiverOutput, t0t1Payload);

        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, ptoTime);

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    @Override
    public SquareZlVector[] bit2a(SquareZ2Vector[] xiArray) throws MpcAbortException {
        // merge
        SquareZ2Vector mergedXiArray = (SquareZ2Vector) z2cReceiver.merge(xiArray);
        // bit2a
        SquareZlVector mergedZiArray = bit2a(mergedXiArray);
        // split
        int[] nums = Arrays.stream(xiArray)
            .mapToInt(SquareZ2Vector::getNum).toArray();

        return Arrays.stream(split(mergedZiArray.getZlVector(), nums))
            .map(z -> SquareZlVector.create(z, false)).toArray(SquareZlVector[]::new);
    }

    /**
     * splits the zl vector.
     *
     * @param mergedZlVector the merged zl vector.
     * @param nums           number for each of the split vector.
     * @return the split zl vectors.
     */
    public static ZlVector[] split(ZlVector mergedZlVector, int[] nums) {
        ZlVector[] zlVectors = new ZlVector[nums.length];
        for (int index = 0; index < nums.length; index++) {
            zlVectors[index] = mergedZlVector.split(nums[index]);
        }
        assert mergedZlVector.getNum() == 0 : "merged vector must remain 0 element: " + mergedZlVector.getNum();
        return zlVectors;
    }

    private SquareZlVector t0t1(CotReceiverOutput cotReceiverOutput, List<byte[]> t0t1Payload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(t0t1Payload.size() == num * 2);
        byte[][] t0s = t0t1Payload.subList(0, num).toArray(new byte[0][]);
        byte[][] t1s = t0t1Payload.subList(num, num * 2).toArray(new byte[0][]);
        Prg prg = PrgFactory.createInstance(envType, byteL);
        // Let P0's output be a0
        IntStream t0IntStream = IntStream.range(0, num);
        t0IntStream = parallel ? t0IntStream.parallel() : t0IntStream;
        BigInteger[] a0s = t0IntStream
            .mapToObj(index -> {
                boolean x0 = cotReceiverOutput.getChoice(index);
                byte[] a0 = prg.extendToBytes(cotReceiverOutput.getRb(index));
                if (!x0) {
                    BytesUtils.xori(a0, t0s[index]);
                } else {
                    BytesUtils.xori(a0, t1s[index]);
                }
                return a0;
            })
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);
        ZlVector z0ZlVector = ZlVector.create(zl, a0s);
        return SquareZlVector.create(z0ZlVector, false);
    }
}
