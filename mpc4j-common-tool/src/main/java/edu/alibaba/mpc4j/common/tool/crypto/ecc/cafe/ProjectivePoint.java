/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

/**
 * A point $(X:Y:Z)$ on the $\mathbb P^2$ model of the curve.
 */
class ProjectivePoint {
    final CafeFieldElement X;
    final CafeFieldElement Y;
    final CafeFieldElement Z;

    ProjectivePoint(CafeFieldElement X, CafeFieldElement Y, CafeFieldElement Z) {
        this.X = X;
        this.Y = Y;
        this.Z = Z;
    }

    /**
     * Convert this point from the $\mathbb P^2$ model to the $\mathbb P^3$ model.
     * <p>
     * This costs $3 \mathrm M + 1 \mathrm S$.
     */
    EdwardsPoint toExtended() {
        return new EdwardsPoint(this.X.mul(this.Z), Y.mul(this.Z), this.Z.sqr(), this.X.mul(this.Y));
    }

    /**
     * Point doubling: add this point to itself.
     *
     * @return $[2]P$ as a CompletedPoint.
     */
    CompletedPoint dbl() {
        CafeFieldElement XX = this.X.sqr();
        CafeFieldElement YY = this.Y.sqr();
        CafeFieldElement ZZ2 = this.Z.squareAndDouble();
        CafeFieldElement XPlusY = this.X.add(this.Y);
        CafeFieldElement XPlusYSq = XPlusY.sqr();
        CafeFieldElement YYPlusXX = YY.add(XX);
        CafeFieldElement YYMinusXX = YY.sub(XX);
        return new CompletedPoint(XPlusYSq.sub(YYPlusXX), YYPlusXX, YYMinusXX, ZZ2.sub(YYMinusXX));
    }
}
