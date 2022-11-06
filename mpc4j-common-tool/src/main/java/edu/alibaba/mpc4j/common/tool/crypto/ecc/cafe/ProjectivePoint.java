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
    final FieldElement X;
    final FieldElement Y;
    final FieldElement Z;

    ProjectivePoint(FieldElement X, FieldElement Y, FieldElement Z) {
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
        FieldElement XX = this.X.sqr();
        FieldElement YY = this.Y.sqr();
        FieldElement ZZ2 = this.Z.squareAndDouble();
        FieldElement XPlusY = this.X.add(this.Y);
        FieldElement XPlusYSq = XPlusY.sqr();
        FieldElement YYPlusXX = YY.add(XX);
        FieldElement YYMinusXX = YY.sub(XX);
        return new CompletedPoint(XPlusYSq.sub(YYPlusXX), YYPlusXX, YYMinusXX, ZZ2.sub(YYMinusXX));
    }
}
