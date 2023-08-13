package edu.alibaba.mpc4j.crypto.fhe.bfv;

import edu.alibaba.mpc4j.crypto.fhe.utils.Polynomial;
import edu.alibaba.mpc4j.crypto.fhe.utils.RandomSample;
import org.omg.PortableServer.POA;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/7/17
 */
public class BfvEncryptor {



    public long polyModulusDegree;
    // Q
    public long coeffModulus;

    public PublicKey publicKey;
    // Delta
    public long scalingFactor;

    public BfvEncryptor(BfvParameters param, PublicKey publicKey) {
        this.polyModulusDegree = param.polyModulusDegree;
        this.coeffModulus = param.cipherModulus;
        this.publicKey = publicKey;
        this.scalingFactor = param.scalingFactor;
    }

    public Ciphertext encrypt(Plaintext message) {

        assert message.poly.polyModulusDegree == polyModulusDegree;

        Polynomial p0 = publicKey.p0;
        Polynomial p1 = publicKey.p1;

        Polynomial scaledMessage = message.poly.mulScalar(scalingFactor, coeffModulus);

        Polynomial u = new Polynomial(polyModulusDegree, RandomSample.sampleTriangle((int) polyModulusDegree));

//        long[] vec = new long[(int) polyModulusDegree];
//        Arrays.fill(vec, 1);
//        Polynomial u = new Polynomial(polyModulusDegree, vec);

        Polynomial e0 =  new Polynomial(polyModulusDegree, RandomSample.sampleTriangle((int) polyModulusDegree));
        Polynomial e1 =  new Polynomial(polyModulusDegree, RandomSample.sampleTriangle((int) polyModulusDegree));
//        Polynomial e0 = new Polynomial(polyModulusDegree, new long[(int) polyModulusDegree]);
//        Polynomial e1 = new Polynomial(polyModulusDegree, new long[(int) polyModulusDegree]);

        Polynomial c0 = e0.add( p0.mul(u, coeffModulus), coeffModulus).add(scaledMessage, coeffModulus);
        Polynomial c1 = e1.add( p1.mul(u, coeffModulus), coeffModulus);

        return new Ciphertext(c0, c1);
    }



}
