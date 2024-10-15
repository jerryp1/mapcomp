package edu.alibaba.mpc4j.s2pc.opf.pgenerator.smallfield.ahi22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.opf.pgenerator.PermGenFactory.PartyTypes;
import edu.alibaba.mpc4j.s2pc.opf.pgenerator.smallfield.AbstractSmallFieldPermGenParty;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Ahi22 Permutable Sorter abstract party
 *
 */
public abstract class AbstractAhi22SmallFieldPermGenParty extends AbstractSmallFieldPermGenParty {
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

    protected AbstractAhi22SmallFieldPermGenParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, Ahi22SmallFieldPermGenConfig config, PartyTypes partyTypes) {
        super(ptoDesc, rpc, otherParty, config);
        if (partyTypes.equals(PartyTypes.SENDER)) {
            bit2aParty = Bit2aFactory.createSender(rpc, otherParty, config.getBit2aConfig());
            zlcParty = ZlcFactory.createSender(rpc, otherParty, config.getZlcConfig());
            z2cParty = Z2cFactory.createSender(rpc, otherParty, config.getZ2cConfig());
            zlMuxParty = ZlMuxFactory.createSender(rpc, otherParty, config.getZlMuxConfig());
        } else {
            bit2aParty = Bit2aFactory.createReceiver(rpc, otherParty, config.getBit2aConfig());
            zlcParty = ZlcFactory.createReceiver(rpc, otherParty, config.getZlcConfig());
            z2cParty = Z2cFactory.createReceiver(rpc, otherParty, config.getZ2cConfig());
            zlMuxParty = ZlMuxFactory.createReceiver(rpc, otherParty, config.getZlMuxConfig());
        }
        addMultipleSubPtos(bit2aParty, zlcParty, z2cParty, zlMuxParty);
        zl = config.getBit2aConfig().getZl();
        byteL = zl.getByteL();
    }

    @Override
    public void init(int maxNum, int maxBitNum) throws MpcAbortException {
        setInitInput(maxNum, maxBitNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxFullNum = CommonUtils.getByteLength(maxNum) << 3;
        bit2aParty.init(zl.getL(), maxFullNum * ((1 << maxBitNum) - 1));
        z2cParty.init(maxFullNum * (1 << (maxBitNum - 1)));
        zlcParty.init(maxFullNum);
        zlMuxParty.init(maxBitNum == 1 ? maxFullNum : maxFullNum * (1 << maxBitNum));
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector sort(SquareZ2Vector[] xiArray) throws MpcAbortException {
        setPtoInput(xiArray);
        logPhaseInfo(PtoState.PTO_BEGIN);

        SquareZlVector result;
        if (xiArray.length == 1) {
            result = execute(xiArray[0]);
        } else if (xiArray.length == 2) {
            result = execute2(xiArray[0], xiArray[1]);
        } else {
            result = execute3(xiArray[0], xiArray[1], xiArray[2]);
        }

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    private SquareZlVector execute(SquareZ2Vector xi) throws MpcAbortException {

        stopWatch.start();
        SquareZlVector ones = SquareZlVector.createOnes(zl, num);
        SquareZlVector[] signs = new SquareZlVector[2];
        signs[1] = bit2aParty.bit2a(xi);
        signs[0] = zlcParty.sub(ones, signs[1]);
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime(), "compute signs");

        // prefix sum
        stopWatch.start();
        SquareZlVector[] indexes = computeIndex(signs);
        SquareZlVector res = zlcParty.add(indexes[0], zlMuxParty.mux(xi, zlcParty.sub(indexes[1], indexes[0])));
        SquareZlVector plainOne = SquareZlVector.createOnes(zl, res.getNum());
        res = zlcParty.sub(res, plainOne);
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime(), "compute permutation");

        return res;
    }

    /**
     * get permutation representing the stable sorting of elements a[i]|b[i]
     */
    private SquareZlVector execute2(SquareZ2Vector a, SquareZ2Vector b) throws MpcAbortException {
        // compute sign for 00， 01， 10， 11
        stopWatch.start();
        SquareZlVector arithmeticOnes = SquareZlVector.createOnes(zl, num);
        SquareZ2Vector signAllOne = z2cParty.and(a, b);
        SquareZlVector[] originSigns = bit2aParty.bit2a(new SquareZ2Vector[]{a, b, signAllOne});
        SquareZlVector[] signs = new SquareZlVector[4];
        signs[3] = originSigns[2];
        signs[2] = zlcParty.sub(originSigns[0], signs[3]);
        signs[1] = zlcParty.sub(originSigns[1], signs[3]);
        signs[0] = zlcParty.sub(zlcParty.sub(arithmeticOnes, originSigns[0]), signs[1]);
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime(), "compute signs");

        // prefix sum
        stopWatch.start();
        SquareZlVector[] indexes = computeIndex(signs);
        SquareZlVector res = mulWithAdd(signs, indexes);
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime(), "compute permutation");

        return res;
    }

    /**
     * get permutation representing the stable sorting of elements a[i]|b[i]|c[i]
     */
    private SquareZlVector execute3(SquareZ2Vector a, SquareZ2Vector b, SquareZ2Vector c) throws MpcAbortException {
        // compute 8 signs, corresponding to 000 ~ 111
        stopWatch.start();
        SquareZ2Vector[] firstAndRes = z2cParty.and(new SquareZ2Vector[]{a, b, c}, new SquareZ2Vector[]{b, c, a});
        SquareZ2Vector secondAndRes = z2cParty.and(firstAndRes[0], c);
        SquareZlVector[] bitA = bit2aParty.bit2a(new SquareZ2Vector[]{a, b, c, firstAndRes[0], firstAndRes[1], firstAndRes[2], secondAndRes});
        SquareZlVector[] signs = new SquareZlVector[8];
        signs[7] = bitA[6];
        signs[6] = zlcParty.sub(bitA[3], signs[7]);
        signs[5] = zlcParty.sub(bitA[5], signs[7]);
        signs[4] = zlcParty.sub(bitA[0], zlcParty.add(bitA[3], signs[5]));
        signs[3] = zlcParty.sub(bitA[4], signs[7]);
        signs[2] = zlcParty.sub(bitA[1], zlcParty.add(bitA[3], signs[3]));
        signs[1] = zlcParty.sub(bitA[2], zlcParty.add(bitA[4], signs[5]));
        SquareZlVector invAInvB = zlcParty.add(zlcParty.sub(bitA[3], zlcParty.add(bitA[0], bitA[1])), SquareZlVector.createOnes(zl, a.bitNum()));
        signs[0] = zlcParty.sub(invAInvB, signs[1]);
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime(), "compute signs");

        // prefix sum
        stopWatch.start();
        SquareZlVector[] indexes = computeIndex(signs);
        SquareZlVector res = mulWithAdd(signs, indexes);
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime(), "compute permutation");

        return res;
    }

    private SquareZlVector[] computeIndex(SquareZlVector[] signs) {
        // prefix sum
        SquareZlVector zeroPlain = SquareZlVector.createZeros(zl, 1);
        SquareZlVector[] indexes = new SquareZlVector[signs.length];
        for (int i = 0; i < signs.length; i++) {
            indexes[i] = zlcParty.rowAdderWithPrefix(signs[i], zeroPlain);
            zeroPlain = SquareZlVector.create(zl, new BigInteger[]{indexes[i].getZlVector().getElement(indexes[i].getNum() - 1)}, false);
        }
        return indexes;
    }

    private SquareZlVector mulWithAdd(SquareZlVector[] a, SquareZlVector[] b) throws MpcAbortException {
        SquareZ2Vector[] binaryA = Arrays.stream(a).map(SquareZlVector::getLastBit).toArray(SquareZ2Vector[]::new);
        SquareZlVector[] mulRes = zlMuxParty.mux(binaryA, b);
        for (int i = 1; i < mulRes.length; i++) {
            mulRes[0] = zlcParty.add(mulRes[0], mulRes[i]);
        }
        SquareZlVector plainOne = SquareZlVector.createOnes(zl, a[0].getNum());
        return zlcParty.sub(mulRes[0], plainOne);
    }
}
