/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

/**
 * A point $((X:Z), (Y:T))$ on the $\mathbb P^1 \times \mathbb P^1$ model of the
 * curve.
 */
class CompletedPoint {
    final CafeFieldElement X;
    final CafeFieldElement Y;
    final CafeFieldElement Z;
    final CafeFieldElement T;

    CompletedPoint(CafeFieldElement X, CafeFieldElement Y, CafeFieldElement Z, CafeFieldElement T) {
        this.X = X;
        this.Y = Y;
        this.Z = Z;
        this.T = T;
    }

    /**
     * Convert this point from the $\mathbb P^1 \times \mathbb P^1$ model to the
     * $\mathbb P^2$ model.
     * <p>
     * This costs $3 \mathrm M$.
     */
    ProjectivePoint toProjective() {
        return new ProjectivePoint(this.X.mul(this.T), Y.mul(this.Z), this.Z.mul(this.T));
    }

    /**
     * Convert this point from the $\mathbb P^1 \times \mathbb P^1$ model to the
     * $\mathbb P^3$ model.
     * <p>
     * This costs $4 \mathrm M$.
     */
    CafeEdwardsPoint toExtended() {
        return new CafeEdwardsPoint(this.X.mul(this.T), Y.mul(this.Z), this.Z.mul(this.T),
                this.X.mul(this.Y));
    }
}
