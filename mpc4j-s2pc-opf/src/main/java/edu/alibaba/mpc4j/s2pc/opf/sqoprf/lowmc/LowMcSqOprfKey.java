package edu.alibaba.mpc4j.s2pc.opf.sqoprf.lowmc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;

/**
 * PSSW09 based LowMc single-query OPRF key.
 *
 * @author Qixian Zhou
 * @date 2023/4/17
 */
public class LowMcSqOprfKey implements SqOprfKey {

	/**
	 * key derivation function
	 */
	private final Kdf kdf;

	/**
	 * prp
	 */
	private final Prp prp;
	/**
	 * is inv prp, wihich decides prp evaluate option
	 */
	private boolean isInvPrp;

	/**
	 * key for OprpSender.oprp, because we cannot get the key from Prp object, so I define a key here.
	 */
	private byte[] oprpKey;


	public LowMcSqOprfKey(EnvType envType, byte[] key, boolean isInvPrp, PrpFactory.PrpType prpType) {

		assert key.length == CommonConstants.BLOCK_BYTE_LENGTH : "key length must be: " + CommonConstants.BLOCK_BYTE_LENGTH + ":" + key.length;

		this.kdf = KdfFactory.createInstance(envType);
		this.prp = PrpFactory.createInstance(prpType);
		this.isInvPrp = isInvPrp;
		this.prp.setKey(key);
		this.oprpKey = BytesUtils.clone(key);
	}

	@Override
	public byte[] getPrf(byte[] input) {
		return isInvPrp ? kdf.deriveKey(prp.invPrp(input)) : kdf.deriveKey(prp.prp(input));
	}


	public byte[] getOprpKey() {
		return oprpKey;
	}

}
