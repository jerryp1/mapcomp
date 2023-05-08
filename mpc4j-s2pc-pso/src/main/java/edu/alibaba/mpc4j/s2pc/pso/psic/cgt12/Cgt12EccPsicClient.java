package edu.alibaba.mpc4j.s2pc.pso.psic.cgt12;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CGT12 based on ECC PSCI client.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class Cgt12EccPsicClient<T> extends AbstractPsicClient<T> {
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

	public Cgt12EccPsicClient(Rpc clientRpc, Party serverParty, Cgt12EccPsicConfig config) {
		super(Cgt12EccPsicPtoDesc.getInstance(), clientRpc, serverParty, config);
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
		// client calculates H(y)^β, which not been randomly permuted.
		List<byte[]> hyBetaPayload = generateHyBetaPayload();
		DataPacketHeader hyBetaHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), Cgt12EccPsicPtoDesc.PtoStep.CLIENT_SEND_HY_BETA.ordinal(), extraInfo,
				ownParty().getPartyId(), otherParty().getPartyId()
		);
		rpc.send(DataPacket.fromByteArrayList(hyBetaHeader, hyBetaPayload));
		stopWatch.stop();
		long hyBetaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 1, 2, hyBetaTime);

		// client receives H(x)^α. Note that this value is after peqt hash.
		DataPacketHeader hxAlphaPeqtHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), Cgt12EccPsicPtoDesc.PtoStep.SERVER_SEND_RANDOMLY_PERMUTED_HX_ALPHA.ordinal(), extraInfo,
				otherParty().getPartyId(), ownParty().getPartyId()
		);
		List<byte[]> hxAlphaPeqtPayload = rpc.receive(hxAlphaPeqtHeader).getPayload();

		stopWatch.start();
		// client receives H(H(y)^βα)
		DataPacketHeader hyBeatAlphaHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), Cgt12EccPsicPtoDesc.PtoStep.CLIENT_SEND_RANDOMLY_PERMUTED_HY_BETA_ALPHA.ordinal(), extraInfo,
				otherParty().getPartyId(), ownParty().getPartyId()
		);
		List<byte[]> hyBeatAlphaPayload = rpc.receive(hyBeatAlphaHeader).getPayload();
		// (y) ^ beta * alpha * beta_inv
		Set<ByteBuffer> peqtSet = handleHyBetaAlphaPayload(hyBeatAlphaPayload);
		int cardinality = calculateInteractionCardinality(hxAlphaPeqtPayload, peqtSet);
		stopWatch.stop();
		long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 2, 2, peqtTime);

		logPhaseInfo(PtoState.PTO_END);
		return cardinality;
	}

	private List<byte[]> generateHyBetaPayload() {
		Stream<T> clientElementStream = clientElementArrayList.stream();
		clientElementStream = parallel ? clientElementStream.parallel() : clientElementStream;

		// Note that no random permutation is required here
		return clientElementStream
				.map(ObjectUtils::objectToByteArray)
				.map(ecc::hashToCurve)
				.map(p -> ecc.multiply(p, beta))
				.map(p -> ecc.encode(p, compressEncode))
				.collect(Collectors.toList());
	}

	private Set<ByteBuffer> handleHyBetaAlphaPayload(List<byte[]> hyBetaAlphaPayload) throws MpcAbortException {
		MpcAbortPreconditions.checkArgument(hyBetaAlphaPayload.size() == clientElementSize);
		Stream<byte[]> hxAlphaStream = hyBetaAlphaPayload.stream();
		hxAlphaStream = parallel ? hxAlphaStream.parallel() : hxAlphaStream;
		BigInteger betaInv = beta.modInverse(ecc.getN());

		return hxAlphaStream
				.map(ecc::decode)
				.map(p -> ecc.multiply(p, betaInv))
				.map(p -> ecc.encode(p, false))
				.map(p -> peqtHash.digestToBytes(p))
				.map(ByteBuffer::wrap)
				.collect(Collectors.toSet());
	}

	private int calculateInteractionCardinality(List<byte[]> hxAlphaPeqtPayload, Set<ByteBuffer> peqtSet) throws MpcAbortException {
		MpcAbortPreconditions.checkArgument(hxAlphaPeqtPayload.size() == serverElementSize);
		Set<ByteBuffer> hxAlphaPeqtSet = hxAlphaPeqtPayload.stream()
				.map(ByteBuffer::wrap)
				.collect(Collectors.toCollection(HashSet::new));
		// calculate intersection
		peqtSet.retainAll(hxAlphaPeqtSet);
		return peqtSet.size();
	}

}