package edu.alibaba.mpc4j.crypto.fhe.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Qixian Zhou
 * @date 2023/9/4
 */
public class DynArrayTest {



    @Test
    public void dynArrayBasic() {
        {
            DynArray arr = new DynArray();
            Assert.assertEquals(0, arr.size());
            Assert.assertEquals(0, arr.capacity());
            Assert.assertTrue(arr.empty());

            arr.resize(1);
            Assert.assertEquals(1, arr.size());
            Assert.assertEquals(1, arr.capacity());
            Assert.assertFalse(arr.empty());
            Assert.assertEquals(0, arr.at(0));
            arr.set(0, 1);
            Assert.assertEquals(1, arr.at(0));

            arr.reserve(6);
            Assert.assertEquals(1, arr.size());
            Assert.assertEquals(6, arr.capacity());
            Assert.assertFalse(arr.empty());
            Assert.assertEquals(1, arr.at(0));

            arr.resize(4);
            Assert.assertEquals(4, arr.size());
            Assert.assertEquals(6, arr.capacity());
            Assert.assertFalse(arr.empty());
            arr.set(0, 0);
            arr.set(1, 1);
            arr.set(2, 2);
            arr.set(3, 3);
            Assert.assertEquals(0, arr.at(0));
            Assert.assertEquals(1, arr.at(1));
            Assert.assertEquals(2, arr.at(2));
            Assert.assertEquals(3, arr.at(3));

            arr.shrinkToFit();
            Assert.assertEquals(4, arr.size());
            Assert.assertEquals(4, arr.capacity());
            Assert.assertFalse(arr.empty());
            Assert.assertEquals(0, arr.at(0));
            Assert.assertEquals(1, arr.at(1));
            Assert.assertEquals(2, arr.at(2));
            Assert.assertEquals(3, arr.at(3));
        }
    }


}
