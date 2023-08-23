package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * Container of bitmap in private state.
 *
 * @author Li Peng
 * @date 2023/8/20
 */
public class SecureContainer implements Container {
    /**
     * container
     */
    SquareZ2Vector container;

    /**
     * private constructor
     */
    private SecureContainer() {
        // empty
    }

    public static SecureContainer create(SquareZ2Vector container) {
        SecureContainer secureContainer = new SecureContainer();
        secureContainer.container = container;
        return secureContainer;
    }

    public static SecureContainer create(BitVector container, boolean plain) {
        SecureContainer secureContainer = new SecureContainer();
        secureContainer.container = SquareZ2Vector.create(container, plain);
        return secureContainer;
    }

    @Override
    public SquareZ2Vector toSecureVector() {
        return container;
    }

    @Override
    public BitVector getBitVector() {
        return container.getBitVector();
    }

    @Override
    public Container copy() {
        SquareZ2Vector squareZ2Vector = container.copy();
        return SecureContainer.create(squareZ2Vector);
    }

    @Override
    public boolean isPlain() {
        return false;
    }
}
