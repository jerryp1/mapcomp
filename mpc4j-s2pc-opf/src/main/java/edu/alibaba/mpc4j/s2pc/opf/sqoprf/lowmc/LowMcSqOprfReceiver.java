package edu.alibaba.mpc4j.s2pc.opf.sqoprf.lowmc;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprp.*;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.AbstractSqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;


import java.util.List;
import java.util.concurrent.TimeUnit;

import java.util.stream.IntStream;

/**
 * PSSW09 based LowMc single-query OPRF Receiver.
 *
 * @author Qixian Zhou
 * @date 2023/4/17
 */
public class LowMcSqOprfReceiver extends AbstractSqOprfReceiver {

	/**
	 * Oprp Receiver
	 */
	private OprpReceiver oprpReceiver;
	/**
	 * Oprp receiver output
	 */
	private OprpReceiverOutput oprpReceiverOutput;


	public LowMcSqOprfReceiver(Rpc receiverRpc, Party senderParty, LowMcSqOprfConfig config) {
		super(LowMcSqOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);

		oprpReceiver = OprpFactory.createReceiver(receiverRpc, senderParty, config.getOprpConfig());
		addSubPtos(oprpReceiver);

		oprpReceiverOutput = null;
	}


	@Override
	public void init(int maxBatchSize) throws MpcAbortException {
		setInitInput(maxBatchSize);
		logPhaseInfo(PtoState.INIT_BEGIN);

		//1. init Oprp
		stopWatch.start();
		oprpReceiver.init(maxBatchSize);
		stopWatch.stop();
		long initOprpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.INIT_STEP, 1, 1, initOprpTime);

		logPhaseInfo(PtoState.INIT_END);
	}

	@Override
	public SqOprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException {

		setPtoInput(inputs);
		logPhaseInfo(PtoState.PTO_BEGIN);

		// 1. run Oprp, get the OprpSenderOutput
		stopWatch.start();
		oprpReceiverOutput = oprpReceiver.oprp(inputs);
		stopWatch.stop();
		long oprpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 1, 2, oprpTime, "Sender get OprpSenderOutput");

		// 2. Receiver receive shares and generate SqOprfOutput
		stopWatch.start();
		DataPacketHeader sharesHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), LowMcSqOprfPtoDesc.PtoStep.SENDER_SEND_SHARES.ordinal(), extraInfo,
				otherParty().getPartyId(), ownParty().getPartyId()
		);
		List<byte[]> sharesPayload = rpc.receive(sharesHeader).getPayload();
		byte[][] prfs = handleSharesPayload(sharesPayload);
		SqOprfReceiverOutput receiverOutput = new SqOprfReceiverOutput(inputs, prfs);
		stopWatch.stop();
		long sharesTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 2, 2, sharesTime, "Receiver generate prfs.");

		logPhaseInfo(PtoState.PTO_END);

		return receiverOutput;
	}

	private byte[][] handleSharesPayload(List<byte[]> sharesPayload) throws MpcAbortException {

		MpcAbortPreconditions.checkArgument(sharesPayload.size() == batchSize);

		Kdf kdf = KdfFactory.createInstance(envType);
		IntStream batchStream = IntStream.range(0, batchSize);
		batchStream = parallel ? batchStream.parallel() : batchStream;

		return batchStream.
				mapToObj(index ->
						kdf.deriveKey(
								BytesUtils.xor(
										oprpReceiverOutput.getShare(index),
										sharesPayload.get(index)))
				).toArray(byte[][]::new);
	}
}
