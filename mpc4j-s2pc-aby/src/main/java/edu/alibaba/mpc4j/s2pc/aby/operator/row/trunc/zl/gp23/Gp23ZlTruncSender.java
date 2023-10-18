package edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.gp23;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.AbstractZlTruncParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotSenderOutput;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.gp23.Gp23ZlTruncPtoDesc.*;

/**
 * GP23 Zl Truncation Sender.
 *
 * @author Liqiang Peng
 * @date 2023/10/2
 */
public class Gp23ZlTruncSender extends AbstractZlTruncParty {
    /**
     * z2 circuit sender.
     */
    private final Z2cParty z2cSender;
    /**
     * cot sender
     */
    private final CotSender cotSender;

    public Gp23ZlTruncSender(Rpc senderRpc, Party receiverParty, Gp23ZlTruncConfig config) {
        super(getInstance(), senderRpc, receiverParty, config);
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        addSubPtos(z2cSender);
        cotSender = CotFactory.createSender(senderRpc, receiverParty, config.getCotConfig());
        addSubPtos(cotSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        z2cSender.init(maxL * maxNum);
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, 2 * maxL * maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector trunc(SquareZlVector xi, int s) throws MpcAbortException {
        setPtoInput(xi, s);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        BitVector[] i0 = getIi(xi);
        MpcZ2Vector k0 = generateK0Share(i0);
        MpcZ2Vector k1 = generateK1Share(i0);
        stopWatch.stop();
        long genKiShareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, genKiShareTime);

        stopWatch.start();
        int[] rs0 = booleanShareToArithShare(k0);
        int[] rs1 = booleanShareToArithShare(k1);
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        BigInteger[] k = intStream.mapToObj(index -> {
            int value = (k0.getBitVector().get(index) ? 1 : 0) + (k1.getBitVector().get(index) ? 1 : 0);
            return BigInteger.valueOf(value + (rs0[index] + rs1[index]) * 2L).mod(zl.getRangeBound());
        }).toArray(BigInteger[]::new);
        ZlVector kShare = ZlVector.create(zl, k);
        kShare.setParallel(parallel);
        stopWatch.stop();
        long shareConvertTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, shareConvertTime);

        stopWatch.start();
        BigInteger[] r1 = IntStream.range(0, num)
            .mapToObj(i -> BigInteger.ONE.shiftLeft(l - s))
            .toArray(BigInteger[]::new);
        ZlVector shift = ZlVector.create(zl, r1);
        BigInteger[] r2 = iDiv(xi.getZlVector().getElements(), s);
        ZlVector r = ZlVector.create(zl, r2);
        r.setParallel(parallel);
        r.subi(kShare.mul(shift));
        SquareZlVector squareZlVector = SquareZlVector.create(r, false);
        stopWatch.stop();
        long genOutputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, genOutputTime);

        logPhaseInfo(PtoState.PTO_END);
        return squareZlVector;
    }

    private BitVector[] getIi(SquareZlVector xi) {
        BigInteger b1 = zl.getRangeBound().divide(BigInteger.valueOf(3));
        BigInteger b2 = zl.getRangeBound().shiftLeft(1).divide(BigInteger.valueOf(3)).add(BigInteger.ONE);
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        int[][] i0 = intStream.mapToObj(index -> {
            BigInteger x = xi.getZlVector().getElement(index);
            if (x.compareTo(b1) <= 0) {
                return new int[]{0, 0};
            } else if (x.compareTo(b2) > 0) {
                return new int[]{1, 0};
            } else {
                return new int[]{0, 1};
            }
        }).toArray(int[][]::new);
        BitVector a = BitVectorFactory.createZeros(num);
        BitVector b = BitVectorFactory.createZeros(num);
        for (int index = 0; index < num; index++) {
            if (i0[index][0] == 1) {
                a.set(index, true);
            }
            if (i0[index][1] == 1) {
                b.set(index, true);
            }
        }
        return new BitVector[]{a, b};
    }

    private MpcZ2Vector generateK0Share(BitVector[] i0) throws MpcAbortException {
        MpcZ2Vector z0 = SquareZ2Vector.create(i0[0].and(i0[1].not()), false);
        MpcZ2Vector z1 = SquareZ2Vector.create(BitVectorFactory.createZeros(num), false);
        return z2cSender.and(z0, z1);
    }

    private MpcZ2Vector generateK1Share(BitVector[] i0) throws MpcAbortException {
        MpcZ2Vector z0 = SquareZ2Vector.create(i0[0].not().and(i0[1].not()), false);
        MpcZ2Vector z1 = SquareZ2Vector.create(BitVectorFactory.createZeros(num), false);
        MpcZ2Vector z2 = SquareZ2Vector.create(i0[0].not().and(i0[1]), false);
        MpcZ2Vector z3 = SquareZ2Vector.create(BitVectorFactory.createZeros(num), false);
        MpcZ2Vector z4 = SquareZ2Vector.create(i0[0].and(i0[1].not()), false);
        MpcZ2Vector z5 = SquareZ2Vector.create(BitVectorFactory.createZeros(num), false);
        MpcZ2Vector[] z = z2cSender.and(new MpcZ2Vector[]{z0, z2, z4}, new MpcZ2Vector[]{z1, z3, z5});
        return z2cSender.xor(z2cSender.xor(z[0], z[1]), z[2]);
    }

    private BigInteger[] iDiv(BigInteger[] input, int d) {
        int num = input.length;
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream.mapToObj(index -> input[index].shiftRight(d)).toArray(BigInteger[]::new);
    }

    private int[] booleanShareToArithShare(MpcZ2Vector k) throws MpcAbortException {
        // P_1 and P_2 engage in a OT_1^2, where P_1 acts as the sender, P_1's input is (r_i, r_i + x_i).
        CotSenderOutput cotSenderOutput = cotSender.send(num);
        RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfFactory.CrhfType.MMO, cotSenderOutput);
        int messageByteLength = IntUtils.boundedNonNegIntByteLength(num);
        int offset = CommonUtils.getByteLength(num) * Byte.SIZE - num;
        // P_1 generates n random values r_1, ... r_n \in Z_{n + 1} and computes r = Σ_{i = 1}^n t_i
        int[] rs = new int[num];
        IntStream.range(0, num).forEach(index -> rs[index] = secureRandom.nextInt(2));
        List<byte[]> senderMessagePayload = IntStream.range(0, num)
            .mapToObj(index -> {
                byte[] key0 = Arrays.copyOf(rotSenderOutput.getR0(index), messageByteLength);
                byte[] key1 = Arrays.copyOf(rotSenderOutput.getR1(index), messageByteLength);
                int rxi = BinaryUtils.getBoolean(k.getBitVector().getBytes(), index + offset) ? 1 : 0;
                int negRxi = (rxi + rs[index]);
                byte[][] ciphertexts = new byte[2][];
                ciphertexts[0] = IntUtils.nonNegIntToFixedByteArray(rs[index], messageByteLength);
                ciphertexts[1] = IntUtils.nonNegIntToFixedByteArray(negRxi, messageByteLength);
                BytesUtils.xori(ciphertexts[0], key0);
                BytesUtils.xori(ciphertexts[1], key1);
                return ciphertexts;
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        DataPacketHeader senderMessageHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_S.ordinal(), extraInfo++,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(senderMessageHeader, senderMessagePayload));
        return rs;
    }
}
