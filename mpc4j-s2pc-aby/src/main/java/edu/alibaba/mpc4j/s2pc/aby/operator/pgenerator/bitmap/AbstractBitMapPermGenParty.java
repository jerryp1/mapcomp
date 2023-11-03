package edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.AbstractPermGenParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenFactory.PartyTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;

import java.math.BigInteger;

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
     * Zl mux sender.
     */
    private final ZlMuxParty zlMuxParty;

    protected AbstractBitMapPermGenParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, BitmapPermGenConfig config, PartyTypes partyTypes) {
        super(ptoDesc, rpc, otherParty, config);
        if (partyTypes.equals(PartyTypes.SENDER)) {
            bit2aParty = Bit2aFactory.createSender(rpc, otherParty, config.getBit2aConfig());
            zlcParty = ZlcFactory.createSender(rpc, otherParty, config.getZlcConfig());
            zlMuxParty = ZlMuxFactory.createSender(rpc, otherParty, config.getZlMuxConfig());
        } else {
            bit2aParty = Bit2aFactory.createReceiver(rpc, otherParty, config.getBit2aConfig());
            zlcParty = ZlcFactory.createReceiver(rpc, otherParty, config.getZlcConfig());
            zlMuxParty = ZlMuxFactory.createReceiver(rpc, otherParty, config.getZlMuxConfig());
        }
        addMultipleSubPtos(bit2aParty, zlcParty, zlMuxParty);
        zl = config.getBit2aConfig().getZl();
        byteL = zl.getByteL();
    }

    @Override
    public void init(int maxL, int maxNum, int maxBitNum) throws MpcAbortException {
        setInitInput(maxL, maxNum, maxBitNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bit2aParty.init(maxL, maxNum * maxBitNum);
        zlcParty.init(maxNum);
        zlMuxParty.init(maxNum * maxBitNum);
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector sort(SquareZ2Vector[] xiArray) throws MpcAbortException {
        setPtoInput(xiArray);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        SquareZlVector[] bitA = bit2aParty.bit2a(xiArray);
        logStepInfo(PtoState.PTO_STEP, 1, 3, resetAndGetTime(), "transfer binary shares to arithmetic shares");

        // prefix sum
        stopWatch.start();
        SquareZlVector[] indexes = computeIndex(bitA);
        logStepInfo(PtoState.PTO_STEP, 2, 3, resetAndGetTime(), "prefix sum");

        stopWatch.start();
        SquareZlVector res = muxMultiIndex(xiArray, indexes);
        logStepInfo(PtoState.PTO_STEP, 2, 3, resetAndGetTime(), "compute permutation");

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }

    private SquareZlVector[] computeIndex(SquareZlVector[] signs) {
        // prefix sum
        SquareZlVector zeroPlain = SquareZlVector.createZeros(zl, 1);
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
        SquareZlVector plainOne = SquareZlVector.createOnes(zl, mulRes[0].getNum());
        return zlcParty.sub(mulRes[0], plainOne);
    }
}
