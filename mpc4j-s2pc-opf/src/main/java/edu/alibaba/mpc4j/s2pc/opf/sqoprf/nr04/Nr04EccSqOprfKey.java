package edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
//import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSenderKey;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * NR04 ECC single-query OPRF key.
 *
 * @author Qixian Zhou
 * @date 2023/4/12
 */
public class Nr04EccSqOprfKey implements SqOprfKey {


	/**
	 * ECC
	 */
	private final Ecc ecc;
	/**
	 * key derivation function
	 */
	private final Kdf kdf;
	/**
	 * a0 = (a_1^0, a_2^0, ..., a_n^0)
	 * a_i^0 is n-bit random number, n is the bit-length of input
	 * The default value of n is 128.
	 */
	private BigInteger[] a0Array;

	/**
	 * a1 = (a_1^1, a_2^1, ..., a_n^1)
	 * a_i^1 is n-bit random number, n is the bit-length of input
	 * The default value of n is 128.
	 */
	private BigInteger[] a1Array;

	public BigInteger getA0Array(int index) {
		return a0Array[index];
	}

	public BigInteger getA1Array(int index) {
		return a1Array[index];
	}

	/**
	 * @param envType EnvType
	 * @param a0Array
	 * @param a1Array
	 */
	public Nr04EccSqOprfKey(EnvType envType, BigInteger[] a0Array, BigInteger[] a1Array) {
		// require the length of a0Array and a1Array is n
		assert a0Array.length == CommonConstants.BLOCK_BIT_LENGTH && a1Array.length == CommonConstants.BLOCK_BIT_LENGTH;
		ecc = EccFactory.createInstance(envType);
		kdf = KdfFactory.createInstance(envType);

		this.a0Array = Arrays.stream(a0Array)
				.peek(a0 -> {
					assert a0.bitLength() == CommonConstants.BLOCK_BIT_LENGTH
							: "a0 bit length must be equal to " + CommonConstants.BLOCK_BIT_LENGTH + ": " + a0.bitLength();
				})
				.toArray(BigInteger[]::new);
		this.a1Array = Arrays.stream(a1Array)
				.peek(a1 -> {
					assert a1.bitLength() == CommonConstants.BLOCK_BIT_LENGTH
							: "a0 bit length must be equal to " + CommonConstants.BLOCK_BIT_LENGTH + ": " + a1.bitLength();
				})
				.toArray(BigInteger[]::new);
	}

	@Override
	public byte[] getPrf(byte[] input) {

		assert input.length == CommonConstants.BLOCK_BYTE_LENGTH;
		// C = a_0^{x[0]} * a_1^{x[1]} * .... * a_n^{x[n]} mod q
		// x[i] represents the i-th bit of input
		BigInteger c = BigInteger.ONE;
		for (int i = 0; i < CommonConstants.BLOCK_BIT_LENGTH; i++) {
			if (BinaryUtils.getBoolean(input, i)) {
				c = c.multiply(a1Array[i]).mod(ecc.getN());
			} else {
				c = c.multiply(a0Array[i]).mod(ecc.getN());
			}
		}
		// C * BasePoint
		return kdf.deriveKey(ecc.encode(ecc.multiply(ecc.getG(), c), false));
	}


}
