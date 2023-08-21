package edu.alibaba.mpc4j.s2pc.sbitmap;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.SecureBitmapFactory;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.SecureBitmapFactory.SecureBitmapType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.roaringbitmap.BitmapContainer;
import org.roaringbitmap.RoaringBitmap;

import java.util.ArrayList;
import java.util.Collection;

/**
 * tests for secure bitmap
 *
 * @author Weiran Liu
 * @date 2022/12/29
 */
@RunWith(Parameterized.class)
public class SecureBitmapTest {
    /**
     * the test type
     */
    private final SecureBitmapType type;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configuration = new ArrayList<>();
        // FULL
        configuration.add(new Object[]{SecureBitmapType.FULL.name(), SecureBitmapType.FULL,});
        // ROARING
        configuration.add(new Object[]{SecureBitmapType.ROARING.name(), SecureBitmapType.ROARING,});

        return configuration;
    }

    public SecureBitmapTest(String name, SecureBitmapType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalCreateFromBitmap() {
        RoaringBitmap validBitmap = RoaringBitmap.bitmapOf(0, 3, 5, 8);
        // create with totalBitNum = 0
        Assert.assertThrows(IllegalArgumentException.class, () ->
            SecureBitmapFactory.createFromBitMap(type, 0, validBitmap)
        );
        // create with totalBitNum = -1 * BitmapContainer.MAX_CAPACITY
        Assert.assertThrows(IllegalArgumentException.class, () ->
            SecureBitmapFactory.createFromBitMap(type, -1 * BitmapContainer.MAX_CAPACITY, validBitmap)
        );
        // create with invalid positive totalBitNum
        Assert.assertThrows(IllegalArgumentException.class, () ->
            SecureBitmapFactory.createFromBitMap(type, 7, validBitmap)
        );
        // create with bound positive totalBitNum
        Assert.assertThrows(IllegalArgumentException.class, () ->
            SecureBitmapFactory.createFromBitMap(type, 8, validBitmap)
        );
    }

    @Test
    public void testIllegalCreateOfRange() {
        // create with negative rangeStart
        Assert.assertThrows(IllegalArgumentException.class, () ->
            SecureBitmapFactory.createOfRange(type, 7, -1, 1)
        );
        // create with negative rangeEnd = rangeStart
        Assert.assertThrows(IllegalArgumentException.class, () ->
            SecureBitmapFactory.createOfRange(type, 7, 1, 1)
        );
        // create with large rangeEnd
        Assert.assertThrows(IllegalArgumentException.class, () ->
            SecureBitmapFactory.createOfRange(type, 7, 1, 8)
        );
        // create with bound rangeEnd
        Assert.assertThrows(IllegalArgumentException.class, () ->
            SecureBitmapFactory.createOfRange(type, 7, 1, 7)
        );
    }
}
