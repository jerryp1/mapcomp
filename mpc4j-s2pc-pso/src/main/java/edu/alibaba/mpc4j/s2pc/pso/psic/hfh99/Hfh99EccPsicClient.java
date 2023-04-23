package edu.alibaba.mpc4j.s2pc.pso.psic.hfh99;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pso.psic.AbstractPsicClient;
import edu.alibaba.mpc4j.s2pc.pso.psic.PsicUtils;


import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * HFH99 based on ECC PSCI client.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class Hfh99EccPsicClient<T> extends AbstractPsicClient<T> {
	/**
	 * ECC
	 */
	private final Ecc ecc;
	/**
	 * compress encode
	 */
	private final boolean compressEncode;
	/**
	 * PEQT hash
	 */
	private Hash peqtHash;
	/**
	 * client secret key β
	 */
	private BigInteger beta;

	public Hfh99EccPsicClient(Rpc clientRpc, Party serverParty, Hfh99EccPsicConfig config) {
		super(Hfh99EccPsicPtoDesc.getInstance(), clientRpc, serverParty, config);
		ecc = EccFactory.createInstance(envType);
		compressEncode = config.getCompressEncode();
	}

	@Override
	public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
		setInitInput(maxClientElementSize, maxServerElementSize);
		logPhaseInfo(PtoState.INIT_BEGIN);

		stopWatch.start();
		// generate β
		beta = ecc.randomZn(secureRandom);
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
		int peqtByteLength = PsicUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
		peqtHash = HashFactory.createInstance(envType, peqtByteLength);
		// client calculates H(y)^β and randomly permute it , the sends it to server
		List<byte[]> randomlyPermuteHyBetaPayload = generateRandomlyPermuteHyBetaPayload();
		DataPacketHeader hyBetaHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), Hfh99EccPsicPtoDesc.PtoStep.CLIENT_SEND_RANDOMLY_PERMUTED_HY_BETA.ordinal(), extraInfo,
				ownParty().getPartyId(), otherParty().getPartyId()
		);
		rpc.send(DataPacket.fromByteArrayList(hyBetaHeader, randomlyPermuteHyBetaPayload));
		stopWatch.stop();
		long hyBetaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 1, 2, hyBetaTime);
		// client receives H(x)^α, which has been randomly permuted.
		DataPacketHeader randomlyPermutedHxAlphaHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), Hfh99EccPsicPtoDesc.PtoStep.SERVER_SEND_RANDOMLY_PERMUTED_HX_ALPHA.ordinal(), extraInfo,
				otherParty().getPartyId(), ownParty().getPartyId()
		);
		List<byte[]> randomlyPermuteHxAlphaPayload = rpc.receive(randomlyPermutedHxAlphaHeader).getPayload();

		stopWatch.start();
		// client calculates H(H(x)^αβ)
		Set<ByteBuffer> peqtSet = handleHxAlphaPayload(randomlyPermuteHxAlphaPayload);
		// client receives H(H(y)^βα), which has been randomly permuted.
		DataPacketHeader randomlyPermutedPeqtHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), Hfh99EccPsicPtoDesc.PtoStep.CLIENT_SEND_RANDOMLY_PERMUTED_HY_BETA_ALPHA.ordinal(), extraInfo,
				otherParty().getPartyId(), ownParty().getPartyId()
		);
		List<byte[]> randomlyPermutedPeqtPayload = rpc.receive(randomlyPermutedPeqtHeader).getPayload();
		int cardinality = handleRandomlyPermutedPeqtPayload(randomlyPermutedPeqtPayload, peqtSet);

		stopWatch.stop();
		long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 2, 2, peqtTime);

		logPhaseInfo(PtoState.PTO_END);
		return cardinality;
	}

	private List<byte[]> generateRandomlyPermuteHyBetaPayload() {
		Stream<T> clientElementStream = clientElementArrayList.stream();
		clientElementStream = parallel ? clientElementStream.parallel() : clientElementStream;

		List<byte[]> result = clientElementStream
				.map(ObjectUtils::objectToByteArray)
				.map(ecc::hashToCurve)
				.map(p -> ecc.multiply(p, beta))
				.map(p -> ecc.encode(p, compressEncode))
				.collect(Collectors.toList());
		// randomly permute
		Collections.shuffle(result, secureRandom);

		return result;
	}

	private Set<ByteBuffer> handleHxAlphaPayload(List<byte[]> hxAlphaPayload) throws MpcAbortException {
		MpcAbortPreconditions.checkArgument(hxAlphaPayload.size() == serverElementSize);
		Stream<byte[]> hxAlphaStream = hxAlphaPayload.stream();
		hxAlphaStream = parallel ? hxAlphaStream.parallel() : hxAlphaStream;
		return hxAlphaStream
				.map(ecc::decode)
				.map(p -> ecc.multiply(p, beta))
				.map(p -> ecc.encode(p, false))
				.map(p -> peqtHash.digestToBytes(p))
				.map(ByteBuffer::wrap)
				.collect(Collectors.toSet());
	}

	private int handleRandomlyPermutedPeqtPayload(List<byte[]> randomlyPermutedPeqtPayload, Set<ByteBuffer> peqtSet) throws MpcAbortException {
		MpcAbortPreconditions.checkArgument(randomlyPermutedPeqtPayload.size() == clientElementSize);
		Set<ByteBuffer> randomlyPermutedPeqtSet = randomlyPermutedPeqtPayload.stream()
				.map(ByteBuffer::wrap)
				.collect(Collectors.toCollection(HashSet::new));

		// calculate intersection
		peqtSet.retainAll(randomlyPermutedPeqtSet);
		return peqtSet.size();
	}
}
