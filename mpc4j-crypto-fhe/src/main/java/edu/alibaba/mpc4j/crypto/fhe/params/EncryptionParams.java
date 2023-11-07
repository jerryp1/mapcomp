package edu.alibaba.mpc4j.crypto.fhe.params;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGenerator;
import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGeneratorFactory;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.utils.HashFunction;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


import java.util.Arrays;
import java.util.Objects;

/**
 * Represents user-customizable encryption scheme settings. The parameters (most
 * importantly poly_modulus, coeff_modulus, plain_modulus) significantly affect
 * the performance, capabilities, and security of the encryption scheme. Once
 * an instance of EncryptionParameters is populated with appropriate parameters,
 * it can be used to create an instance of the SEALContext class, which verifies
 * the validity of the parameters, and performs necessary pre-computations.
 *
 * Picking appropriate encryption parameters is essential to enable a particular
 * application while balancing performance and security. Some encryption settings
 * will not allow some inputs (e.g. attempting to encrypt a polynomial with more
 * coefficients than poly_modulus or larger coefficients than plain_modulus) or,
 * support the desired computations (with noise growing too fast due to too large
 * plain_modulus and too small coeff_modulus).
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/encryptionparams.h#L86
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/30
 */
public class EncryptionParams implements Cloneable {

    /**
     * scheme type
     */
    private final SchemeType scheme;
    /**
     * poly modulus degree
     */
    private int polyModulusDegree;
    /**
     * coeff modulus
     */
    private Modulus[] coeffModulus;
    /**
     * uniform random generator factory
     */
    private UniformRandomGeneratorFactory randomGeneratorFactory;
    /**
     * plain modulus
     */
    private Modulus plainModulus;
    /**
     * parms id
     */
    private ParmsIdType parmsId;

    /**
     * Creates an empty set of encryption parameters, SchemType is NONE.
     */
    public EncryptionParams() {
        scheme = SchemeType.NONE;
        polyModulusDegree = 0;
        coeffModulus = new Modulus[0];
        randomGeneratorFactory = null;
        plainModulus = null;
        parmsId = ParmsIdType.parmsIdZero();
        computeParmsId();
    }

    /**
     * create encryption parameters with given scheme type.
     *
     * @param scheme scheme type.
     */
    public EncryptionParams(SchemeType scheme) {
        if (!isValidScheme(scheme)) {
            throw new IllegalArgumentException("unsupported scheme");
        }
        this.scheme = scheme;
        polyModulusDegree = 0;
        coeffModulus = new Modulus[0];
        randomGeneratorFactory = null;
        plainModulus = null;
        parmsId = ParmsIdType.parmsIdZero();
        computeParmsId();
    }

    /**
     * create encryption parameters with given scheme type.
     *
     * @param scheme scheme type.
     */
    public EncryptionParams(int scheme) {
        this(SchemeType.getByValue(scheme));
    }

    /**
     * deep copy an EncryptionParams object.
     *
     * @param other another EncryptionParams object.
     */
    public EncryptionParams(EncryptionParams other) {
        this.scheme = other.scheme;
        this.polyModulusDegree = other.polyModulusDegree;
        this.coeffModulus = Arrays.stream(other.coeffModulus).map(Modulus::new).toArray(Modulus[]::new);
        this.plainModulus = new Modulus(other.plainModulus);
        this.randomGeneratorFactory = other.randomGeneratorFactory;
        this.parmsId = new ParmsIdType(other.parmsId);
    }

    /**
     * Sets the degree of the polynomial modulus parameter to the specified value.
     * The polynomial modulus directly affects the number of coefficients in
     * plaintext polynomials, the size of ciphertext elements, the computational
     * performance of the scheme (bigger is worse), and the security level (bigger
     * is better). In Microsoft SEAL the degree of the polynomial modulus must be
     * a power of 2 (e.g.  1024, 2048, 4096, 8192, 16384, or 32768).
     *
     * @param polyModulusDegree the new polynomial modulus degree.
     */
    public void setPolyModulusDegree(int polyModulusDegree) {
        if (scheme == SchemeType.NONE && polyModulusDegree != 0) {
            throw new IllegalArgumentException("polyModulusDegree is not supported for this scheme");
        }
        this.polyModulusDegree = polyModulusDegree;
        // re-compute parms id
        computeParmsId();
    }

    /**
     * Sets the coefficient modulus parameter. The coefficient modulus consists
     * of a list of distinct prime numbers, and is represented by a vector of
     * Modulus objects.
     * The coefficient modulus directly affects the size
     * of ciphertext elements, the amount of computation that the scheme can
     * perform (bigger is better), and the security level (bigger is worse). In
     * our implementation(ref SEAL-4.0) each of the prime numbers in the coefficient modulus must
     * be at most 60 bits, and must be congruent to 1 modulo 2*poly_modulus_degree.
     *
     * @param coeffModulus the new coefficient modulus.
     */
    public void setCoeffModulus(Modulus[] coeffModulus) {
        if (scheme == SchemeType.NONE) {
            if (coeffModulus.length != 0) {
                throw new IllegalArgumentException("coeffModulus is not supported for this scheme");
            }
        } else if (coeffModulus.length > Constants.COEFF_MOD_COUNT_MAX || coeffModulus.length < Constants.COEFF_MOD_COUNT_MIN) {
            throw new IllegalArgumentException("coeffModulus size is invalid");
        }
        this.coeffModulus = coeffModulus;
        // re-compute parms id
        computeParmsId();
    }

