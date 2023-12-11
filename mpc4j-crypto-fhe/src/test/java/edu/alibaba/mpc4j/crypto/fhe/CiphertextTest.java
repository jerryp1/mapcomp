package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Ciphertext Test.
 *
 * @author Qixian Zhou
 * @date 2023/9/13
 */
public class CiphertextTest {

    @Test
    public void bfvCiphertextBasics() {

        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);

        parms.setPolyModulusDegree(2);
        parms.setCoeffModulus(CoeffModulus.create(2, new int[] {30}));
        parms.setPlainModulus(2);

        Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);

        Ciphertext ctxt = new Ciphertext(context);
        ctxt.reserve(10);
        Assert.assertEquals(0, ctxt.getSize());
        Assert.assertEquals(0, ctxt.getDynArray().size());
        // capacity = 10, N = 2, coeffModulusSize = 1
        Assert.assertEquals(10L * 2, ctxt.getDynArray().capacity());
        Assert.assertEquals(2, ctxt.getPolyModulusDegree());
        Assert.assertEquals(ctxt.getParmsId(), context.getFirstParmsId());
        Assert.assertFalse(ctxt.isNttForm());

        long[] ptr = ctxt.getData();
        ctxt.reserve(5);
        Assert.assertEquals(0, ctxt.getSize());
        Assert.assertEquals(0, ctxt.getDynArray().size());
        // capacity = 10, N = 2, coeffModulusSize = 1
        Assert.assertEquals(5L * 2, ctxt.getDynArray().capacity());
        Assert.assertEquals(2, ctxt.getPolyModulusDegree());
        Assert.assertEquals(ctxt.getParmsId(), context.getFirstParmsId());
        Assert.assertNotSame(ptr, ctxt.getData()); // reverse 后，底层数组发生变化

        ptr = ctxt.getData();
        ctxt.reserve(10);
        Assert.assertEquals(0, ctxt.getSize());
        Assert.assertEquals(0, ctxt.getDynArray().size());
        // capacity = 10, N = 2, coeffModulusSize = 1
        Assert.assertEquals(10L * 2, ctxt.getDynArray().capacity());
        Assert.assertEquals(2, ctxt.getPolyModulusDegree());
        Assert.assertEquals(ctxt.getParmsId(), context.getFirstParmsId());
        Assert.assertNotSame(ptr, ctxt.getData()); // reverse 后，底层数组发生变化

        ptr = ctxt.getData();
        ctxt.reserve(2);
        Assert.assertEquals(0, ctxt.getSize());
        Assert.assertEquals(0, ctxt.getDynArray().size());
        // capacity = 10, N = 2, coeffModulusSize = 1
        Assert.assertEquals(2L * 2, ctxt.getDynArray().capacity());
        Assert.assertEquals(2, ctxt.getPolyModulusDegree());
        Assert.assertEquals(ctxt.getParmsId(), context.getFirstParmsId());
        Assert.assertNotSame(ptr, ctxt.getData()); // reverse 后，底层数组发生变化

        ptr = ctxt.getData();
        ctxt.reserve(5);
        Assert.assertEquals(0, ctxt.getSize());
        Assert.assertEquals(0, ctxt.getDynArray().size());
        // capacity = 10, N = 2, coeffModulusSize = 1
        Assert.assertEquals(5L * 2, ctxt.getDynArray().capacity());
        Assert.assertEquals(2, ctxt.getPolyModulusDegree());
        Assert.assertEquals(ctxt.getParmsId(), context.getFirstParmsId());
        Assert.assertNotSame(ptr, ctxt.getData()); // reverse 后，底层数组发生变化

        Ciphertext ctxt2 = ctxt.clone();
        Assert.assertEquals(ctxt.getCoeffModulusSize(), ctxt2.getCoeffModulusSize());
        Assert.assertEquals(ctxt.isNttForm(), ctxt2.isNttForm());
        Assert.assertEquals(ctxt.getPolyModulusDegree(), ctxt2.getPolyModulusDegree());
        Assert.assertEquals(ctxt.getParmsId(), ctxt2.getParmsId());
        Assert.assertEquals(ctxt.getSize(), ctxt2.getSize());

        Ciphertext ctxt3;
        ctxt3 = ctxt.clone();
        Assert.assertEquals(ctxt.getCoeffModulusSize(), ctxt3.getCoeffModulusSize());
        Assert.assertEquals(ctxt.isNttForm(), ctxt3.isNttForm());
        Assert.assertEquals(ctxt.getPolyModulusDegree(), ctxt3.getPolyModulusDegree());
        Assert.assertEquals(ctxt.getParmsId(), ctxt3.getParmsId());
        Assert.assertEquals(ctxt.getSize(), ctxt3.getSize());
    }
}
