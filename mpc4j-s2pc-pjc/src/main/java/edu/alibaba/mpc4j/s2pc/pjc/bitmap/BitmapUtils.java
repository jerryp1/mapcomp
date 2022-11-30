package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.roaringbitmap.BitmapContainer;
import org.roaringbitmap.ContainerPointer;
import org.roaringbitmap.RoaringBitmap;

import static edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapContainer.*;

/**
 * Bitmap工具类
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2022/11/28
 */
public class BitmapUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitmapUtils.class);

    public static byte[] roaringBitmapToBytes(RoaringBitmap roaringBitmap) {
        // TODO 这里是否要检测，元素是否超过2^24?
        BitmapContainer[] cs = expandContainers(roaringBitmap);
        byte[] bytes = new byte[BYTE_LENGTH];
        for (int i = 0; i < cs.length; i++) {
            // TODO 这里对转换后的长度没有做检测
            byte[] bitmap = LongUtils.longArrayToByteArrayLE(cs[i].toLongBuffer().array());
            assert bitmap.length == BitmapContainer.MAX_CAPACITY / Byte.SIZE;
            System.arraycopy(bitmap, 0, bytes, i * CONTAINER_BYTE_SIZE, CONTAINER_BYTE_SIZE);
        }
        return bytes;
    }


    /**
     * 将roaringBitmap拓展为全量bitmap容器
     *
     * @param roaringBitmap roaringBitmap
     * @return 全量bitmap容器
     */
    public static BitmapContainer[] expandContainers(RoaringBitmap roaringBitmap) {
        ContainerPointer cp = roaringBitmap.getContainerPointer();
        if (cp.getCardinality() == 0) {
            LOGGER.error("error cardinality {}", cp.getCardinality());
        }
        BitmapContainer[] cs = new BitmapContainer[CONTAINERS_NUM];
        int lastKey = -1;
        while (cp.getContainer() != null) {
            int currentKey = cp.key();
            cs[currentKey] = cp.getContainer().toBitmapContainer();
            expandContainers(cs, lastKey, currentKey);
            lastKey = currentKey;
            cp.advance();
        }
        expandContainers(cs, lastKey, CONTAINERS_NUM);
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
}
