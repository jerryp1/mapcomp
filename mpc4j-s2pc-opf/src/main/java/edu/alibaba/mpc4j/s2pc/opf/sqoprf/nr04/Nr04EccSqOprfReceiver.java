package edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.AbstractSqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * NR04 ECC single-query OPRF receiver.
 *
 * @author Qixian Zhou
 * @date 2023/4/12
 */
public class Nr04EccSqOprfReceiver extends AbstractSqOprfReceiver {

	/**
	 * 椭圆曲线
	 */
	private final Ecc ecc;
	/**
	 * 是否压缩编码
	 */
	private final boolean compressEncode;

	/**
	 * 核COT协议 发送方
	 */
	private final CoreCotReceiver coreCotReceiver;
	/**
	 * cotReceiverOutput
	 */
	private CotReceiverOutput cotReceiverOutput;
	/**
	 * randomChoices for Pre-compute COTs
	 */
	private byte[] randomByteChoices;

	/**
	 * store g^{r_i ^{inv}}
	 */
	private ECPoint[] grInvPoints;


	public Nr04EccSqOprfReceiver(Rpc receiverRpc, Party senderParty, Nr04EccSqOprfConfig config) {
		super(Nr04EccSqOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
		coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
		addSubPtos(coreCotReceiver);

		ecc = EccFactory.createInstance(envType);
		compressEncode = config.getCompressEncode();
		cotReceiverOutput = null;
	}

	@Override
	public void init(int maxBatchSize) throws MpcAbortException {
		setInitInput(maxBatchSize);
		logPhaseInfo(PtoState.INIT_BEGIN);

		// 1. generate n * N_C^{max} COTs
		stopWatch.start();
		coreCotReceiver.init(maxBatchSize * CommonConstants.BLOCK_BIT_LENGTH);
		// random choices
		int byteLength = CommonUtils.getByteLength(CommonConstants.BLOCK_BIT_LENGTH * maxBatchSize);
		randomByteChoices = new byte[byteLength];
		secureRandom.nextBytes(randomByteChoices);
		// run COT
		cotReceiverOutput = coreCotReceiver.receive(BinaryUtils.byteArrayToBinary(randomByteChoices));
		stopWatch.stop();
		long generateCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.INIT_STEP, 1, 1, generateCotTime);

		logPhaseInfo(PtoState.INIT_END);
	}

