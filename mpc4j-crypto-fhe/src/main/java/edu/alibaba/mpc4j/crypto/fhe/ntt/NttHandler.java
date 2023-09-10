package edu.alibaba.mpc4j.crypto.fhe.ntt;

import edu.alibaba.mpc4j.crypto.fhe.zq.MultiplyUintModOperand;

/**
 * @author Qixian Zhou
 * @date 2023/8/27
 */
public class NttHandler {

    private ModArithLazy arithmetic;

    public NttHandler() {
    }

    public NttHandler(ModArithLazy arithmetic) {
        this.arithmetic = arithmetic;
    }


    public void transformToRev(long[] values, int logN, MultiplyUintModOperand[] roots, MultiplyUintModOperand scalar) {

        int n = 1 << logN;
        // registers to hold temporary values
        MultiplyUintModOperand r;
        long u;
        long v;
        // pointers for faster indexing
//        long x;
//        long y;
        // variables for indexing
        int gap = n >>> 1;
        int m = 1;
        int rootsIndex = 0;

        for (; m < (n >>> 1); m <<= 1) {
            int offSet = 0;
            if (gap < 4) {
                for (int i = 0; i < m; i++) {
                    r = roots[++rootsIndex];
//                    x = values[offSet];
//                    y = values[offSet + gap];
                    for (int j = 0; j < gap; j++) {
                        u = arithmetic.guard(values[offSet + j]);
                        v = arithmetic.mulRoot(values[offSet + gap + j], r);
                        // compute and update
                        values[offSet + j] = arithmetic.add(u, v);
                        values[offSet + gap + j] = arithmetic.sub(u, v);
                    }
                    offSet += (gap << 1);
                }
            } else {
                for (int i = 0; i < m; i++) {
                    r = roots[++rootsIndex];
//                    x = values[offSet];
//                    y = values[offSet + gap];
                    for (int j = 0; j < gap; j += 4) {
                        u = arithmetic.guard(values[offSet + j]);
                        v = arithmetic.mulRoot(values[offSet + gap + j], r);
                        values[offSet + j] = arithmetic.add(u, v);
                        values[offSet + gap + j] = arithmetic.sub(u, v);

                        u = arithmetic.guard(values[offSet + j + 1]);
                        v = arithmetic.mulRoot(values[offSet + gap + j + 1], r);
                        values[offSet + j + 1] = arithmetic.add(u, v);
                        values[offSet + gap + j + 1] = arithmetic.sub(u, v);

                        u = arithmetic.guard(values[offSet + j + 2]);
                        v = arithmetic.mulRoot(values[offSet + gap + j + 2], r);
                        values[offSet + j + 2] = arithmetic.add(u, v);
                        values[offSet + gap + j + 2] = arithmetic.sub(u, v);

                        u = arithmetic.guard(values[offSet + j + 3]);
                        v = arithmetic.mulRoot(values[offSet + gap + j + 3], r);
                        values[offSet + j + 3] = arithmetic.add(u, v);
                        values[offSet + gap + j + 3] = arithmetic.sub(u, v);
                    }
                    offSet += (gap << 1);
                }
            }
            gap >>>= 1;
        }

        if (scalar != null) {
            int valuesIndex = 0;
            MultiplyUintModOperand scaledR;

            for (int i = 0; i < m; i++) {
                r = roots[++rootsIndex];
                scaledR = arithmetic.mulRootScalar(r, scalar);

                u = arithmetic.mulScalar(arithmetic.guard(values[0 + valuesIndex]), scalar);
                v = arithmetic.mulRoot(values[1 + valuesIndex], scaledR);
                values[0 + valuesIndex] = arithmetic.add(u, v);
                values[1 + valuesIndex] = arithmetic.sub(u, v);
                valuesIndex += 2;
            }
        } else {

            int valuesIndex = 0;
            for (int i = 0; i < m; i++) {
                r = roots[++rootsIndex];
                u = arithmetic.guard(values[0 + valuesIndex]);
                v = arithmetic.mulRoot(values[1 + valuesIndex], r);

                values[0 + valuesIndex] = arithmetic.add(u, v);
                values[1 + valuesIndex] = arithmetic.sub(u, v);

                valuesIndex += 2;
            }
        }


    }



