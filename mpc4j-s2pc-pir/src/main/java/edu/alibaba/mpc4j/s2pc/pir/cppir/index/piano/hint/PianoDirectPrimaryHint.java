package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

import java.security.SecureRandom;

/**
 * directly generated primary hint for PIANO PIR, which contains a PRF key and a parity and indexes in the set are all
 * from the PRF key.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class PianoDirectPrimaryHint extends AbstractPianoHint implements PianoPrimaryHint {
    /**
     * Creates the hints with a random PRF key.
     *
     * @param envType      environment.
     * @param chunkSize    chunk size.
     * @param chunkNum     chunk num.
     * @param l            parity bit length.
     * @param secureRandom the random state.
     */
    public PianoDirectPrimaryHint(EnvType envType, int chunkSize, int chunkNum, int l,
                                  SecureRandom secureRandom) {
        super(envType, chunkSize, chunkNum, l);
        byte[] prfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(prfKey);
        prf.setKey(prfKey);
    }

    @Override
    public int expandOffset(int chunkId) {
        MathPreconditions.checkNonNegativeInRange("chunk ID", chunkId, chunkNum);
        byte[] chunkIdByteArray = IntUtils.intToByteArray(chunkId);
        return prf.getInteger(chunkIdByteArray, chunkSize);
    }
}
