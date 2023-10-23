package edu.alibaba.mpc4j.s2pc.sbitmap.utils;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.PlainBitmap;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.RoaringPlainBitmap;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.measure.NominalScale;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.data.vector.ByteVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Sbitmap Utilities.
 *
 * @author Li Peng
 * @date 2023/8/3
 */
public class SbitmapUtils {
    /**
     * name of key attribute
     */
    private static String KEY = "key";
    /**
     * name of value attribute
     */
    private static String VALUE = "value";
    /**
     * name of bitmap attribute
     */
    private static String BITMAP = "bitmap";

    /**
     * private constructor
     */
    private SbitmapUtils() {
        // empty
    }

    public static ByteVector createNominalVector(StructField structField, int[] intArray) {
        assert (structField.measure instanceof NominalScale)
            : "StructField for column " + structField.name + " must be Nominal: " + structField.measure;
        assert structField.type.isByte()
            : "StructField type for column " + structField.name + " must be byte: " + structField.type;
        // 转换并合并ByteVector数据
        byte[] byteArray = new byte[intArray.length];
        IntStream.range(0, byteArray.length).forEach(index -> byteArray[index] = (byte) intArray[index]);
        return ByteVector.of(structField, byteArray);
    }

    /**
     * Create bitmap for nominal attributes.
     *
     * @param dataFrame dataframe.
     * @return bitmap.
     */
    public static DataFrame createBitmapForNominals(DataFrame dataFrame) {
        int nRows = dataFrame.nrows();
        StructType schema = dataFrame.schema();
        StructType bitmapSchema = new StructType(
            new StructField(KEY, DataTypes.StringType),
            new StructField(VALUE, DataTypes.StringType),
            new StructField(BITMAP, DataTypes.StringType));
        List<Tuple> bitmapTuples = new ArrayList<>();
        // iterate attributes
        for (int columnIndex = 0; columnIndex < schema.length(); columnIndex++) {
            StructField structField = schema.field(columnIndex);
            // build bitmap when attribute is nominal scale.
            if (structField.measure instanceof NominalScale) {
                String[] distinctValues = ((NominalScale) structField.measure).levels();
                int[] distinctIntValues = Arrays.stream(distinctValues)
                    .mapToInt(s -> ((NominalScale) structField.measure).valueOf(s).intValue()).toArray();
                // init bitmaps
                BitVector[] bitmapBitVectors = IntStream.range(0, distinctIntValues.length)
                    .mapToObj(i -> BitVectorFactory.createZeros(nRows)).toArray(BitVector[]::new);
                for (int rowIndex = 0; rowIndex < nRows; rowIndex++) {
                    int value = dataFrame.getInt(rowIndex, columnIndex);
                    for (int valueIndex = 0; valueIndex < distinctValues.length; valueIndex++) {
                        if (value == distinctIntValues[valueIndex]) {
                            // set current location of bitmap to ture.
                            bitmapBitVectors[valueIndex].set(rowIndex, true);
                            break;
                        }
                    }
                }
                // wrap to PlainBitmap
                PlainBitmap[] bitmaps = Arrays.stream(bitmapBitVectors)
                    .map(RoaringBitmapUtils::toRoaringBitmap)
                    .map(b -> RoaringPlainBitmap.fromBitmap(nRows, b)).toArray(PlainBitmap[]::new);
                // assemble the bitmaps to dataframe table
                for (int valueIndex = 0; valueIndex < distinctValues.length; valueIndex++) {
                    Tuple bitmapTuple = Tuple.of(
                        new Object[]{structField.name,
                            distinctValues[valueIndex],
                            // encode
                            Converter.serializeToBase64(bitmaps[valueIndex])
                        },
                        bitmapSchema);
                    bitmapTuples.add(bitmapTuple);
                }
            }
        }
        assert bitmapTuples.size() != 0 : "Num of bitmaps should not be 0.";
        return DataFrame.of(bitmapTuples);
    }

}

