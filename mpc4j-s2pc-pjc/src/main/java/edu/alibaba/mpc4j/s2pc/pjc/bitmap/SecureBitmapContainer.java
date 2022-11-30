package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcSquareVector;
import org.roaringbitmap.BitmapContainer;

/**
 * 安全Bitmap容器
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2022/11/24
 */
public class SecureBitmapContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecureBitmapContainer.class);
    /**
     * 所有容器的bit总长度，2^24 TODO 要用builder设置成一个default值， 考虑传入一个config
     */
    public static final int BIT_LENGTH = 1 << 20;
    /**
     * 所有容器的byte总长度，即2^32bits转换为bytes的长度
     */
    public static final int BYTE_LENGTH = BIT_LENGTH / Byte.SIZE;
    /**
     * 容器总数量
     */
    public static final int CONTAINERS_NUM = BIT_LENGTH / BitmapContainer.MAX_CAPACITY;
    /**
     * 单个容器的byte总长度，即2^16bits转换为bytes的长度
     */
    public static final int CONTAINER_BYTE_SIZE = BitmapContainer.MAX_CAPACITY / Byte.SIZE;
    /**
     * 容器数量，暂时为所有容器总和
     */
    private final int containerNum;

    final BcSquareVector vector;

    public SecureBitmapContainer(BcSquareVector vector) {
        assert vector.byteLength() == BYTE_LENGTH;
        containerNum = CONTAINERS_NUM;
        this.vector = vector;
    }

    public BcSquareVector getVectors() {
        return vector;
    }

    public int getContainerNum() {
        return containerNum;
    }

    public boolean isPublic() {
        return vector.isPublic();
    }
}
