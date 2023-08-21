package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container;

import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * @author Li Peng
 * @date 2023/8/20
 */
public class SecureContainer implements Container{
    SquareZ2Vector container;

    @Override
    public SquareZ2Vector getSecureVector() {
        return container;
    }
}
