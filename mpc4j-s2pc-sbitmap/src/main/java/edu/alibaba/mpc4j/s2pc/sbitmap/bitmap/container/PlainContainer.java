package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * Container of bitmap in plain state.
 *
 * @author Li Peng
 * @date 2023/8/20
 */
public class PlainContainer implements Container {
    /**
     * container.
     */
    BitVector container;

    /**
     * private constructor
     */
    private PlainContainer() {
        // empty
    }

    public static PlainContainer create(BitVector bitVector) {
        PlainContainer plainContainer = new PlainContainer();
        plainContainer.container = bitVector;
        return plainContainer;
    }

    @Override
    public SquareZ2Vector toSecureVector() {
        return SquareZ2Vector.create(container, true);
    }

    @Override
    public BitVector getBitVector() {
        return container;
    }

    @Override
    public Container copy() {
        BitVector bitVector = container.copy();
        return create(bitVector);
    }

    @Override
    public boolean isPlain() {
        return true;
    }

    public void set(int x, boolean value) {
        this.container.set(x, value);
    }

    public int bitCount() {
        return container.bitNum();
    }
}
