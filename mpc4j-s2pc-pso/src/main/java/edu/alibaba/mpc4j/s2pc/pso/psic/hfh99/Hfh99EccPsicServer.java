package edu.alibaba.mpc4j.s2pc.pso.psic.hfh99;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pso.psic.AbstractPsicServer;
import edu.alibaba.mpc4j.s2pc.pso.psic.PsicUtils;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * HFH99 based on ECC PSCI server
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class Hfh99EccPsicServer<T> extends AbstractPsicServer<T> {
	/**
	 * ECC
	 */
	private final Ecc ecc;
	/**
	 * compress enmcode
	 */
	private final boolean compressEncode;
	/**
	 * PEQT hash
	 */
	private Hash peqtHash;
	/**
	 * secret key
	 */
	private BigInteger alpha;

	public Hfh99EccPsicServer(Rpc serverRpc, Party clientParty, Hfh99EccPsicConfig config) {
		super(Hfh99EccPsicPtoDesc.getInstance(), serverRpc, clientParty, config);
		ecc = EccFactory.createInstance(envType);
		compressEncode = config.getCompressEncode();
	}

	@Override
	public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
		setInitInput(maxServerElementSize, maxClientElementSize);
		logPhaseInfo(PtoState.INIT_BEGIN);

		stopWatch.start();
		// generate alpha
		alpha = ecc.randomZn(secureRandom);
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
		int peqtByteLength = PsicUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
		peqtHash = HashFactory.createInstance(envType, peqtByteLength);
		// server calculates H(x)^alpha and randomly permute it , the sned it to client.
		List<byte[]> randomlyPermutedHxAlphaPayload = generateRandomlyPermuteHxAlphaPayload();
		DataPacketHeader randomlyPermutedHxAlphaHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), Hfh99EccPsicPtoDesc.PtoStep.SERVER_SEND_RANDOMLY_PERMUTED_HX_ALPHA.ordinal(), extraInfo,
				ownParty().getPartyId(), otherParty().getPartyId()
		);
		rpc.send(DataPacket.fromByteArrayList(randomlyPermutedHxAlphaHeader, randomlyPermutedHxAlphaPayload));
		stopWatch.stop();
		long hxAlphaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 1, 2, hxAlphaTime);

		// server receiver H(y)^\beta , which has been randomly permuted.
		DataPacketHeader randomlyPermutedHyBetaHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), Hfh99EccPsicPtoDesc.PtoStep.CLIENT_SEND_RANDOMLY_PERMUTED_HY_BETA.ordinal(), extraInfo,
				otherParty().getPartyId(), ownParty().getPartyId()
		);
		List<byte[]> randomlyPermutedHyBetaPayload = rpc.receive(randomlyPermutedHyBetaHeader).getPayload();

		stopWatch.start();
		// server calculates H(y)^βα and randomly permute it , the sned it to client.
		List<byte[]> randomlyPermutedPeqtPayload = handleRandomlyPermutedHyBetaPayload(randomlyPermutedHyBetaPayload);
		DataPacketHeader randomlyPermutedPeqtHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), Hfh99EccPsicPtoDesc.PtoStep.CLIENT_SEND_RANDOMLY_PERMUTED_HY_BETA_ALPHA.ordinal(), extraInfo,
				ownParty().getPartyId(), otherParty().getPartyId()
		);
		rpc.send(DataPacket.fromByteArrayList(randomlyPermutedPeqtHeader, randomlyPermutedPeqtPayload));
		stopWatch.stop();
		long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 2, 2, peqtTime);

		logPhaseInfo(PtoState.PTO_END);
	}

	private List<byte[]> generateRandomlyPermuteHxAlphaPayload() {
		Stream<T> serverElementStream = serverElementArrayList.stream();
		serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;

		List<byte[]> result = serverElementStream
				.map(ObjectUtils::objectToByteArray)
				.map(ecc::hashToCurve)
				.map(p -> ecc.multiply(p, alpha))
				.map(p -> ecc.encode(p, compressEncode))
				.collect(Collectors.toList());
		// randomly permute
		Collections.shuffle(result, secureRandom);
		return result;
	}

	private List<byte[]> handleRandomlyPermutedHyBetaPayload(List<byte[]> hyBetaPayload) throws MpcAbortException {
		MpcAbortPreconditions.checkArgument(hyBetaPayload.size() == clientElementSize);
		Stream<byte[]> hyBetaStream = hyBetaPayload.stream();
		hyBetaStream = parallel ? hyBetaStream.parallel() : hyBetaStream;

		List<byte[]> result = hyBetaStream
				.map(ecc::decode)
				.map(p -> ecc.multiply(p, alpha))
				.map(p -> ecc.encode(p, false))
				.map(p -> peqtHash.digestToBytes(p))
				.collect(Collectors.toList());
		// randomly permute
		Collections.shuffle(result, secureRandom);
		return result;
	}
}