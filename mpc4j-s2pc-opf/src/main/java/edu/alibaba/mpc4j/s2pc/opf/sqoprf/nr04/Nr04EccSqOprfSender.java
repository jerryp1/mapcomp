package edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.AbstractSqOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.Ra17EccSqOprfKey;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.bouncycastle.math.ec.ECPoint;

import javax.swing.*;

/**
 * NR04 ECC single-query OPRF secner.
 *
 * @author Qixian Zhou
 * @date 2023/4/12
 */
public class Nr04EccSqOprfSender extends AbstractSqOprfSender {

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
	private final CoreCotSender coreCotSender;
	/**
	 * cotSenderOutput
	 */
	private CotSenderOutput cotSenderOutput;

	/**
	 * 密钥
	 */
	private Nr04EccSqOprfKey key;
	/**
	 * R_{i,j}^0, length: n * N_C^{max}
	 */
	private BigInteger[][] r0Array;
	/**
	 * R_{i,j}^1, length: n * N_C^{max}
	 */
	private BigInteger[][] r1Array;

	public Nr04EccSqOprfSender(Rpc senderRpc, Party receiverParty, Nr04EccSqOprfConfig config) {
		super(Nr04EccSqOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
		ecc = EccFactory.createInstance(envType);
		compressEncode = config.getCompressEncode();
		coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
		addSubPtos(coreCotSender);

		key = null;
		cotSenderOutput = null;
	}


	@Override
	public void init(int maxBatchSize, SqOprfKey key) throws MpcAbortException {

		setInitInput(maxBatchSize);
		logPhaseInfo(PtoState.INIT_BEGIN);

		// 1. sets the key
		stopWatch.start();
		this.key = (Nr04EccSqOprfKey) key;
		stopWatch.stop();
		long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.INIT_STEP, 1, 3, initTime);

		// 2. generate n * N_C^{max} COTs
		stopWatch.start();
		byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
		secureRandom.nextBytes(delta);
		coreCotSender.init(delta, maxBatchSize * CommonConstants.BLOCK_BIT_LENGTH);
		cotSenderOutput = coreCotSender.send(maxBatchSize * CommonConstants.BLOCK_BIT_LENGTH);
		stopWatch.stop();
		long generateCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.INIT_STEP, 1, 2, generateCotTime);

