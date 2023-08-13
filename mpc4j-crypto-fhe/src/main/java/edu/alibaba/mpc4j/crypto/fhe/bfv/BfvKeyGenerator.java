package edu.alibaba.mpc4j.crypto.fhe.bfv;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.fhe.utils.Polynomial;
import edu.alibaba.mpc4j.crypto.fhe.utils.RandomSample;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/7/17
 */
public class BfvKeyGenerator {


    public BfvParameters param;

    public SecretKey secretKey;

    public PublicKey publicKey;

    public RelinKey relinKey;


    public BfvKeyGenerator(BfvParameters param) {
        this.param = param;
        this.generateSecretKey();
        this.generatePublicKey();
        this.generateRelinKey();
    }

    private void generateSecretKey() {

        this.secretKey = new SecretKey(new Polynomial(param.polyModulusDegree,
                RandomSample.sampleTriangle((int) param.polyModulusDegree)));


//        long[] vec = new long[(int) param.polyModulusDegree];
//        Arrays.fill(vec, 1);
//
//        this.secretKey = new SecretKey(new Polynomial(param.polyModulusDegree, vec));



    }

    private void generatePublicKey() {

        Polynomial p1 = new Polynomial(param.polyModulusDegree,
                RandomSample.sampleUniform(0, param.cipherModulus, (int) param.polyModulusDegree) );
        // error 的分布和 密钥分布 在实现的时候相同？
        Polynomial pkError = new Polynomial(param.polyModulusDegree,
                RandomSample.sampleTriangle((int) param.polyModulusDegree));

//        long[] vec = new long[(int) param.polyModulusDegree];
//        Arrays.fill(vec, 1);
//        Polynomial p1 = new Polynomial(param.polyModulusDegree, vec);
//        Polynomial pkError = new Polynomial(param.polyModulusDegree, vec);

        // 这里多项式乘法不加速吗？
        Polynomial p0 = pkError.add(p1.mul(this.secretKey.s, param.cipherModulus), param.cipherModulus).mulScalar(-1, param.cipherModulus);

        this.publicKey = new PublicKey(p0, p1);
    }

    private void generateRelinKey() {
        // T = ceil(sqrt(q))
        long base = (long) Math.ceil(Math.sqrt((double) param.cipherModulus));
        // l = ceil(log_T(q))
        // log_a(b) = log_c(b) / log_c(a)
        int numLevels =  (int) Math.floor(Math.log(param.cipherModulus) / Math.log(base)) + 1;

        PublicKey[] keys = new PublicKey[numLevels];
        long power = 1;
        // 不加速吗？
        Polynomial skSquare = this.secretKey.s.mul(this.secretKey.s, param.cipherModulus);
//        long[] vec = new long[(int) param.polyModulusDegree];
//        Arrays.fill(vec, 1);
        for (int i = 0; i < numLevels; i++) {

//            Polynomial k1 = new Polynomial(param.polyModulusDegree, vec);
//            Polynomial error = new Polynomial(param.polyModulusDegree, vec);


            // a_i \leftarrow R_q
            Polynomial k1 = new Polynomial(param.polyModulusDegree, RandomSample.sampleUniform(0, param.cipherModulus,
                                            (int) param.polyModulusDegree));
            Polynomial error = new Polynomial(param.polyModulusDegree, RandomSample.sampleTriangle((int) param.polyModulusDegree));
            // k0 = -(a_i \cdot s + e_i) + T^i \cdot s^2
            Polynomial k0 = this.secretKey.s.mul(k1, param.cipherModulus)
                    .add(error, param.cipherModulus)
                    .mulScalar(-1)
                    .add(skSquare.mulScalar(power), param.cipherModulus); ;
            k0.modInplace(param.cipherModulus);
            //
            keys[i] = new PublicKey(k0, k1);
            //
            power *= base;
            power %= param.cipherModulus;
        }

        this.relinKey = new RelinKey(base, keys);

    }


}
