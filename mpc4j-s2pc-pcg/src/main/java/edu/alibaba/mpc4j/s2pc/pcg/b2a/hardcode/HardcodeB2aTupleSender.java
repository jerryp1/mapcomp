package edu.alibaba.mpc4j.s2pc.pcg.b2a.hardcode;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.AbstractB2aTupleParty;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.B2aTuple;

/**
 * hardcode Z2 multiplication triple generator sender.
 *
 * @author Li Peng
 * @date 2023/11/21
 */
public class HardcodeB2aTupleSender extends AbstractB2aTupleParty {

    public HardcodeB2aTupleSender(Rpc senderRpc, Party receiverParty, HardcodeB2aTupleConfig config) {
        super(HardcodeB2aTuplePtoDesc.getInstance(), senderRpc, receiverParty, config);
    }

    public HardcodeB2aTupleSender(Rpc senderRpc, Party receiverParty, Party aiderParty, HardcodeB2aTupleConfig config) {
        super(HardcodeB2aTuplePtoDesc.getInstance(), senderRpc, receiverParty, config);
    }

    @Override
    public void init(int updateNum) throws MpcAbortException {
        setInitInput(updateNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public B2aTuple generate(int num) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);
        int byteNum = CommonUtils.getByteLength(num);
        B2aTuple b2aTuple = B2aTuple.create(BitVectorFactory.createZeros(num), ZlVector.createZeros(zl, num));
        logPhaseInfo(PtoState.PTO_END);
        return b2aTuple;
    }
}
