package edu.alibaba.mpc4j.crypto.fhe.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Qixian Zhou
 * @date 2023/9/11
 */
public class GaloisToolTest {


    @Test
    public void create() {
        Assert.assertThrows(IllegalArgumentException.class, () -> new GaloisTool(0));
        Assert.assertThrows(IllegalArgumentException.class, () -> new GaloisTool(18));

        GaloisTool galoisTool = new GaloisTool(1);
        GaloisTool galoisTool1 = new GaloisTool(13);
    }


    @Test
    public void eltFromStep() {

        {
            // coeffCount = 8
            GaloisTool galoisTool = new GaloisTool(3);
            // galois elt 和 step 关系到底是什么？
            Assert.assertEquals(15, galoisTool.getEltFromStep(0));
            Assert.assertEquals(3, galoisTool.getEltFromStep(1));
            Assert.assertEquals(3, galoisTool.getEltFromStep(-3));
            Assert.assertEquals(9, galoisTool.getEltFromStep(2));
            Assert.assertEquals(9, galoisTool.getEltFromStep(-2));
            Assert.assertEquals(11, galoisTool.getEltFromStep(3));
            Assert.assertEquals(11, galoisTool.getEltFromStep(-1));
        }
    }

    @Test
    public void eltFromSteps() {

        {
            // coeffCount = 8
            GaloisTool galoisTool = new GaloisTool(3);
            int[] elts = galoisTool.getEltsFromSteps(new int[] {0, 1, -3, 2, -2, 3, -1});
            int[] eltsTrue = new int[] {15, 3, 3, 9, 9, 11, 11};

            for (int i = 0; i < elts.length; i++) {
                Assert.assertEquals(eltsTrue[i], elts[i]);
            }

        }
    }


    @Test
    public void eltsAll() {

        {
            // coeffCount = 8
            GaloisTool galoisTool = new GaloisTool(3);
            int[] elts = galoisTool.getEltsAll();
            int[] eltsTrue = new int[] {15, 3, 11, 9, 9};

            for (int i = 0; i < elts.length; i++) {
                Assert.assertEquals(eltsTrue[i], elts[i]);
            }

        }
    }

    @Test
    public void indexFromElt() {

        {
            Assert.assertEquals(7, GaloisTool.getIndexFromElt(15));
            Assert.assertEquals(1, GaloisTool.getIndexFromElt(3));
            Assert.assertEquals(4, GaloisTool.getIndexFromElt(9));
            Assert.assertEquals(5, GaloisTool.getIndexFromElt(11));
        }
    }




}
