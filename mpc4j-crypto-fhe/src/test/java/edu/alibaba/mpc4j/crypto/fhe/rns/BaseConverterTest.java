package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIter;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/8/19
 */
public class BaseConverterTest {

    @Test
    public void initialize() {

        BaseConverter bct;
        // no throw
        bct= new BaseConverter(new RnsBase(new long[] {2}), new RnsBase(new long[] {2}));
        bct= new BaseConverter(new RnsBase(new long[] {2}), new RnsBase(new long[] {3}));
        bct= new BaseConverter(new RnsBase(new long[] {2, 3, 5}), new RnsBase(new long[] {2}));
        bct= new BaseConverter(new RnsBase(new long[] {2, 3, 5}), new RnsBase(new long[] {3, 5}));
        bct= new BaseConverter(new RnsBase(new long[] {2, 3, 5}), new RnsBase(new long[] {2, 3, 5, 7, 11}));
        bct= new BaseConverter(new RnsBase(new long[] {2, 3, 5}), new RnsBase(new long[] {7, 11}));
    }


    private void bctTest(BaseConverter bct, long[] in, long[] out) {
        long[] outArray = new long[out.length];
        bct.fastConvert(in, outArray);
        Assert.assertArrayEquals(outArray, out);
    }

    private void bctExactTest(BaseConverter bct, long[] in, long out) {
        long curOut =  bct.exactConvert(in);
        Assert.assertEquals(curOut, out);
    }


    @Test
    public void exactConvert() {

        BaseConverter bct;

        bct= new BaseConverter(new RnsBase(new long[] {2}), new RnsBase(new long[] {2}));
        bctExactTest(bct, new long[] {0}, 0);
        bctExactTest(bct, new long[] {1}, 1);

        bct= new BaseConverter(new RnsBase(new long[] {2}), new RnsBase(new long[] {3}));
        bctExactTest(bct, new long[] {0}, 0);
        bctExactTest(bct, new long[] {1}, 1);

        bct= new BaseConverter(new RnsBase(new long[] {3}), new RnsBase(new long[] {2}));
        bctExactTest(bct, new long[] {0}, 0);
        bctExactTest(bct, new long[] {1}, 1);

        bct= new BaseConverter(new RnsBase(new long[] {2, 3}), new RnsBase(new long[] {2}));
        bctExactTest(bct, new long[] {0, 0}, 0);
        bctExactTest(bct, new long[] {1, 1}, 1);
        bctExactTest(bct, new long[] {0, 2}, 0);
        bctExactTest(bct, new long[] {1, 0}, 1);
    }

    @Test
    public void convert() {

        BaseConverter bct;

        bct= new BaseConverter(new RnsBase(new long[] {2}), new RnsBase(new long[] {2}));
        bctTest(bct, new long[] {0}, new long[] {0});
        bctTest(bct, new long[] {1}, new long[] {1});

        bct= new BaseConverter(new RnsBase(new long[] {2}), new RnsBase(new long[] {3}));
        bctTest(bct, new long[] {0}, new long[] {0});
        bctTest(bct, new long[] {1}, new long[] {1});

        bct= new BaseConverter(new RnsBase(new long[] {3}), new RnsBase(new long[] {2}));
        bctTest(bct, new long[] {0}, new long[] {0});
        bctTest(bct, new long[] {1}, new long[] {1});


        bct= new BaseConverter(new RnsBase(new long[] {2, 3}), new RnsBase(new long[] {2}));
        bctTest(bct, new long[] {0, 0}, new long[] {0});
        bctTest(bct, new long[] {1, 1}, new long[] {1});
        bctTest(bct, new long[] {0, 2}, new long[] {0});
        bctTest(bct, new long[] {1, 0}, new long[] {1});

        bct= new BaseConverter(new RnsBase(new long[] {2, 3}), new RnsBase(new long[] {2, 3}));
        bctTest(bct, new long[] {0, 0}, new long[] {0, 0});
        bctTest(bct, new long[] {1, 1}, new long[] {1, 1});
        bctTest(bct, new long[] {1, 2}, new long[] {1, 2});
        bctTest(bct, new long[] {0, 2}, new long[] {0, 2});


        bct= new BaseConverter(new RnsBase(new long[] {2, 3}), new RnsBase(new long[] {3, 4, 5}));
        bctTest(bct, new long[] {0, 0}, new long[] {0, 0, 0});
        bctTest(bct, new long[] {1, 1}, new long[] {1, 3, 2});
        bctTest(bct, new long[] {1, 2}, new long[] {2, 1, 0});

        bct= new BaseConverter(new RnsBase(new long[] {3, 4, 5}), new RnsBase(new long[] {2, 3}));
        bctTest(bct, new long[] {0, 0, 0}, new long[] {0, 0});
        bctTest(bct, new long[] {1, 1, 1}, new long[] {1, 1});
    }



    private void bctExactArrayTest(BaseConverter bct, long[] in, long[] out) {
        long[] outArray = Arrays.copyOf(out, out.length);

        RnsIter inIter = new RnsIter(in, 3);

        bct.exactConvertArray(inIter, outArray);

        Assert.assertArrayEquals(out, outArray);
    }


    @Test
    public void exactConvertArray() {

        // 2 mod 3 ---> 2 mod 2 = 0, exact convert will failure on this
        BaseConverter bct;
        bct = new BaseConverter(new RnsBase(new long[]{3}), new RnsBase(new long[]{2}));
        bctExactArrayTest(bct,
                new long[]{
                        0, 1, 0 // 2
                },
                new long[]{
                        0, 1, 0
                }
        );

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2}));
        bctExactArrayTest(bct,
                new long[]{
                        0, 1, 0,
                        0, 1, 2
                },
                new long[]
                        {0, 1, 0}
                );

    }


    private void bctArrayTest(BaseConverter bct, long[] in, long[] out) {
        long[] outArray = Arrays.copyOf(out, out.length);

        RnsIter outIter = new RnsIter(outArray, 3);

        RnsIter inIter = new RnsIter(in, 3);
        // in Iter 是在 inBase 下的数据，outIter 是在 outBase 下的数据
        bct.fastConvertArray(inIter, outIter);
        Assert.assertArrayEquals(out, outIter.coeffIter);
    }

    @Test
    public void convertArray() {

        BaseConverter bct;
        bct = new BaseConverter(new RnsBase(new long[]{3}), new RnsBase(new long[]{2}));
        bctArrayTest(bct,
                new long[]{
                        0, 1, 2
                },
                new long[]{
                        0, 1, 0
                }
        );

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2}));
        bctArrayTest(bct,
                new long[]{
                     0, 1, 0, 0, 1, 2
                },
                new long[]{
                        0, 1, 0
                });

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2, 3}));
        bctArrayTest(bct,
                new long[]{
                        1, 1, 0, 1, 2, 2
                },
                new long[]{
                        1, 1, 0, 1, 2, 2
                });

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{3, 4, 5}));
        bctArrayTest(bct,
                new long[]{
                    0, 1, 1, 0, 1, 2
                },
                new long[]{
                    0, 1, 2, 0, 3, 1, 0, 2, 0
                });
    }

}
