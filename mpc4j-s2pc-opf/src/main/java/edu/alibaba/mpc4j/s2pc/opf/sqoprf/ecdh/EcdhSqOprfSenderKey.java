package edu.alibaba.mpc4j.s2pc.opf.sqoprf.ecdh;

import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSenderKey;

import java.math.BigInteger;

/**
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public class EcdhSqOprfSenderKey implements SqOprfSenderKey {

    private BigInteger alpha;

    public EcdhSqOprfSenderKey() {

    }

    public EcdhSqOprfSenderKey(BigInteger alpha) {
        this.alpha = alpha;
    }

    public BigInteger getAlpha() {
        return alpha;
    }

    public void setAlpha(BigInteger alpha) {
        this.alpha = alpha;
    }
}
