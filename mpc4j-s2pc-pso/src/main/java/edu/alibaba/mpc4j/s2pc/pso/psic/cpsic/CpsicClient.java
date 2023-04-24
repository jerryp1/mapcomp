package edu.alibaba.mpc4j.s2pc.pso.psic.cpsic;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingFactory;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingParty;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiClientOutput;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.psic.AbstractPsicClient;


import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Circuit-PSI Cardinality Client
 *
 * @author Qixian Zhou
 * @date 2023/4/24
 */
public class CpsicClient<T> extends AbstractPsicClient<T> {

	/**
	 * Client payload Circuit PSI Client
	 */
	CcpsiClient ccpsiClient;
	/**
	 * Hamming Sender
	 */
	HammingParty hammingReceiver;


	public CpsicClient(Rpc serverRpc, Party clientParty, CpsicConfig config) {
		super(CpsicPtoDesc.getInstance(), serverRpc, clientParty, config);
		ccpsiClient = CcpsiFactory.createClient(serverRpc, clientParty, config.getCcpsiConfig());
		hammingReceiver = HammingFactory.createReceiver(serverRpc, clientParty, config.getHammingConfig());

		addSubPtos(ccpsiClient);
		addSubPtos(hammingReceiver);
	}

	@Override
	public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
		setInitInput(maxClientElementSize, maxServerElementSize);
		logPhaseInfo(PtoState.INIT_BEGIN);

		stopWatch.start();
		// init ccpsiClient
		ccpsiClient.init(maxClientElementSize, maxServerElementSize);
		// how to right set this maxBitNum in init?
		// must be init, or will cause error
		hammingReceiver.init(maxClientElementSize + maxServerElementSize);
		stopWatch.stop();
		long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

		logPhaseInfo(PtoState.INIT_END);
	}

	@Override
	public int psic(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException {
		setPtoInput(clientElementSet, serverElementSize);
		logPhaseInfo(PtoState.PTO_BEGIN);

		stopWatch.start();
		// 1. run ccpsi Server
		// convert Set<T> to Set<ByteBuffer>
		Stream<T> clientElementStream = clientElementSet.stream();
		clientElementStream = parallel ? clientElementStream.parallel() : clientElementStream;
		Set<ByteBuffer> clientElementSetByteBuffer = clientElementStream
				.map(ObjectUtils::objectToByteArray)
				.map(ByteBuffer::wrap)
				.collect(Collectors.toSet());
		CcpsiClientOutput ccpsiClientOutput = ccpsiClient.psi(clientElementSetByteBuffer, serverElementSize);
		stopWatch.stop();
		long psiTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 1, 2, psiTime);

		// 2. init hammingParty and run
		stopWatch.start();
		hammingReceiver.init(ccpsiClientOutput.getZ1().getNum());// re-init
		int cardinality = hammingReceiver.receiveHammingDistance(ccpsiClientOutput.getZ1());
		stopWatch.stop();
		long hammingTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 2, 2, hammingTime);

		logPhaseInfo(PtoState.PTO_END);

		return cardinality;
	}
}