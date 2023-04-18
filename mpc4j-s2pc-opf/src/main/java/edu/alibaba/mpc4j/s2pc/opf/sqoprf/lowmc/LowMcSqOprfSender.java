package edu.alibaba.mpc4j.s2pc.opf.sqoprf.lowmc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpSender;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpSenderOutput;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.AbstractSqOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PSSW09 based LowMc single-query OPRF Sender.
 *
 * @author Qixian Zhou
 * @date 2023/4/17
 */
public class LowMcSqOprfSender extends AbstractSqOprfSender {

	/**
	 * Oprp Sender
	 */
	private OprpSender oprpSender;
	/**
	 * Oprp sender output
	 */
	private OprpSenderOutput oprpSenderOutput;
	/**
	 * Low Mc Sq oprf key
	 */
	private LowMcSqOprfKey key;

	public LowMcSqOprfSender(Rpc senderRpc, Party receiverParty, LowMcSqOprfConfig config) {
		super(LowMcSqOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
		oprpSender = OprpFactory.createSender(senderRpc, receiverParty, config.getOprpConfig());
		addSubPtos(oprpSender);
		key = null;
	}

	@Override
	public LowMcSqOprfKey keyGen() {

		byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
		secureRandom.nextBytes(key);
		return new LowMcSqOprfKey(envType, key, oprpSender.isInvPrp(), oprpSender.getPrpType());
	}

	@Override
	public void init(int maxBatchSize, SqOprfKey key) throws MpcAbortException {
		setInitInput(maxBatchSize);
		logPhaseInfo(PtoState.INIT_BEGIN);

		// 1. set key
		stopWatch.start();
		this.key = (LowMcSqOprfKey) key;
		stopWatch.stop();
		long setKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.INIT_STEP, 1, 2, setKeyTime);

		//2. init Oprp
		stopWatch.start();
		oprpSender.init(maxBatchSize);
		stopWatch.stop();
		long initOprpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.INIT_STEP, 2, 2, initOprpTime);

		logPhaseInfo(PtoState.INIT_END);
	}

	@Override
	public void oprf(int batchSize) throws MpcAbortException {
		setPtoInput(batchSize);
		logPhaseInfo(PtoState.PTO_BEGIN);

		// 1. run Oprp, get the OprpSenderOutput
		stopWatch.start();
		oprpSenderOutput = oprpSender.oprp(key.getOprpKey(), batchSize);
		stopWatch.stop();
		long oprpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 1, 2, oprpTime, "Sender get OprpSenderOutput");

		// 2. Sender generates sharesPayload and sends to Receiver
		stopWatch.start();
		List<byte[]> sharesPayload = generateSharesPayload();
		DataPacketHeader sharesHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), LowMcSqOprfPtoDesc.PtoStep.SENDER_SEND_SHARES.ordinal(), extraInfo,
				ownParty().getPartyId(), otherParty().getPartyId()
		);
		rpc.send(DataPacket.fromByteArrayList(sharesHeader, sharesPayload));
		stopWatch.stop();
		long sharesTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 2, 2, sharesTime, "Sender sends shares payload.");

		logPhaseInfo(PtoState.PTO_END);
	}

	private List<byte[]> generateSharesPayload() {

		IntStream batchStream = IntStream.range(0, batchSize);
		batchStream = parallel ? batchStream.parallel() : batchStream;

		return batchStream.
				mapToObj(
						index ->
								oprpSenderOutput.getShare(index)).collect(Collectors.toList());

	}

}