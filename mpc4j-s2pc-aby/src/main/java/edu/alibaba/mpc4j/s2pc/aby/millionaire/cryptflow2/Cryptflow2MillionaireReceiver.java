package edu.alibaba.mpc4j.s2pc.aby.millionaire.cryptflow2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
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
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Cryptflow2 Millionaire Protocol Receiver.
 *
 * @author Li Peng
 * @date 2023/4/25
 */
public class Cryptflow2MillionaireReceiver extends AbstractMillionaireParty {
    /**
     * 1-out-of-n (with n = 2^l) ot receiver.
     */
    private final LnotReceiver lnotReceiver;
    /**
     * boolean circuit receiver.
     */
    private final BcParty bcReceiver;
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
     * the input value of receiver.
     */
    private ZlVector inputs;

    public Cryptflow2MillionaireReceiver(Rpc receiverRpc, Party senderParty, Cryptflow2MillionaireConfig config) {
        super(Cryptflow2MillionairePtoDesc.getInstance(), receiverRpc, senderParty, config);
        lnotReceiver = LnotFactory.createReceiver(receiverRpc, senderParty, config.getLnotConfig());
        bcReceiver = BcFactory.createReceiver(receiverRpc, senderParty, config.getBcConfig());
        addSubPtos(lnotReceiver);
    }

    @Override
    public void init(int l, int maxBitNum) throws MpcAbortException {
        // TODO maxBitnum有什么用？
        setInitInput(l, maxBitNum);
        logPhaseInfo(PtoState.INIT_BEGIN);
        this.q = (int) Math.ceil((double) l / m);
        stopWatch.start();
        // init 1-out-of-n ot receiver.
        lnotReceiver.init(m, maxBitNum, maxBitNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BitVector lt(ZlVector inputs) {
        num = inputs.getNum();
        this.inputs = inputs;
        SecureRandom secureRandom = new SecureRandom();
        // split inputs
        ZlVector[] splittedInputs = splitInputs();
        // iterate substring and perform ot
        SquareZ2Vector[][] receiverShares = iterateSubString(splittedInputs);
        SquareZ2Vector[] ltShares = receiverShares[0];
        SquareZ2Vector[] eqShares = receiverShares[1];
        // tree-based AND
        int logQ = LongUtils.ceilLog2(q);
        for (int i = 1; i < logQ; i++) {
            for (int j = 0; j < q / (1 << i); j++) {
                ltShares[j] = bcReceiver.xor(bcReceiver.and(ltShares[j * 2], eqShares[j * 2 + 1]), ltShares[j * 2 + 1]);
                eqShares[j] = bcReceiver.and(eqShares[j * 2], eqShares[j * 2 + 1]);
            }
        }
        bcReceiver.revealOther(ltShares[0]);
        return bcReceiver.revealOwn(ltShares[0]);
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

    private SquareZ2Vector[][] iterateSubString(ZlVector[] splittedInputs) {
        // receive ot
        LnotReceiverOutput[] receiverOutputs = Arrays.stream(splittedInputs)
                .map(vector -> lnotReceiver.receive(
                        Arrays.stream(vector.getElements()).mapToInt(BigInteger::intValue).toArray()));
        // 只取1bit，拓展为1byte即可
        Prg prg = PrgFactory.createInstance(envType, 1);
        SquareZ2Vector[] receiverLtShares = new SquareZ2Vector[q];
        SquareZ2Vector[] receiverEqShares = new SquareZ2Vector[q];
        // iterate substring
        for (int i = 0; i < q; i++) {

            byte[] otKey = new byte[CommonUtils.getByteLength(num)];
            // iterate vector
            for (int u = 0; u < num; u++) {
                // 将key的第一个bit设置到otekey中
                BinaryUtils.setBoolean(otKey, u, BinaryUtils.getBoolean(
                        prg.extendToBytes(receiverOutputs[i].getRb(u)), 0));
            }
            // receiving payload
            DataPacketHeader ltReceivingHeader = new DataPacketHeader(
                    encodeTaskId, getPtoDesc().getPtoId(), Cryptflow2MillionairePtoDesc.PtoStep.OT.ordinal(), extraInfo,
                    otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> ltReceivingPayload = rpc.receive(ltReceivingHeader).getPayload();
            DataPacketHeader eqReceivingHeader = new DataPacketHeader(
                    encodeTaskId, getPtoDesc().getPtoId(), Cryptflow2MillionairePtoDesc.PtoStep.OT.ordinal(), extraInfo,
                    otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> eqReceivingPayload = rpc.receive(eqReceivingHeader).getPayload();

            // select and decrypt based on input value
            byte[] ltEnc = new byte[];
            byte[] eqEnc = new byte[];
            for (int u = 0; u < num; u++) {
                int select = inputs.getElement(u).intValue();
                // obtain specific boolean
                BinaryUtils.setBoolean(ltEnc, u, BinaryUtils.getBoolean(ltReceivingPayload.get(select), u));
                BinaryUtils.setBoolean(eqEnc, u, BinaryUtils.getBoolean(eqReceivingPayload.get(select), u));
            }
            byte[] ltReceiving = BytesUtils.xor(ltEnc, otKey);
            byte[] eqReceiving = BytesUtils.xor(eqEnc, otKey);

            receiverLtShares[i] = SquareZ2Vector.create(num, ltReceiving, false);
            receiverEqShares[i] = SquareZ2Vector.create(num, eqReceiving, false);
        }
        return new SquareZ2Vector[2][] {
            receiverLtShares, receiverEqShares
        } ;
    }

}
