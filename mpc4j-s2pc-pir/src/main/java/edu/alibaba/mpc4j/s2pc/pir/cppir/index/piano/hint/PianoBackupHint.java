package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.coder.linear.AbstractBchCoder;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * backup hint for PIANO PIR, which contains a PRF key and a parity under the punctured index.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class PianoBackupHint extends AbstractPianoHint {
    /**
     * punctured chunk index
     */
    private final int puncturedChunkId;

    /**
     * Creates the hints with a random PRF key.
     *
     * @param envType      environment.
     * @param chunkSize    chunk size.
     * @param chunkNum     chunk num.
     * @param l            parity bit length.
     * @param secureRandom the random state.
     */
    public PianoBackupHint(EnvType envType, int chunkSize, int chunkNum, int l,
                           int puncturedChunkId, SecureRandom secureRandom) {
        super(envType, chunkSize, chunkNum, l);
        MathPreconditions.checkNonNegativeInRange("puncturedChunkId", puncturedChunkId, chunkNum);
        this.puncturedChunkId = puncturedChunkId;
        byte[] prfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(prfKey);
        prf.setKey(prfKey);
    }

    /**
     * Gets the punctured chunk index.
     *
     * @return the punctured chunk index.
     */
    public int getPuncturedChunkId() {
        return puncturedChunkId;
    }

    @Override
    public int expandOffset(int chunkId) {
        MathPreconditions.checkNonNegativeInRange("chunk ID", chunkId, chunkNum);
        Preconditions.checkArgument(chunkId != puncturedChunkId);
        byte[] chunkIdByteArray = IntUtils.intToByteArray(chunkId);
        return prf.getInteger(chunkIdByteArray, chunkSize);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(envType)
            .append(chunkSize)
            .append(chunkNum)
            .append(l)
            .append(prf.getKey())
            .append(parity)
            .append(puncturedChunkId)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PianoBackupHint)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        PianoBackupHint that = (PianoBackupHint)obj;
        return new EqualsBuilder()
            .append(this.envType, that.envType)
            .append(this.chunkSize, that.chunkSize)
            .append(this.chunkNum, that.chunkNum)
            .append(this.l, that.l)
            .append(this.prf.getKey(), that.prf.getKey())
            .append(this.parity, that.parity)
            .append(this.puncturedChunkId, that.puncturedChunkId)
            .isEquals();
    }
}
