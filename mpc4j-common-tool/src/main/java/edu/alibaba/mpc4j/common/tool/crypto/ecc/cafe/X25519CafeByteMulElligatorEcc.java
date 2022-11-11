package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteMulElligatorEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.utils.X25519ByteEccUtils;

import java.security.SecureRandom;

/**
 * Cafe实现的Ristretto全功能字节椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2022/11/11
 */
public class X25519CafeByteMulElligatorEcc implements ByteMulElligatorEcc {

    public X25519CafeByteMulElligatorEcc() {
        // empty
    }

    @Override
    public byte[] randomScalar(SecureRandom secureRandom) {
        return X25519ByteEccUtils.randomClampScalar(secureRandom);
    }

    @Override
    public ByteEccFactory.ByteEccType getByteEccType() {
        return ByteEccFactory.ByteEccType.X25519_CAFE;
    }

    @Override
    public boolean baseMul(final byte[] k, byte[] result, byte[] uniformResult) {
        assert X25519ByteEccUtils.checkClampScalar(k);
        assert result.length == X25519ByteEccUtils.POINT_BYTES;
        assert uniformResult.length == X25519ByteEccUtils.POINT_BYTES;

        CafeScalar cafeScalarK = new CafeScalar(k);
        // crypto_scalarmult_ristretto255_base(AAbytes, privateKey);
        CafeEdwardsPoint repr = CafeConstants.ED25519_BASE_POINT_TABLE.mul(cafeScalarK);

        // edwards25519.FeSub(&inv1, &A.Z, &A.Y)
        CafeFieldElement inv1 = repr.z.sub(repr.y);
        // edwards25519.FeMul(&inv1, &inv1, &A.X)
        inv1 = inv1.mul(repr.x);
        // edwards25519.FeInvert(&inv1, &inv1)
        inv1 = inv1.inv();

        // edwards25519.FeMul(&u, &inv1, &A.X)
        CafeFieldElement u = inv1.mul(repr.x);
        // edwards25519.FeAdd(&t0, &A.Y, &A.Z)
        CafeFieldElement t0 = repr.y.add(repr.z);
        // edwards25519.FeMul(&u, &u, &t0)
        u = u.mul(t0);

        // edwards25519.FeMul(&v, &t0, &inv1)
        CafeFieldElement v = t0.mul(inv1);
        // edwards25519.FeMul(&v, &v, &A.Z)
        v = v.mul(repr.z);
        // edwards25519.FeMul(&v, &v, &sqrtMinusAPlus2)
        v = v.mul(CafeFieldElement.SQRT_MINUS_A_PLUS_2);

        // edwards25519.FeAdd(&b, &u, &edwards25519.A)
        CafeFieldElement b = u.add(CafeFieldElement.A);
        // edwards25519.FeSquare(&b3, &b) // 2
        CafeFieldElement b3 = b.sqr();
        // edwards25519.FeMul(&b3, &b3, &b) // 3
        b3 = b3.mul(b);
        // edwards25519.FeSquare(&c, &b3) // 6
        CafeFieldElement c = b3.sqr();
        // edwards25519.FeMul(&b7, &c, &b)  // 7
        CafeFieldElement b7 = c.mul(b);
        // edwards25519.FeMul(&b8, &b7, &b) // 8
        CafeFieldElement b8 = b7.mul(b);
        // edwards25519.FeMul(&c, &b7, &u)
        c = b7.mul(u);
        // q58(&c, &c)
        c = c.powPm5d8();

        // edwards25519.FeSquare(&chi, &c)
        CafeFieldElement chi = c.sqr();
        // edwards25519.FeSquare(&chi, &chi)
        chi = chi.sqr();

        // edwards25519.FeSquare(&t0, &u)
        t0 = u.sqr();
        // edwards25519.FeMul(&chi, &chi, &t0)
        chi = chi.mul(t0);
        // edwards25519.FeSquare(&t0, &b7) // 14
        t0 = b7.sqr();
        // edwards25519.FeMul(&chi, &chi, &t0)
        chi = chi.mul(t0);
        // edwards25519.FeNeg(&chi, &chi)
        chi = chi.neg();

        // edwards25519.FeToBytes(&chiBytes, &chi)
        byte[] chiBytes = chi.encode();
        // chi[1] is either 0 or 0xff
        if (chiBytes[1] == (byte)0xFF) {
            return false;
        }

        // Calculate r1 = sqrt(-u/(2*(u+A)))
        // edwards25519.FeMul(&r1, &c, &u)
        CafeFieldElement r1 = c.mul(u);
        // edwards25519.FeMul(&r1, &r1, &b3)
        r1 = r1.mul(b3);
        // edwards25519.FeMul(&r1, &r1, &sqrtMinusHalf)
        r1 = r1.mul(CafeFieldElement.SQRT_MINUS_HALF);

        // edwards25519.FeSquare(&t0, &r1)
        t0 = r1.sqr();
        // edwards25519.FeMul(&t0, &t0, &b)
        t0 = t0.mul(b);
        // edwards25519.FeAdd(&t0, &t0, &t0)
        t0 = t0.add(t0);
        // edwards25519.FeAdd(&t0, &t0, &u)
        t0 = t0.add(u);

        // edwards25519.FeOne(&maybeSqrtM1)
        CafeFieldElement maybeSqrtM1 = CafeFieldElement.ONE;
        // edwards25519.FeCMove(&maybeSqrtM1, &edwards25519.SqrtM1, edwards25519.FeIsNonZero(&t0))
        maybeSqrtM1 = maybeSqrtM1.cmov(CafeConstants.SQRT_M1, t0.cequals(CafeFieldElement.ZERO) ^ 1);
        // edwards25519.FeMul(&r1, &r1, &maybeSqrtM1)
        r1 = r1.mul(maybeSqrtM1);

        // Calculate r = sqrt(-(u+A)/(2u))
        // edwards25519.FeSquare(&t0, &c) // 2
        t0 = c.sqr();
        // edwards25519.FeMul(&t0, &t0, &c) // 3
        t0 = t0.mul(c);
        // edwards25519.FeSquare(&t0, &t0) // 6
        t0 = t0.sqr();
        // edwards25519.FeMul(&r, &t0, &c) // 7
        CafeFieldElement r = t0.mul(c);

        // edwards25519.FeSquare(&t0, &u) // 2
        t0 = u.sqr();
        // edwards25519.FeMul(&t0, &t0, &u) // 3
        t0 = t0.mul(u);
        // edwards25519.FeMul(&r, &r, &t0)
        r = r.mul(t0);

        // edwards25519.FeSquare(&t0, &b8) // 16
        t0 = b8.sqr();
        // edwards25519.FeMul(&t0, &t0, &b8) // 24
        t0 = t0.mul(b8);
        // edwards25519.FeMul(&t0, &t0, &b) // 25
        t0 = t0.mul(b);
        // edwards25519.FeMul(&r, &r, &t0)
        r = r.mul(t0);
        // edwards25519.FeMul(&r, &r, &sqrtMinusHalf)
        r = r.mul(CafeFieldElement.SQRT_MINUS_HALF);

        // edwards25519.FeSquare(&t0, &r)
        t0 = r.sqr();
        // edwards25519.FeMul(&t0, &t0, &u)
        t0 = t0.mul(u);
        // edwards25519.FeAdd(&t0, &t0, &t0)
        t0 = t0.add(t0);
        // edwards25519.FeAdd(&t0, &t0, &b)
        t0 = t0.add(b);
        // edwards25519.FeOne(&maybeSqrtM1)
        maybeSqrtM1 = CafeFieldElement.ONE;
        // edwards25519.FeCMove(&maybeSqrtM1, &edwards25519.SqrtM1, edwards25519.FeIsNonZero(&t0))
        maybeSqrtM1 = maybeSqrtM1.cmov(CafeConstants.SQRT_M1, t0.cequals(CafeFieldElement.ZERO) ^ 1);
        // edwards25519.FeToBytes(&vBytes, &v)
        r = r.mul(maybeSqrtM1);

        // vInSquareRootImage := feBytesLE(&vBytes, &halfQMinus1Bytes)
        int vInSquareRootImage = v.isNegative() ^ 1;
        // edwards25519.FeCMove(&r, &r1, vInSquareRootImage)
        r = r.cmov(r1, vInSquareRootImage);

        // edwards25519.FeToBytes(publicKey, &u)
        byte[] uBytes = u.encode();
        System.arraycopy(uBytes, 0, result, 0, X25519ByteEccUtils.POINT_BYTES);

        // edwards25519.FeToBytes(representative, &r)
        byte[] rBytes = r.encode();
        System.arraycopy(rBytes, 0, uniformResult, 0, X25519ByteEccUtils.POINT_BYTES);

        return true;
    }