	@Override
	public SqOprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException {
		setPtoInput(inputs);
		// each length of input should be 16-byte
		Arrays.stream(inputs).forEach((input) -> {
			MathPreconditions.checkEqual("input", "block byte length", input.length, CommonConstants.BLOCK_BYTE_LENGTH);
//			assert input.length == CommonConstants.BLOCK_BYTE_LENGTH : "each length of input should be " + CommonConstants.BLOCK_BYTE_LENGTH + ":" + input.length;
		});

		logPhaseInfo(PtoState.PTO_BEGIN);
		// 1. receive grInvArray and convert these to EcPoint array
		stopWatch.start();
		DataPacketHeader grInvHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), Nr04EccSqOprfPtoDesc.PtoStep.SENDER_SEND_GR_INV.ordinal(), extraInfo,
				otherParty().getPartyId(), ownParty().getPartyId()
		);
		List<byte[]> grInvPayload = rpc.receive(grInvHeader).getPayload();
		grInvPoints = handleGrInvPayload(grInvPayload).toArray(new ECPoint[0]);
		stopWatch.stop();
		long receiveGrInvTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 1, 3, receiveGrInvTime, "Receiver receives GR Inv.");

		// 2. generate binaryPayload and sends to Sender
		stopWatch.start();
		DataPacketHeader binaryHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), Nr04EccSqOprfPtoDesc.PtoStep.RECEIVER_SEND_BINARY.ordinal(), extraInfo,
				ownParty().getPartyId(), otherParty().getPartyId()
		);
		List<byte[]> binaryPayload = generateBinaryPayload(inputs);
		rpc.send(DataPacket.fromByteArrayList(binaryHeader, binaryPayload));
		stopWatch.stop();
		long binaryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 2, 3, binaryTime, "Receiver sends binary payload.");

		// 3. receive message payload
		stopWatch.start();
		DataPacketHeader messageHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), Nr04EccSqOprfPtoDesc.PtoStep.SENDER_SEND_MESSAGE.ordinal(), extraInfo,
				otherParty().getPartyId(), ownParty().getPartyId()
		);
		List<byte[]> messagePayload = rpc.receive(messageHeader).getPayload();
		// get R^{y_i[j]}
		BigInteger[] rArray = handleMessagePayload(messagePayload);
		SqOprfReceiverOutput receiverOutput = generateReceiverOutput(rArray);
		stopWatch.stop();
		long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 3, 3, oprfTime, "Receiver receives message payload and generate oprfs.");

		logPhaseInfo(PtoState.PTO_END);

		return receiverOutput;
	}

	private SqOprfReceiverOutput generateReceiverOutput(BigInteger[] rArray) throws MpcAbortException {

		MpcAbortPreconditions.checkArgument(rArray.length == batchSize * CommonConstants.BLOCK_BIT_LENGTH);
		IntStream batchStream = IntStream.range(0, batchSize);
		batchStream = parallel ? batchStream.parallel() : batchStream;

		Kdf kdf = KdfFactory.createInstance(envType);
		byte[][] prfs = new byte[batchSize][];
		batchStream.forEach(index -> {
			// One index corresponds to 128 bits
			// C = R^{y_i[j]}
			BigInteger c = BigInteger.ONE;
			for (int j = 0; j < CommonConstants.BLOCK_BIT_LENGTH; j++) {
				// Directly multiply, because we have already dealt with the exponent.
				c = c.multiply(rArray[index * CommonConstants.BLOCK_BIT_LENGTH + j]).mod(ecc.getN());
			}
			// c * grInv
			prfs[index] = kdf.deriveKey(ecc.encode(ecc.multiply(grInvPoints[index], c), false));

		});

		return new SqOprfReceiverOutput(inputs, prfs);
	}


	private BigInteger[] handleMessagePayload(List<byte[]> messagePayload) throws MpcAbortException {
		Crhf crhf = CrhfFactory.createInstance(envType, CrhfFactory.CrhfType.MMO);
		Prg prg = PrgFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);

		MpcAbortPreconditions.checkArgument(messagePayload.size() == 2 * batchSize * CommonConstants.BLOCK_BIT_LENGTH);

		IntStream batchStream = IntStream.range(0, batchSize * CommonConstants.BLOCK_BIT_LENGTH);
		batchStream = parallel ? batchStream.parallel() : batchStream;

		byte[][] messageArray = messagePayload.toArray(new byte[0][]);

		return batchStream.mapToObj(index -> {

			byte[] key = cotReceiverOutput.getRb(index);
			key = crhf.hash(key);
			key = prg.extendToBytes(key);
			// choose right message, which can be successfully decrypted
			// y_i as choices bit
			boolean yi = BinaryUtils.getBoolean(inputs[index / CommonConstants.BLOCK_BIT_LENGTH], index % CommonConstants.BLOCK_BIT_LENGTH);
			byte[] message = yi ? messageArray[2 * index + 1] : messageArray[2 * index];
			BytesUtils.xori(message, key);
			return BigIntegerUtils.byteArrayToNonNegBigInteger(message);
		}).toArray(BigInteger[]::new);
	}


	private List<ECPoint> handleGrInvPayload(List<byte[]> grInvPayload) throws MpcAbortException {

		MpcAbortPreconditions.checkArgument(grInvPayload.size() == maxBatchSize);

		Stream<byte[]> grInvStream = grInvPayload.stream();
		grInvStream = parallel ? grInvStream.parallel() : grInvStream;
		return grInvStream
				// decode g^{r_i ^{inv}}
				.map(ecc::decode)
				.collect(Collectors.toList());
	}


	private List<byte[]> generateBinaryPayload(byte[][] inputs) throws MpcAbortException {
		MpcAbortPreconditions.checkArgument(inputs.length == batchSize);

		IntStream batchStream = IntStream.range(0, inputs.length);
		batchStream = parallel ? batchStream.parallel() : batchStream;
		return batchStream.mapToObj(index -> {
					// bit-wise xor: bi = ri \oplus yi
					byte[] curRandomChoies = Arrays.copyOfRange(randomByteChoices, index * CommonConstants.BLOCK_BYTE_LENGTH, (index + 1) * CommonConstants.BLOCK_BYTE_LENGTH);
					return BytesUtils.xor(curRandomChoies, inputs[index]);
				})
				.collect(Collectors.toList());
	}


}
