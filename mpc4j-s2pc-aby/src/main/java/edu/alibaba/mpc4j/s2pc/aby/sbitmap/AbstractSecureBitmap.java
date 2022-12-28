package edu.alibaba.mpc4j.s2pc.aby.sbitmap;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import org.roaringbitmap.RoaringBitmap;

/**
 * Abstract SecureBitmap.
 *
 * @author Weiran Liu
 * @date 2022/12/28
 */
public abstract class AbstractSecureBitmap implements SecureBitmap {
    /**
     * maximal number of bits.
     */
    protected final int maxBitNum;
    /**
     * maximal number of
     */
    protected final int maxContainerNum;
    /**
     * maximal number of bytes.
     */
    protected final int maxByteNum;
    /**
     * the plain state
     */
    protected final boolean plain;
    /**
     * the plain RoaringBitMap
     */
    protected final RoaringBitmap roaringBitmap;

    protected AbstractSecureBitmap(int maxBitNum, RoaringBitmap roaringBitmap) {
        RoaringBitmapUtils.checkValidBitNum(maxBitNum);
        this.maxBitNum = maxBitNum;
        maxContainerNum = RoaringBitmapUtils.getContainerNum(maxBitNum);
        maxByteNum = RoaringBitmapUtils.getByteNum(maxBitNum);
        // check all keys are in range [0, maxContainerNum)
        char[] keys = RoaringBitmapUtils.getKeyCharArray(roaringBitmap);
        for (char key : keys) {
            MathPreconditions.checkNonNegativeInRange("key", key, maxContainerNum);
        }
        this.roaringBitmap = roaringBitmap;
        plain = true;
    }

    protected AbstractSecureBitmap(int maxBitNum) {
        RoaringBitmapUtils.checkValidBitNum(maxBitNum);
        this.maxBitNum = maxBitNum;
        maxContainerNum = RoaringBitmapUtils.getContainerNum(maxBitNum);
        maxByteNum = RoaringBitmapUtils.getByteNum(maxBitNum);
        this.roaringBitmap = null;
        plain = false;
    }

    @Override
    public int maxContainerNum() {
        return maxContainerNum;
    }

    @Override
    public int maxBitNum() {
        return maxBitNum;
    }

    @Override
    public int maxByteNum() {
        return maxByteNum;
    }

    @Override
    public boolean isPlain() {
        return plain;
    }

    @Override
    public RoaringBitmap toRoaringBitmap() {
        Preconditions.checkArgument(plain, "SecureBitmap must be in plain state");
        return roaringBitmap;
    }
}
