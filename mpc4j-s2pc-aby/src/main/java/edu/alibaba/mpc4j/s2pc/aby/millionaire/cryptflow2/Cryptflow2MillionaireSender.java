package edu.alibaba.mpc4j.s2pc.aby.millionaire.cryptflow2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.millionaire.AbstractMillionaireParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Cryptflow2 Millionaire Protocol Sender.
 *
 * @author Li Peng
 * @date 2023/4/24
 */
public class Cryptflow2MillionaireSender extends AbstractMillionaireParty {
    /**
     * 1-out-of-n (with n = 2^l) ot sender.
     */
    private final LnotSender lnotSender;
    /**
     * boolean circuit sender.
     */
    private final BcParty bcSender;
    /**
     * the bit length of substring of input value.
     */
    private final int m = 4;
    /**
     * the num of substring.
     */
    private int q;
    /**
     * the num of elements in a vector.
     */
    private int num;
    /**
     * the input value of sender.
     */
    private ZlVector inputs;


    public Cryptflow2MillionaireSender(Rpc senderRpc, Party receiverParty, Cryptflow2MillionaireConfig config) {
        super(Cryptflow2MillionairePtoDesc.getInstance(), senderRpc, receiverParty, config);
        lnotSender = LnotFactory.createSender(senderRpc, receiverParty, config.getLnotConfig());
        bcSender = BcFactory.createSender(senderRpc, receiverParty, config.getBcConfig());
        addSubPtos(lnotSender);
    }

    @Override
    public void init(int l, int maxBitNum) throws MpcAbortException {
        setInitInput(l, maxBitNum);
        logPhaseInfo(PtoState.INIT_BEGIN);
        this.q = (int) Math.ceil((double) l / m);
        stopWatch.start();
        // init 1-out-of-n ot sender.
        lnotSender.init(m, maxBitNum, maxBitNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);


    }

    @Override
    public BitVector lt(ZlVector inputs) throws MpcAbortException {
        num = inputs.getNum();
        this.inputs = inputs;
        // split inputs
        ZlVector[] splittedInputs = splitInputs();

        SquareZ2Vector[][] senderShares = iterateSubstring(splittedInputs);
        SquareZ2Vector[] ltShares = senderShares[0];
        SquareZ2Vector[] eqShares = senderShares[1];
        // tree-based AND
        int logQ = LongUtils.ceilLog2(q);
        for (int i = 1; i < logQ; i++) {
            for (int j = 0; j < q / (1 << i); j++) {
                ltShares[j] = bcSender.xor(bcSender.and(ltShares[j * 2], eqShares[j * 2 + 1]), ltShares[j * 2 + 1]);
                eqShares[j] = bcSender.and(eqShares[j * 2], eqShares[j * 2 + 1]);
            }
        }
        bcSender.revealOther(ltShares[0]);
        return bcSender.revealOwn(ltShares[0]);
    }

    private ZlVector[] splitInputs() {
        byte[][] inputBytes = Arrays.stream(inputs.getElements())
                .map(value -> BigIntegerUtils.nonNegBigIntegerToByteArray(value, l))
                .toArray(byte[][]::new);
        ZlDatabase inputDatabase = ZlDatabase.create(l, inputBytes);
        BitVector[] inputBitVectors = inputDatabase.bitPartition(envType, parallel);

        IntStream indexIntStream = IntStream.range(0, q);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        // TODO 这里取值的顺序可能会导致错误
        ZlDatabase[] splittedDatabases = indexIntStream.mapToObj(i -> {
            BitVector[] vectors = IntStream.range(0, m)
                    .mapToObj(j -> inputBitVectors[i + j])
                    .toArray(BitVector[]::new);
            return ZlDatabase.create(envType, parallel, vectors);
        }).toArray(ZlDatabase[]::new);
        return Arrays.stream(splittedDatabases)
                .map(database -> ZlVector.create(ZlFactory.createInstance(envType, l), database.getBigIntegerData()))
                .toArray(ZlVector[]::new);
    }

    private SquareZ2Vector[][] iterateSubstring(ZlVector[] splittedInputs) throws MpcAbortException {
        // sender create own random shares for every substring of inputs
        BitVector[][] s = new BitVector[q][];
        BitVector[][] t = new BitVector[q][];
        ZlVector[] targetValues = IntStream.range(0, 1 << m)
                .mapToObj(k -> ZlVector.create(ZlFactory.createInstance(envType, l),
                        IntStream.range(0, num).mapToObj(i -> BigInteger.valueOf(k)).toArray(BigInteger[]::new)))
                .toArray(ZlVector[]::new);

        SquareZ2Vector[] senderLtShares = new SquareZ2Vector[q];
        SquareZ2Vector[] senderEqShares = new SquareZ2Vector[q];
        for (int j = 0; j < q; j++) {
            BitVector senderLtShareBitVector = BitVectorFactory.createRandom(num, secureRandom);
            BitVector senderEqShareBitVector = BitVectorFactory.createRandom(num, secureRandom);
            senderLtShares[j] = SquareZ2Vector.create(senderLtShareBitVector, false);
            senderEqShares[j] = SquareZ2Vector.create(senderEqShareBitVector, false);
            for (int k = 0; k < 1 << m; k++) {
                s[j][k] = senderLtShareBitVector.xor(splittedInputs[j].lt(targetValues[k]));
                t[j][k] = senderEqShareBitVector.xor(splittedInputs[j].eq(targetValues[k]));
            }
        }

        // perform 1-out-of-n ot
        LnotSenderOutput[] senderOutput = new LnotSenderOutput[q];
        for (int i = 0; i < q; i++) {
            senderOutput[i] = lnotSender.send(num);
        }
        // send s,t using ot keys
        Prg prg = PrgFactory.createInstance(envType, 1);
        // iterate substring
        for (int i = 0; i < q; i++) {
            // iterate (0,2^m-1)
            List<byte[]> ltSendings = new ArrayList<>();
            List<byte[]> eqSendings = new ArrayList<>();

            for (int k = 0; k < 1 << m; k++) {
                byte[] otKey = new byte[CommonUtils.getByteLength(num)];
                // iterate vector
                for (int u = 0; u < num; u++) {
                    // 将key的第一个bit设置到otekey中
                    BinaryUtils.setBoolean(otKey, u, BinaryUtils.getBoolean(
                            prg.extendToBytes(senderOutput[i].getRb(k, u)), 0));
                }
                // TODO只发送了s
                byte[] ltSending = BytesUtils.xor(s[i][k].getBytes(), otKey);
                byte[] eqSending = BytesUtils.xor(t[i][k].getBytes(), otKey);
                ltSendings.add(ltSending);
                eqSendings.add(eqSending);
            }
            // TODO 多次发送，一些辅助信息可能要更新
            DataPacketHeader ltSendingHeader = new DataPacketHeader(
                    encodeTaskId, getPtoDesc().getPtoId(), Cryptflow2MillionairePtoDesc.PtoStep.OT.ordinal(), extraInfo,
                    ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(ltSendingHeader, ltSendings));
            DataPacketHeader eqSendingHeader = new DataPacketHeader(
                    encodeTaskId, getPtoDesc().getPtoId(), Cryptflow2MillionairePtoDesc.PtoStep.OT.ordinal(), extraInfo,
                    ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(eqSendingHeader, eqSendings));
        }
        return new SquareZ2Vector[][]{senderLtShares, senderEqShares};
    }
}