    public void transformFromRev(long[] values, int logN, MultiplyUintModOperand[] roots, MultiplyUintModOperand scalar) {

        // constant transform size
        int n = 1 << logN;
        // registers to hold temporary values
        MultiplyUintModOperand r;
        long u;
        long v;
        // pointers for faster indexing
//        long x;
//        long y;
        // variables for indexing
        int gap = 1;
        int m = n >>> 1;
        int rootsIndex = 0;

        for (; m > 1; m >>= 1) {
            int offset = 0;
            if (gap < 4) {
                for (int i = 0; i < m; i++) {
                    r = roots[++rootsIndex];
                    for (int j = 0; j < gap; j++) {
                        u = values[offset + j];
                        v = values[offset + gap + j];

                        values[offset + j] = arithmetic.guard(arithmetic.add(u, v));
                        values[offset + gap + j] = arithmetic.mulRoot(arithmetic.sub(u, v), r);
                    }
                    offset += (gap << 1);
                }
            } else {
                for (int i = 0; i < m; i++) {
                    r = roots[++rootsIndex];
                    for (int j = 0; j < gap; j += 4) {
                        u = values[offset + j];
                        v = values[offset + gap + j];
                        values[offset + j] = arithmetic.guard(arithmetic.add(u, v));
                        values[offset + gap + j] = arithmetic.mulRoot(arithmetic.sub(u, v), r);

                        u = values[offset + j + 1];
                        v = values[offset + gap + j + 1];
                        values[offset + j + 1] = arithmetic.guard(arithmetic.add(u, v));
                        values[offset + gap + j + 1] = arithmetic.mulRoot(arithmetic.sub(u, v), r);

                        u = values[offset + j + 2];
                        v = values[offset + gap + j + 2];
                        values[offset + j + 2] = arithmetic.guard(arithmetic.add(u, v));
                        values[offset + gap + j + 2] = arithmetic.mulRoot(arithmetic.sub(u, v), r);

                        u = values[offset + j + 3];
                        v = values[offset + gap + j + 3];
                        values[offset + j + 3] = arithmetic.guard(arithmetic.add(u, v));
                        values[offset + gap + j + 3] = arithmetic.mulRoot(arithmetic.sub(u, v), r);
                    }
                    offset += (gap << 1);
                }
            }
            gap <<= 1;
        }

        // handle scalar
        if (scalar != null) {
            r = roots[++rootsIndex];
            MultiplyUintModOperand scaledR = arithmetic.mulRootScalar(r, scalar);

            if (gap < 4) {
                for (int j = 0; j < gap; j++) {
                    u = arithmetic.guard(values[j]);
                    v = values[gap + j];
                    values[j] = arithmetic.mulScalar(arithmetic.guard(arithmetic.add(u, v)), scalar);
                    values[gap + j] = arithmetic.mulRoot(arithmetic.sub(u, v), scaledR);
                }
            } else {
                for (int j = 0; j < gap; j += 4) {

                    u = arithmetic.guard(values[j]);
                    v = values[gap + j];
                    values[j] = arithmetic.mulScalar(arithmetic.guard(arithmetic.add(u, v)), scalar);
                    values[gap + j] = arithmetic.mulRoot(arithmetic.sub(u, v), scaledR);

                    u = arithmetic.guard(values[j + 1]);
                    v = values[gap + j + 1];
                    values[j + 1] = arithmetic.mulScalar(arithmetic.guard(arithmetic.add(u, v)), scalar);
                    values[gap + j + 1] = arithmetic.mulRoot(arithmetic.sub(u, v), scaledR);

                    u = arithmetic.guard(values[j + 2]);
                    v = values[gap + j + 2];
                    values[j + 2] = arithmetic.mulScalar(arithmetic.guard(arithmetic.add(u, v)), scalar);
                    values[gap + j + 2] = arithmetic.mulRoot(arithmetic.sub(u, v), scaledR);

                    u = arithmetic.guard(values[j + 3]);
                    v = values[gap + j + 3];
                    values[j + 3] = arithmetic.mulScalar(arithmetic.guard(arithmetic.add(u, v)), scalar);
                    values[gap + j + 3] = arithmetic.mulRoot(arithmetic.sub(u, v), scaledR);
                }
            }
        } else {

            r = roots[++rootsIndex];
            if (gap < 4) {
                for (int j = 0; j < gap; j++) {
                    u = values[j];
                    v = values[gap + j];
                    values[j] = arithmetic.guard(arithmetic.add(u, v));
                    values[gap + j] = arithmetic.mulRoot(arithmetic.sub(u, v), r);
                }
            } else {
                for (int j = 0; j < gap; j += 4) {

                    u = values[j];
                    v = values[gap + j];
                    values[j] = arithmetic.guard(arithmetic.add(u, v));
                    values[gap + j] = arithmetic.mulRoot(arithmetic.sub(u, v), r);

                    u = values[j + 1];
                    v = values[gap + j + 1];
                    values[j + 1] = arithmetic.guard(arithmetic.add(u, v));
                    values[gap + j + 1] = arithmetic.mulRoot(arithmetic.sub(u, v), r);

                    u = values[j + 2];
                    v = values[gap + j + 2];
                    values[j + 2] = arithmetic.guard(arithmetic.add(u, v));
                    values[gap + j + 2] = arithmetic.mulRoot(arithmetic.sub(u, v), r);

                    u = values[j + 3];
                    v = values[gap + j + 3];
                    values[j + 3] = arithmetic.guard(arithmetic.add(u, v));
                    values[gap + j + 3] = arithmetic.mulRoot(arithmetic.sub(u, v), r);
                }
            }
        }
    }

}