    /**
     * Sets the coefficient modulus parameter.
     *
     * @param coeffModulus the new coefficient modulus.
     */
    public void setCoeffModulus(long[] coeffModulus) {
        setCoeffModulus(Modulus.createModulus(coeffModulus));
    }

    /**
     * Sets the plaintext modulus parameter. The plaintext modulus is an integer
     * modulus represented by the Modulus class. The plaintext modulus
     * determines the largest coefficient that plaintext polynomials can represent.
     * It also affects the amount of computation that the scheme can perform
     * (bigger is worse). In our implementation(ref SEAL-4.0), the plaintext modulus can be at most
     * 60 bits long, but can otherwise be any integer. Note, however, that some
     * features (e.g. batching) require the plaintext modulus to be of a particular form.
     *
     * @param plainModulus the new plaintext modulus
     */
    public void setPlainModulus(Modulus plainModulus) {
        if (scheme != SchemeType.BFV && scheme != SchemeType.BGV && !plainModulus.isZero()) {
            throw new IllegalArgumentException("plainModulus is not supported for this scheme");
        }
        this.plainModulus = plainModulus;
        // Re-compute parms_id
        computeParmsId();
    }

    /**
     * Sets the plaintext modulus parameter.
     *
     * @param plainModulus the new plaintext modulus.
     */
    public void setPlainModulus(long plainModulus) {
        setPlainModulus(new Modulus(plainModulus));
    }

    /**
     * set random generator factory.
     * @param randomGeneratorFactory random generator factory.
     */
    public void setRandomGeneratorFactory(UniformRandomGeneratorFactory randomGeneratorFactory) {
        this.randomGeneratorFactory = randomGeneratorFactory;
    }

    /**
     * return poly modulus degree.
     *
     * @return poly modulus degree.
     */
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    /**
     * return plain modulus.
     *
     * @return plain modulus.
     */
    public Modulus getPlainModulus() {
        return plainModulus;
    }

    /**
     * return coeff modulus.
     *
     * @return coeff modulus.
     */
    public Modulus[] getCoeffModulus() {
        return coeffModulus;
    }

    /**
     * return scheme type.
     *
     * @return scheme type.
     */
    public SchemeType getScheme() {
        return scheme;
    }

    /**
     * return random generator factory.
     *
     * @return random generator factory.
     */
    public UniformRandomGeneratorFactory getRandomGeneratorFactory() {
        return randomGeneratorFactory;
    }

    /**
     * return parms id.
     *
     * @return parms id.
     */
    public ParmsIdType getParmsId() {
        return parmsId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EncryptionParams that = (EncryptionParams) o;
        return new EqualsBuilder()
            .append(parmsId, that.parmsId)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return parmsId.hashCode();
    }

    public void computeParmsId() {
        int coeffModulusSize = coeffModulus == null ? 0 : coeffModulus.length;
        int plainModulusUint64Count = plainModulus == null ? 0 : plainModulus.getUint64Count();
        int totalUint64Count = Common.addSafe(1, 1, true, coeffModulusSize, plainModulusUint64Count);
        long[] paramData = new long[totalUint64Count];
        // Write the scheme identifier
        paramData[0] = scheme.getValue();
        // Write the poly_modulus_degree. Note that it will always be positive
        paramData[1] = polyModulusDegree;
        int i = 2;
        if (coeffModulusSize > 0) {
            for (Modulus modulus : coeffModulus) {
                paramData[i++] = modulus.getValue();
            }
        }
        if (plainModulus != null && plainModulus.getUint64Count() > 0) {
            paramData[i] = plainModulus.getValue();
        }
        HashFunction.hash(paramData, totalUint64Count, parmsId.value);
        if (parmsId.isZero()) {
            throw new RuntimeException("parmsId cannot be zero");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EncryptionParams{" + "scheme=")
            .append(scheme.ordinal())
            .append(", polyModulusDegree=")
            .append(polyModulusDegree)
            .append(", coeffModulus: [\n");
        for (Modulus modulus : coeffModulus) {
            sb.append(modulus.toString()).append("\n");
        }
        sb.append(", plainModulus=").append(plainModulus).append(", parmsId=").append(parmsId);
        return sb.toString();
    }

    /**
     * check the validity of the scheme type.
     *
     * @param scheme scheme type.
     * @return whether the scheme type is valid.
     */
    private boolean isValidScheme(int scheme) {
        switch (SchemeType.getByValue(scheme)) {
            case NONE:
            case BFV:
                return true;
            case BGV:
            case CKKS:
                throw new IllegalArgumentException("currently unsupported scheme");
            default:
                return false;
        }
    }

    /**
     * check the validity of the scheme type.
     *
     * @param scheme scheme type.
     * @return whether the scheme type is valid.
     */
    private boolean isValidScheme(SchemeType scheme) {
        switch (scheme) {
            case NONE:
            case BFV:
                return true;
            case BGV:
            case CKKS:
                throw new IllegalArgumentException("currently unsupported scheme");
            default:
                return false;
        }
    }

    @Override
    public EncryptionParams clone() {
        try {
            EncryptionParams clone = (EncryptionParams) super.clone();
            // only clone this member
            clone.parmsId = this.parmsId.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
