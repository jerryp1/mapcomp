package edu.alibaba.mpc4j.s2pc.pso.psic.cpsic;

import java.nio.ByteBuffer;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingFactory;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingParty;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psic.AbstractPsicServer;
import edu.alibaba.mpc4j.s2pc.pso.psic.PsicUtils;
import edu.alibaba.mpc4j.s2pc.pso.psic.cgt12.Cgt12EccPsicConfig;
import edu.alibaba.mpc4j.s2pc.pso.psic.cgt12.Cgt12EccPsicPtoDesc;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Circuit-PSI Cardinality Server
 *
 * @author Qixian Zhou
 * @date 2023/4/24
 */
public class CpsicServer<T> extends AbstractPsicServer<T> {

	/**
	 * Client payload Circuit PSI server
	 */
	CcpsiServer ccpsiServer;
	/**
	 * Hamming Sender
	 */
	HammingParty hammingSender;


	public CpsicServer(Rpc serverRpc, Party clientParty, CpsicConfig config) {
		super(CpsicPtoDesc.getInstance(), serverRpc, clientParty, config);
		ccpsiServer = CcpsiFactory.createServer(serverRpc, clientParty, config.getCcpsiConfig());
		hammingSender = HammingFactory.createSender(serverRpc, clientParty, config.getHammingConfig());

		addSubPtos(ccpsiServer);
		addSubPtos(hammingSender);
	}

	@Override
	public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
		setInitInput(maxServerElementSize, maxClientElementSize);
		logPhaseInfo(PtoState.INIT_BEGIN);

		stopWatch.start();
		// init ccpsiServer
		ccpsiServer.init(maxServerElementSize, maxClientElementSize);
		// how to right set this maxBitNum in init?
		// must be init, or will cause error
		hammingSender.init(maxServerElementSize + maxClientElementSize);
		stopWatch.stop();
		long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

		logPhaseInfo(PtoState.INIT_END);
	}

	@Override
	public void psic(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
		setPtoInput(serverElementSet, clientElementSize);
		logPhaseInfo(PtoState.PTO_BEGIN);

		stopWatch.start();
		// 1. run ccpsi Server
		// convert Set<T> to Set<ByteBuffer>
		Stream<T> serverElementStream = serverElementSet.stream();
		serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
		Set<ByteBuffer> serverElementSetByteBuffer = serverElementStream
				.map(ObjectUtils::objectToByteArray)
				.map(ByteBuffer::wrap)
				.collect(Collectors.toSet());
		SquareZ2Vector ccpsiServerOutput = ccpsiServer.psi(serverElementSetByteBuffer, clientElementSize);
		stopWatch.stop();
		long psiTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 1, 2, psiTime);

		// 2. hammingParty and run
		stopWatch.start();
		hammingSender.init(ccpsiServerOutput.getNum());//re-init
		hammingSender.sendHammingDistance(ccpsiServerOutput);
		stopWatch.stop();
		long hammingTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();

		logStepInfo(PtoState.PTO_STEP, 2, 2, hammingTime);

		logPhaseInfo(PtoState.PTO_END);
	}

}