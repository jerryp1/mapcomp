package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

/**
 * single-query OPRF key.
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public interface SqOprfKey {
    /**
     * Gets the prf output.
     *
     * @param input the input.
     * @return the prf output.
     */
    byte[] getPrf(byte[] input);
}
