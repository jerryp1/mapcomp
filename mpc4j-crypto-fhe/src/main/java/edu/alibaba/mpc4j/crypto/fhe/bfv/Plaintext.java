package edu.alibaba.mpc4j.crypto.fhe.bfv;

import edu.alibaba.mpc4j.crypto.fhe.utils.Polynomial;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.Objects;

/**
 * @author Qixian Zhou
 * @date 2023/7/13
 */
public class Plaintext {

    Polynomial poly;

    double scalingFactor;

    public Plaintext(Polynomial poly, double scalingFactor ) {
        this.poly = poly;
        this.scalingFactor = scalingFactor;
    }

    public Plaintext(Polynomial poly) {
        this.poly = poly;
        this.scalingFactor = 0.0;
    }

    public Plaintext(long polyModulusDegree, long[] coeffs) {
        this.poly = new Polynomial(polyModulusDegree, coeffs);
        this.scalingFactor = 0.0;
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Plaintext)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        Plaintext that = (Plaintext)obj;
        return new EqualsBuilder()
                .append(this.poly, that.poly)
                .isEquals();
    }



    @Override
    public int hashCode() {
        return Objects.hash(poly, scalingFactor);
    }

    @Override
    public String toString() {
        return poly.toString();
    }
}
