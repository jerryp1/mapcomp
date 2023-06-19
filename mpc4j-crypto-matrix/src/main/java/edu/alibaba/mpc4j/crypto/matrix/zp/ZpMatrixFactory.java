package edu.alibaba.mpc4j.crypto.matrix.zp;

/**
 * Zp matrix factory.
 *
 * @author Weiran Liu
 * @date 2023/6/19
 */
public class ZpMatrixFactory {
    /**
     * private constructor.
     */
    private ZpMatrixFactory() {
        // empty
    }

    /**
     * Zp matrix type
     */
    public enum ZpMatrixType {
        /**
         * dense
         */
        DENSE,
        /**
         * sparse
         */
        SPARSE,
        /**
         * band
         */
        BAND,
    }

    /**
     * Returns if the given matrix is an identity matrix.
     *
     * @param matrix matrix.
     * @return true if it is an identity matrix.
     */
    public static boolean isIdentity(ZpMatrix matrix) {
        ZpMatrixType type = matrix.getType();
        switch (type) {
            case DENSE:
                return DenseZpMatrix.isIdentity((DenseZpMatrix) matrix);
            case SPARSE:
            case BAND:
            default:
                throw new IllegalArgumentException("Invalid " + ZpMatrixType.class.getSimpleName() + ": " + type);
        }
    }
}
