/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ConstantsTest {
    @Test
    public void checkEdwardsD() {
        assertThat(Constants.EDWARDS_D, is(CafeFieldElement
                .decode(Hex.decode("a3785913ca4deb75abd841414d0a700098e879777940c78c73fe6f2bee6c0352"))));
    }

    @Test
    public void checkEdwards2D() {
        CafeFieldElement two = CafeFieldElement.ONE_INTS.add(CafeFieldElement.ONE_INTS);
        assertThat(Constants.EDWARDS_2D, is(Constants.EDWARDS_D.mul(two)));
    }

    @Test
    public void checkSqrtADMinusOne() {
        assertThat(Constants.SQRT_AD_MINUS_ONE.sqr().add(CafeFieldElement.ONE_INTS).negate(), is(Constants.EDWARDS_D));
    }

    @Test
    public void checkInvSqrtAMinusD() {
        assertThat(Constants.INVSQRT_A_MINUS_D.inv().sqr().add(CafeFieldElement.ONE_INTS).negate(),
                is(Constants.EDWARDS_D));
    }

    @Test
    public void checkSqrtM1() {
        assertThat(Constants.SQRT_M1, is(CafeFieldElement
                .decode(Hex.decode("b0a00e4a271beec478e42fad0618432fa7d7fb3d99004d2b0bdfc14f8024832b"))));
    }

    @Test
    public void checkEd25519Basepoint() throws InvalidEncodingException {
        CompressedEdwardsY encoded = new CompressedEdwardsY(
                Hex.decode("5866666666666666666666666666666666666666666666666666666666666666"));
        EdwardsPoint B = encoded.decompress();
        assertThat(Constants.ED25519_BASEPOINT.X, is(B.X));
        assertThat(Constants.ED25519_BASEPOINT.Y, is(B.Y));
        assertThat(Constants.ED25519_BASEPOINT.Z, is(B.Z));
        assertThat(Constants.ED25519_BASEPOINT.T, is(B.T));
    }
}
