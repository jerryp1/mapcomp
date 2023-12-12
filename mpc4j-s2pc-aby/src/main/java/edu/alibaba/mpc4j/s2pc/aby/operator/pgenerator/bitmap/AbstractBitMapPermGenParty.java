package edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.AbstractPermGenParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenFactory.PartyTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;

import java.math.BigInteger;
import java.util.stream.IntStream;

/**
 * Permutable bitmap sorter abstract party
 *
 * @author Feng Han
 * @date 2023/10/27
 */
public abstract class AbstractBitMapPermGenParty extends AbstractPermGenParty {
    /**
     * Bit2a sender.
     */
    private final Bit2aParty bit2aParty;
    /**
     * Zl circuit sender.
     */
    private final ZlcParty zlcParty;
    /**
     * Z2 circuit sender.
     */
    private final Z2cParty z2cParty;
    /**
     * Zl mux sender.
     */
    private final ZlMuxParty zlMuxParty;
    /**
     * maximum number of bits in one batch
     */
    private final int maxNumInBatch;

    protected AbstractBitMapPermGenParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, BitmapPermGenConfig config, PartyTypes partyTypes) {
        super(ptoDesc, rpc, otherParty, config);
        if (partyTypes.equals(PartyTypes.SENDER)) {
            bit2aParty = Bit2aFactory.createSender(rpc, otherParty, config.getBit2aConfig());
            z2cParty = Z2cFactory.createSender(rpc, otherParty, config.getZ2cConfig());
            zlcParty = ZlcFactory.createSender(rpc, otherParty, config.getZlcConfig());
            zlMuxParty = ZlMuxFactory.createSender(rpc, otherParty, config.getZlMuxConfig());
        } else {
            bit2aParty = Bit2aFactory.createReceiver(rpc, otherParty, config.getBit2aConfig());
            z2cParty = Z2cFactory.createReceiver(rpc, otherParty, config.getZ2cConfig());
            zlcParty = ZlcFactory.createReceiver(rpc, otherParty, config.getZlcConfig());
            zlMuxParty = ZlMuxFactory.createReceiver(rpc, otherParty, config.getZlMuxConfig());
        }
        maxNumInBatch = config.getMaxNumInBatch();
        addMultipleSubPtos(bit2aParty, zlcParty, zlMuxParty);
        zl = config.getBit2aConfig().getZl();
        byteL = zl.getByteL();
    }

    @Override
    public void init(int maxNum, int maxBitNum) throws MpcAbortException {
        int maxFullNum = CommonUtils.getByteLength(maxNum) << 3;
        setInitInput(maxFullNum, maxBitNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bit2aParty.init(zl.getL(), maxFullNum * maxBitNum);
        z2cParty.init(1);
        zlcParty.init(1);
        zlMuxParty.init(maxFullNum * (maxBitNum + 1));
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector sort(SquareZ2Vector[] xiArray) throws MpcAbortException {
        setPtoInput(xiArray);

        long total = ((long) xiArray.length) * xiArray[0].bitNum();
        if (total > maxNumInBatch) {
            int maxBatchSize = Math.max(maxNumInBatch / xiArray[0].bitNum(), 1);
            return sortWithSeq(xiArray, maxBatchSize);
        }

        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        SquareZlVector[] bitA = new SquareZlVector[xiArray.length + 1];
        System.arraycopy(bit2aParty.bit2a(xiArray), 0, bitA, 0, xiArray.length);

        SquareZlVector sumAll = bitA[0].copy();
        IntStream.range(1, xiArray.length).forEach(i -> sumAll.getZlVector().addi(bitA[i].getZlVector()));
        sumAll.getZlVector().negi();
        ZlVector onesPlain = ZlVector.createOnes(zl, bitA[0].getNum());
        SquareZlVector ones = zlcParty.setPublicValue(onesPlain);
        sumAll.getZlVector().addi(ones.getZlVector());
        bitA[xiArray.length] = sumAll;

        logStepInfo(PtoState.PTO_STEP, 1, 3, resetAndGetTime(), "transfer binary shares to arithmetic shares");

        // prefix sum
        stopWatch.start();
        SquareZlVector zeroPlain = SquareZlVector.createZeros(zl, 1);
        SquareZlVector[] indexes = computeIndex(bitA, zeroPlain);
        logStepInfo(PtoState.PTO_STEP, 2, 3, resetAndGetTime(), "prefix sum");

        stopWatch.start();
        SquareZ2Vector[] bits = new SquareZ2Vector[xiArray.length + 1];
        System.arraycopy(xiArray, 0, bits, 0, xiArray.length);
        SquareZ2Vector notXorAll = xiArray[0].copy();
        IntStream.range(1, xiArray.length).forEach(i -> notXorAll.getBitVector().xori(bits[i].getBitVector()));
        z2cParty.noti(notXorAll);
        bits[xiArray.length] = notXorAll;
        SquareZlVector res = muxMultiIndex(bits, indexes);
        SquareZlVector plainOne = SquareZlVector.createOnes(zl, res.getNum());
        res = zlcParty.sub(res, plainOne);
        logStepInfo(PtoState.PTO_STEP, 3, 3, resetAndGetTime(), "compute permutation");

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }

    private SquareZlVector sortWithSeq(SquareZ2Vector[] xiArray, int maxBatchNum) throws MpcAbortException {
        MathPreconditions.checkGreaterOrEqual("xiArray.length >= maxBatchNum", xiArray.length, maxBatchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        SquareZ2Vector notXorAll = xiArray[0].copy();
        IntStream.range(1, xiArray.length).forEach(i -> notXorAll.getBitVector().xori(xiArray[i].getBitVector()));
        z2cParty.noti(notXorAll);

        SquareZlVector currentIndex = null;
        SquareZlVector zeroPlain = SquareZlVector.createZeros(zl, 1);
        int round = xiArray.length / maxBatchNum + (xiArray.length % maxBatchNum > 0 ? 1 : 0);
        for (int i = 0, endIndex = xiArray.length; i < round; i++, endIndex -= maxBatchNum) {
            stopWatch.start();
            int startIndex = Math.max(0, endIndex - maxBatchNum);
            int inputDim = (endIndex - startIndex) + (i == 0 ? 1 : 0);
            SquareZ2Vector[] inputBits = new SquareZ2Vector[inputDim];
            System.arraycopy(xiArray, startIndex, inputBits, 0, endIndex - startIndex);
            if (i == 0) {
                inputBits[inputDim - 1] = notXorAll;
            }
            SquareZlVector[] bitA = bit2aParty.bit2a(inputBits);
            logStepInfo(PtoState.PTO_STEP, 1 + i * 3, 3 * round, resetAndGetTime(), "transfer binary shares to arithmetic shares");

            stopWatch.start();
            SquareZlVector[] indexes = computeIndex(bitA, zeroPlain);
            zeroPlain = SquareZlVector.create(zl, new BigInteger[]{indexes[0].getZlVector()
                .getElement(indexes[0].getNum() - 1)}, false);
            logStepInfo(PtoState.PTO_STEP, 2 + i * 3, 3 * round, resetAndGetTime(), "prefix sum");

            stopWatch.start();
            SquareZlVector res = muxMultiIndex(inputBits, indexes);
            if (currentIndex == null) {
                currentIndex = res;
            } else {
                currentIndex = zlcParty.add(currentIndex, res);
            }
            logStepInfo(PtoState.PTO_STEP, 3 + i * 3, 3 * round, resetAndGetTime(), "compute permutation");
        }
        assert currentIndex != null;
        SquareZlVector plainOne = SquareZlVector.createOnes(zl, currentIndex.getNum());
        currentIndex = zlcParty.sub(currentIndex, plainOne);
        logPhaseInfo(PtoState.PTO_END);
        return currentIndex;
    }

    private SquareZlVector[] computeIndex(SquareZlVector[] signs, SquareZlVector zeroPlain) {
        // prefix sum
        SquareZlVector[] indexes = new SquareZlVector[signs.length];
        for (int i = signs.length - 1; i >= 0; i--) {
            indexes[i] = zlcParty.rowAdderWithPrefix(signs[i], zeroPlain);
            zeroPlain = SquareZlVector.create(zl, new BigInteger[]{indexes[i].getZlVector().getElement(indexes[i].getNum() - 1)}, false);
        }
        return indexes;
    }

    private SquareZlVector muxMultiIndex(SquareZ2Vector[] xiArray, SquareZlVector[] indexes) throws MpcAbortException {
        assert indexes.length == xiArray.length;
        SquareZlVector[] mulRes = zlMuxParty.mux(xiArray, indexes);
        for (int i = 1; i < mulRes.length; i++) {
            mulRes[0] = zlcParty.add(mulRes[0], mulRes[i]);
        }
        return mulRes[0];
    }
}
