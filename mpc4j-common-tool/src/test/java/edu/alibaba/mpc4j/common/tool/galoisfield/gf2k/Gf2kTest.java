package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * GF(2^128)功能测试。
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
@RunWith(Parameterized.class)
public class Gf2kTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // SSE
        configurationParams.add(new Object[]{Gf2kType.SSE.name(), Gf2kType.SSE,});
        // NTL
        configurationParams.add(new Object[]{Gf2kType.NTL.name(), Gf2kType.NTL,});
        // BC
        configurationParams.add(new Object[]{Gf2kType.BC.name(), Gf2kType.BC,});
        // RINGS
        configurationParams.add(new Object[]{Gf2kType.RINGS.name(), Gf2kType.RINGS,});

        return configurationParams;
    }

    /**
     * the GF(2^λ) type.
     */
    private final Gf2kType type;
    /**
     * the GF(2^λ).
     */
    private final Gf2k gf2k;

    public Gf2kTest(String name, Gf2kType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        gf2k = Gf2kFactory.createInstance(type, EnvType.STANDARD);
    }

    @Test
    public void testType() {
        Assert.assertEquals(type, gf2k.getGf2kType());
    }

    @Test
    public void testConstantMultiply() {
        byte[] p;
        byte[] copyP;
        byte[] q;
        byte[] t;
        byte[] truth;
        // x * x = x^2
        p = new byte[]{
            (byte) 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02
        };
        q = new byte[]{
            (byte) 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02
        };
        truth = new byte[]{
            (byte) 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04
        };
        t = gf2k.mul(p, q);
        Assert.assertArrayEquals(truth, t);
        copyP = BytesUtils.clone(p);
        gf2k.muli(copyP, q);
        Assert.assertArrayEquals(truth, copyP);
        copyP = BytesUtils.clone(p);
        gf2k.muli(copyP, copyP);
        Assert.assertArrayEquals(truth, copyP);
        // x^2 * x^2 = x^4
        p = new byte[]{
            (byte) 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04
        };
        q = new byte[]{
            (byte) 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04
        };
        truth = new byte[]{
            (byte) 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10
        };
        t = gf2k.mul(p, q);
        Assert.assertArrayEquals(truth, t);
        copyP = BytesUtils.clone(p);
        gf2k.muli(copyP, q);
        Assert.assertArrayEquals(truth, copyP);
        copyP = BytesUtils.clone(p);
        gf2k.muli(copyP, copyP);
        Assert.assertArrayEquals(truth, copyP);
    }
}
