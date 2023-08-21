package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * @author Li Peng
 * @date 2023/8/20
 */
public class PlainContainer implements Container{
    BitVector container;

    @Override
    public SquareZ2Vector getSecureVector() {
        return SquareZ2Vector.create(container, true);
    }
}
