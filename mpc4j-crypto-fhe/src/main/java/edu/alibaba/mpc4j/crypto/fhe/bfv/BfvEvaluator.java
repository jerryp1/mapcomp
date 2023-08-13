package edu.alibaba.mpc4j.crypto.fhe.bfv;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.fhe.utils.NttContext;
import edu.alibaba.mpc4j.crypto.fhe.utils.Polynomial;
import org.checkerframework.checker.units.qual.C;

import java.math.BigInteger;

/**
 * @author Qixian Zhou
 * @date 2023/7/17
 */
public class BfvEvaluator {



    public BfvParameters param;

    public BfvEvaluator(BfvParameters param) {
        this.param = param;
    }

    public Ciphertext add(Ciphertext cipher1, Ciphertext cipher2) {
        assert cipher1.cipherModulus == cipher2.cipherModulus;
        Polynomial c0 = cipher1.c0.add(cipher2.c0, param.cipherModulus);
        Polynomial c1 = cipher1.c1.add(cipher2.c1, param.cipherModulus);
        return new Ciphertext(c0, c1);
    }


    public Ciphertext multiply(Ciphertext cipher1, Ciphertext cipher2, RelinKey relinKey) {

        // 判断密文模是否满足相关性质，以使用NTT 来加速乘法
//        assert BigInteger.valueOf(param.cipherModulus).isProbablePrime(CommonConstants.STATS_BIT_LENGTH)
//                && param.cipherModulus % (2 * param.polyModulusDegree) == 1;
//        NttContext nttContext = new NttContext(param.polyModulusDegree, param.cipherModulus);
        // 1. c0 * c0
//        Polynomial c0 = cipher1.c0.mul(cipher2.c0, nttContext);
        Polynomial c0 = cipher1.c0.mulNaiveNoMod(cipher2.c0);

//        System.out.println("c0 ntt no mod: " + c0);

        c0 = c0.mulScalarRound((double) 1/param.scalingFactor);
        c0.modInplace(param.cipherModulus);
        // c0 * c1 + c1 * c0
        Polynomial c1 = cipher1.c0.mulNaiveNoMod(cipher2.c1).add(cipher1.c1.mulNaiveNoMod(cipher2.c0));
//        Polynomial c1 = cipher1.c0.mulNttNoMod(cipher2.c1, nttContext).add(cipher1.c1.mulNttNoMod(cipher2.c0, nttContext));
//        System.out.println("c1 ntt no mod: " + c1);

        c1 = c1.mulScalarRound((double) 1/param.scalingFactor);
        c1.modInplace(param.cipherModulus);

        //2. c1 * c1
//        Polynomial c2 = cipher1.c1.mul(cipher2.c1, nttContext);
        Polynomial c2 = cipher1.c1.mulNaiveNoMod(cipher2.c1);
//        Polynomial c2 = cipher1.c1.mulNttNoMod(cipher2.c1, nttContext);
//        System.out.println("c2 ntt no mod: " + c2);

        c2 = c2.mulScalarRound((double) 1/param.scalingFactor);
        c2.modInplace(param.cipherModulus);

        return relinearize(relinKey, c0, c1, c2);
    }

    private Ciphertext relinearize(RelinKey relinKey, Polynomial c0, Polynomial c1, Polynomial c2) {

        PublicKey[] keys = relinKey.keys;
        long base = relinKey.base;
        int numLevels = keys.length;
        Polynomial[] c2Decomposed = c2.baseDecompose(base, numLevels);

        Polynomial newC0 = c0;
        Polynomial newC1 = c1;

        for (int i = 0; i < numLevels; i++) {
            newC0 = newC0.add(keys[i].p0.mul(c2Decomposed[i], param.cipherModulus), param.cipherModulus);
            newC1 = newC1.add(keys[i].p1.mul(c2Decomposed[i], param.cipherModulus), param.cipherModulus);
        }
        return new Ciphertext(newC0, newC1);
    }


    // todo
//    public Ciphertext multiply(Ciphertext cipher1, Ciphertext cipher2) {
//
//    }



}
