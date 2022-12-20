package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.roaringbitmap.BitmapContainer;
import org.roaringbitmap.ContainerPointer;
import org.roaringbitmap.RoaringBitmap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Bitmap工具类
 *
 * @author Li Peng  
 * @date 2022/11/28
 */
public class BitmapUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitmapUtils.class);

    /**
     * 将roaringBitmap转换为byte[]
     *
     * @param roaringBitmap 将roaringBitmap转换为byte
     * @param maxNum        最大元素数量
     * @return byte[]
     */
    public static byte[] roaringBitmapToBytes(RoaringBitmap roaringBitmap, int maxNum) {
        int byteNum = CommonUtils.getByteLength(maxNum);
        int containerNum = getContainerNum(maxNum);
        BitmapContainer[] cs = expandContainers(roaringBitmap, containerNum);
        ByteBuffer buffer = ByteBuffer.allocate(CommonUtils.getByteLength(getRoundContainerNumBitLength(maxNum)))
            .order(ByteOrder.LITTLE_ENDIAN);
        Arrays.stream(cs).forEach(c -> c.writeArray(buffer));
        byte[] roundContainerBytes = buffer.array();
        return Arrays.copyOf(roundContainerBytes, byteNum);
    }

    /**
     * 将roaringBitmap拓展为全量bitmap容器
     *
     * @param roaringBitmap roaringBitmap
     * @return 全量bitmap容器
     */
    public static BitmapContainer[] expandContainers(RoaringBitmap roaringBitmap, int containerNum) {
        ContainerPointer cp = roaringBitmap.getContainerPointer();
        if (cp.getCardinality() == 0) {
            LOGGER.error("error cardinality {}", cp.getCardinality());
        }
        BitmapContainer[] cs = new BitmapContainer[containerNum];
        int lastKey = -1;
        while (cp.getContainer() != null) {
            int currentKey = cp.key();
            cs[currentKey] = cp.getContainer().toBitmapContainer();
            expandContainers(cs, lastKey, currentKey);
            lastKey = currentKey;
            cp.advance();
        }
        expandContainers(cs, lastKey, containerNum);
        return cs;
    }

    /**
     * 将中间跳过的container设置为空的BitmapContainer
     *
     * @param cs         容器数组
     * @param lastKey    上一个key
     * @param currentKey 当前key
     */
    public static void expandContainers(BitmapContainer[] cs, int lastKey, int currentKey) {
        if (lastKey >= currentKey) {
            LOGGER.error("error key");
        }
        if (lastKey + 1 == currentKey) {
            return;
        }
        for (int i = lastKey + 1; i < currentKey; i++) {
            cs[i] = new BitmapContainer();
        }
    }

    /**
     * 获得maxNum数量个元素至少需要多少个Container存储
     *
     * @param maxNum 最大数量
     * @return 需要的Container数量
     */
    public static int getContainerNum(int maxNum) {
        return CommonUtils.getUnitNum(maxNum, BitmapContainer.MAX_CAPACITY);
    }

    /**
     * 获得maxNum数量个元素在Container中至少需要多少bit
     *
     * @param maxNum 最大数量
     * @return 需要的bit数量
     */
    public static int getRoundContainerNumBitLength(int maxNum) {
        return getContainerNum(maxNum) * BitmapContainer.MAX_CAPACITY;
    }
}