		logPhaseInfo(PtoState.INIT_END);
	}

	@Override
	public void oprf(int batchSize) throws MpcAbortException {

		setPtoInput(batchSize);
		logPhaseInfo(PtoState.PTO_BEGIN);
		// 1. calculate grInvArray and send grInvArray
		stopWatch.start();
		List<byte[]> grInvPayload = generateGrInvPayload();
		DataPacketHeader grInvHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), Nr04EccSqOprfPtoDesc.PtoStep.SENDER_SEND_GR_INV.ordinal(), extraInfo,
				ownParty().getPartyId(), otherParty().getPartyId()
		);
		rpc.send(DataPacket.fromByteArrayList(grInvHeader, grInvPayload));
		stopWatch.stop();
		long sendGrInvTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 1, 2, sendGrInvTime, "Sender sends GR Inv.");

		// 2. sender receive choice bit, which equals bi = ri \oplus yi, ri is random COT choice bit,
		stopWatch.start();
		DataPacketHeader binaryHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), Nr04EccSqOprfPtoDesc.PtoStep.RECEIVER_SEND_BINARY.ordinal(), extraInfo,
				otherParty().getPartyId(), ownParty().getPartyId()
		);
		List<byte[]> binaryPayload = rpc.receive(binaryHeader).getPayload();
		List<byte[]> messagePayload = handleBinaryPayload(binaryPayload);
		// sends messagePayload
		DataPacketHeader messageHeader = new DataPacketHeader(
				encodeTaskId, getPtoDesc().getPtoId(), Nr04EccSqOprfPtoDesc.PtoStep.SENDER_SEND_MESSAGE.ordinal(), extraInfo,
				ownParty().getPartyId(), otherParty().getPartyId()
		);
		rpc.send(DataPacket.fromByteArrayList(messageHeader, messagePayload));
		long messageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		stopWatch.reset();
		logStepInfo(PtoState.PTO_STEP, 2, 2, messageTime, "Sender sends messages");


		logPhaseInfo(PtoState.PTO_END);
	}


	private List<byte[]> handleBinaryPayload(List<byte[]> binaryPayload) throws MpcAbortException {

		MpcAbortPreconditions.checkArgument(binaryPayload.size() == batchSize);

		Crhf crhf = CrhfFactory.createInstance(envType, CrhfFactory.CrhfType.MMO);
		Prg prg = PrgFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);

		IntStream batchStream = IntStream.range(0, batchSize);
		batchStream = parallel ? batchStream.parallel() : batchStream;
		return batchStream
				.mapToObj(index -> {
					List<byte[]> result = new ArrayList(CommonConstants.BLOCK_BIT_LENGTH * 2);
					// Each index corresponds to one element, namely 16 bytes.
					// Bit-by-bit processing
					for (int bitPos = 0; bitPos < CommonConstants.BLOCK_BIT_LENGTH; bitPos++) {
						// H(r0 \oplus bi \Delta)
						boolean bi = BinaryUtils.getBoolean(binaryPayload.get(index), bitPos);
						// key0 = r0 \oplus bi \Delta
						// if bi = true, then key0 = r1, else key0 = r0
						byte[] key0 = bi ? BytesUtils.clone(cotSenderOutput.getR1(index * CommonConstants.BLOCK_BIT_LENGTH + bitPos)) : BytesUtils.clone(cotSenderOutput.getR0(index * CommonConstants.BLOCK_BIT_LENGTH + bitPos));
						key0 = crhf.hash(key0);
						key0 = prg.extendToBytes(key0);

						// key1 = r0 \oplus ~bi \Delta
						// if bi = true, then key1 = r0, else key1 = r1
						byte[] key1 = bi ? BytesUtils.clone(cotSenderOutput.getR0(index * CommonConstants.BLOCK_BIT_LENGTH + bitPos)) : BytesUtils.clone(cotSenderOutput.getR1(index * CommonConstants.BLOCK_BIT_LENGTH + bitPos));
						key1 = crhf.hash(key1);
						key1 = prg.extendToBytes(key1);

						// note that key0 mask R0, key1 mask R1
						byte[] message0 = BigIntegerUtils.nonNegBigIntegerToByteArray(r0Array[index][bitPos], 2 * CommonConstants.BLOCK_BYTE_LENGTH);
						BytesUtils.xori(message0, key0);
						byte[] message1 = BigIntegerUtils.nonNegBigIntegerToByteArray(r1Array[index][bitPos], 2 * CommonConstants.BLOCK_BYTE_LENGTH);
						BytesUtils.xori(message1, key1);

						result.add(message0);
						result.add(message1);
					}
					return result.toArray(new byte[0][]);
				})
				.flatMap(Arrays::stream)
				.collect(Collectors.toList());
	}


	/**
	 * g_i = g^{r_i^{inv}}
	 *
	 * @return GrInvPayload
	 */
	private List<byte[]> generateGrInvPayload() {

		r0Array = new BigInteger[maxBatchSize][CommonConstants.BLOCK_BIT_LENGTH];
		r1Array = new BigInteger[maxBatchSize][CommonConstants.BLOCK_BIT_LENGTH];

		IntStream maxBatchIntStream = IntStream.range(0, maxBatchSize);
		maxBatchIntStream = parallel ? maxBatchIntStream.parallel() : maxBatchIntStream;

		return maxBatchIntStream
				.mapToObj(i -> {
					BigInteger ri = BigInteger.ONE;
					// inner loop can be streamed?
					for (int j = 0; j < CommonConstants.BLOCK_BIT_LENGTH; j++) {
						byte[] rByte = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
						secureRandom.nextBytes(rByte);
						// Whether we need to ensure that the n-length of the n ?
//						rByte[0] |= (byte)0b10000000;//ensure that the highest bit is 1.
						BigInteger r = BigIntegerUtils.byteArrayToNonNegBigInteger(rByte);
						// n-bit * n-bit = 2n, So we use prg to extend the key length to 32-byte
						r0Array[i][j] = r.multiply(key.getA0Array(j));
						r1Array[i][j] = r.multiply(key.getA1Array(j));
						// Accumulative multiplication
						ri = ri.multiply(r).mod(ecc.getN());
					}
					return ri.modInverse(ecc.getN());
				})
				.map(riInv -> ecc.encode(ecc.multiply(ecc.getG(), riInv), compressEncode))
				.collect(Collectors.toList());

	}


	@Override
	public SqOprfKey keyGen() {

		BigInteger[] a0Array = new BigInteger[CommonConstants.BLOCK_BIT_LENGTH];
		BigInteger[] a1Array = new BigInteger[CommonConstants.BLOCK_BIT_LENGTH];
		byte[] a0 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
		byte[] a1 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];

		for (int i = 0; i < CommonConstants.BLOCK_BIT_LENGTH; i++) {
			// generate a random value with n-bit
			secureRandom.nextBytes(a0);
//			a0[0] |= (byte) 0b10000000; // ensure highest bit is 1
			a0Array[i] = BigIntegerUtils.byteArrayToNonNegBigInteger(a0);
			secureRandom.nextBytes(a1);
//			a1[0] |= (byte) 0b10000000;
			a1Array[i] = BigIntegerUtils.byteArrayToNonNegBigInteger(a1);
		}

		return new Nr04EccSqOprfKey(envType, a0Array, a1Array);
	}

}