    @Override
    public byte[] uniformMul(final byte[] uniformPoint, final byte[] k) {
        assert X25519ByteEccUtils.checkClampScalar(k);
        assert X25519ByteEccUtils.checkPoint(uniformPoint);
        // edwards25519.FeFromBytes(&rr2, representative)
        CafeFieldElement rr2 = CafeFieldElement.decode(uniformPoint);
        // compute d = -A / (1 + 2r^2)
        // edwards25519.FeSquare2(&rr2, &rr2)
        rr2 = rr2.squareAndDouble();
        // rr2[0]++
        rr2.t[0]++;
        // edwards25519.FeInvert(&rr2, &rr2)
        rr2 = rr2.inv();
        // edwards25519.FeMul(&v, &edwards25519.A, &rr2)
        CafeFieldElement v = rr2.mul(CafeFieldElement.A);
        // edwards25519.FeNeg(&v, &v)
        v = v.neg();

        // compute e = (d^3 + Ad^2 + d)^{(q - 1) / 2}
        // edwards25519.FeSquare(&v2, &v)
        CafeFieldElement v2 = v.sqr();
        // edwards25519.FeMul(&v3, &v, &v2)
        CafeFieldElement v3 = v2.mul(v);
        // edwards25519.FeAdd(&e, &v3, &v)
        CafeFieldElement e = v3.add(v);
        // edwards25519.FeMul(&v2, &v2, &edwards25519.A)
        v2 = v2.mul(CafeFieldElement.A);
        // edwards25519.FeAdd(&e, &v2, &e)
        e = e.add(v2);
        // chi(&e, &e)
        e = e.chi();
        // edwards25519.FeToBytes(&eBytes, &e)
        byte[] eBytes = e.encode();
        // eBytes[1] is either 0 (for e = 1) or 0xff (for e = -1)
        // eIsMinus1 := int32(eBytes[1]) & 1
        int eIsMinus1 = ((int)eBytes[1]) & 1;
        // edwards25519.FeNeg(&negV, &v)
        CafeFieldElement negV = v.neg();
        // edwards25519.FeCMove(&v, &negV, eIsMinus1)
        v = v.cmov(negV, eIsMinus1);
        // edwards25519.FeZero(&v2)
        v2 = CafeFieldElement.ZERO;
        // edwards25519.FeCMove(&v2, &edwards25519.A, eIsMinus1)
        v2 = v2.cmov(CafeFieldElement.A, eIsMinus1);
        // edwards25519.FeSub(&v, &v, &v2)
        v = v.sub(v2);
        byte[] point = v.encode();

        // edwards25519.FeToBytes(publicKey, &v)
        byte[] result = new byte[X25519ByteEccUtils.POINT_BYTES];
        X25519ByteEccUtils.clampScalarMult(k, point, result);
        return result;
    }

    @Override
    public byte[] mul(byte[] point, byte[] k) {
        assert X25519ByteEccUtils.checkClampScalar(k);
        assert X25519ByteEccUtils.checkPoint(point);
        byte[] result = new byte[X25519ByteEccUtils.POINT_BYTES];
        X25519ByteEccUtils.clampScalarMult(k, point, result);
        return result;
    }

    @Override
    public int pointByteLength() {
        return X25519ByteEccUtils.POINT_BYTES;
    }

    @Override
    public int scalarByteLength() {
        return X25519ByteEccUtils.SCALAR_BYTES;
    }
}
